package ifp.tfg_grua_app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import ifp.tfg_grua_app.databinding.ActivityMapGpsBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Calendar

class MapGPS : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap // Mapa
    private lateinit var binding: ActivityMapGpsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient // GPS
    private lateinit var destino: LatLng // Ubicacion Usuario
    private var posicionActual: LatLng? = null // Ubicacion destino
    private var marcadorUsuario: Marker? = null // Marcador azul
    //private var marcadorDestino: Marker? = null
    //private var rutaCalculada = false
    private var rutaPolyline: Polyline? = null // Linea de recorrido
    private var popupLlegadaMostrado = false   // Para que el popup de llegada salga solo una vez
    private var popupEntregaMostrado = false   // Para que el popup de entrega salga solo una vez

    companion object {
        const val API_KEY = "AIzaSyAxLBlpI6vtCy658BaQDMH8Mpmepl6CafM" // Credencial API Maps
        const val EXTRA_LAT = "destino_lat"
        const val EXTRA_LNG = "destino_lng"
        const val EXTRA_ID = "viaje_id"
        const val EXTRA_FASE = "fase"          // "RECOGIDA" (default) o "ENTREGA"
        const val FASE_RECOGIDA = "RECOGIDA"
        const val FASE_ENTREGA  = "ENTREGA"
    }

    private var fase: String = FASE_RECOGIDA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapGpsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Quita la barra de tareas superior
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Recoge datos del Intent
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
        val recogidaID = intent.getIntExtra(EXTRA_ID, -1)
        fase = intent.getStringExtra(EXTRA_FASE) ?: FASE_RECOGIDA
        destino = LatLng(lat, lng)
        android.util.Log.d("MAPGPS", "Extras recibidos -> id=$recogidaID, lat=$lat, lng=$lng, fase=$fase")

        // Mostrar datos del viaje en el panel
        val viaje = RecogidasRepo.Recogidas.find { it.id == recogidaID } // Lista global de todos los viajes
        viaje?.let {
            binding.tvCliente.text = "Cliente: ${it.cliente}"
            binding.tvMatricula.text = "Matrícula: ${it.matricula}"
            binding.tvMotivo.text = "Motivo: ${it.motivo}"
            binding.tvTelefono.text = "Tel: ${it.telefono}"
            // Rellena cada campo del panel inferior con los datos del viaje
        }

        // GPS
        // Busca el viaje por su id en la lista y rellena los TextView del panel inferior
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Mapa
        // Busca el fragmento del mapa en el XML y lo convierte al tipo correcto para poder llamar a getMapAsync
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this) // Carga el mapa

        binding.btnSalir.setOnClickListener {
            ChangeActivity(this, MainActivity::class.java)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap // Mapa

        // Ocultar botones y puntos de interés del mapa
        mMap.uiSettings.isMapToolbarEnabled   = false
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.isTrafficEnabled = false

        // Cambia la posicion con un click
        mMap.setOnMapClickListener { puntoClickado ->
            posicionActual = puntoClickado
            marcadorUsuario?.position = puntoClickado
            calcularRuta()
        }

        // Revisa si el usuario concedio permisos y arranca el GPS
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            iniciarGPS()
        } else { // Si no hay permisos mouestra popup de permisos
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    private fun iniciarGPS() {

        val request = LocationRequest.Builder( // Peticion al GPS
            Priority.PRIORITY_HIGH_ACCURACY /* Pide que use el más preciso*/, 1000L  // Intervalo de segundos en actualizarse 1000L (1 segundo)
        ).setMinUpdateDistanceMeters(2f).build()    // Distancia de actualizacion 2f (cada 2 metros)

        // Comprueba si hay permisos de ubicacion de nuevo
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        // Actualizaciones de ubicacion
        fusedLocationClient.requestLocationUpdates(request, object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                val loc = result.lastLocation ?: return // Guarda la localizacion mas reciente del user
                posicionActual = LatLng(loc.latitude, loc.longitude) // Convierte ubicacion el coordenadas

                if (marcadorUsuario == null) {

                    marcadorUsuario = mMap.addMarker( // Añade el marcador en la posicion
                        MarkerOptions()
                            .position(posicionActual!!)
                            .anchor(0.5f, 0.5f)
                            .flat(true)
                            .icon(crearIconoUsuario())
                    )
                } else {
                    marcadorUsuario!!.position = posicionActual!!  // Sobreescribe la posicion del marcador
                }
                // Mueve la camara o zoom de maps hacia la ubicacion
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(posicionActual!!, 15f)
                )
                // Recalcula la ruta (y la distancia) en cada actualizacion de GPS
                calcularRuta()
            }
        }, Looper.getMainLooper()) // Ejecuta en el hilo principal
    }


    private fun calcularRuta() { // Llama a la API, interpreta la respuesta (JSON) y actualiza la UI

        val origen = posicionActual ?: return // Posicion actual guardada
        binding.tvInstruccion.text = "Calculando ruta..."

        lifecycleScope.launch(Dispatchers.IO) { // Corrutina

            try {
                val url = "https://maps.googleapis.com/maps/api/directions/json" + // Llamada a la API
                        "?origin=${origen.latitude},${origen.longitude}" +
                        "&destination=${destino.latitude},${destino.longitude}" +
                        "&mode=driving&language=es&key=$API_KEY"

                val json = OkHttpClient() // Peticion HTTP a Google
                    .newCall(Request.Builder().url(url).build())
                    .execute().body?.string() ?: return@launch

                val obj = JSONObject(json) // Convierte JSON a objeto
                android.util.Log.d("MAPGPS", "Respuesta Google status=${obj.optString("status")} body=$json")

                if (obj.getString("status") != "OK") { // Respuesta Google
                    withContext(Dispatchers.Main) {
                        binding.tvInstruccion.text = "Error: ${obj.getString("status")}"
                    }
                    return@launch
                }

                // Parsear datos
                val ruta           = obj.getJSONArray("routes").getJSONObject(0)
                val tramo          = ruta.getJSONArray("legs").getJSONObject(0)
                val distancia      = tramo.getJSONObject("distance").getString("text")
                val distanciaMetros = tramo.getJSONObject("distance").getInt("value") // mismo dato en metros
                val tiempo         = tramo.getJSONObject("duration").getString("text")
                val segundos       = tramo.getJSONObject("duration").getInt("value")
                val steps      = tramo.getJSONArray("steps")
                val instruccion = parsearInstruccion(steps.getJSONObject(0))
                val siguiente  = if (steps.length() > 1)
                    "Después: ${parsearInstruccion(steps.getJSONObject(1))}"
                else "Después: llegarás al destino"
                val puntos     = decodificarPolyline(
                    ruta.getJSONObject("overview_polyline").getString("points")
                )

                withContext(Dispatchers.Main) { // Hilo principal
                    dibujarRuta(puntos) // Divuja la ruta en el mapa

                    // --- Detección de llegada ---
                    if (distanciaMetros <= 5) {
                        binding.tvInstruccion.text = "Ya has llegado"
                        // El popup sale una sola vez y depende de la fase
                        if (fase == FASE_RECOGIDA && !popupLlegadaMostrado) {
                            popupLlegadaMostrado = true
                            mostrarPopupLlegada()
                        } else if (fase == FASE_ENTREGA && !popupEntregaMostrado) {
                            popupEntregaMostrado = true
                            mostrarPopupEntrega()
                        }
                    } else {
                        binding.tvInstruccion.text = instruccion
                    }

                    binding.tvDistancia.text   = distancia
                    binding.tvTiempo.text      = tiempo

                    // Carga los 'steps' de la ruta
                    val siguienteStep = tramo.getJSONArray("steps")
                    // Muestra la siguiente instruccion
                    if (siguienteStep.length() > 1) {
                        binding.tvSiguiente.text = "Después: " +
                                parsearInstruccion(siguienteStep.getJSONObject(1))
                    } else {
                        binding.tvSiguiente.text = "Después: llegarás al destino"
                    }
                    //Calcula la hora de llegada
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.SECOND, segundos)

                    val hora = String.format("%02d:%02d",
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE))

                    binding.tvHoraLlegada.text = hora
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvInstruccion.text = "Error: ${e.message}"
                }
            }
        }
    }

    /**
     * Muestra el popup de llegada con el mismo estilo de tarjeta de MainActivity.
     * Al pulsar "Recogido" lanza una nueva navegación hacia la ubicación destino del servicio.
     */
    private fun mostrarPopupLlegada() {
        // Buscamos el viaje en el cache para poder mostrar los datos y leer el destino
        val recogidaID = intent.getIntExtra(EXTRA_ID, -1)
        val viaje = RecogidasRepo.Recogidas.find { it.id == recogidaID }

        // Inflamos el layout personalizado
        val vista = layoutInflater.inflate(R.layout.dialog_llegada, null)

        vista.findViewById<android.widget.TextView>(R.id.tvClienteLlegada).text =
            viaje?.cliente?.let { "Cliente: $it" } ?: ""
        vista.findViewById<android.widget.TextView>(R.id.tvMatriculaLlegada).text =
            viaje?.matricula?.let { "Matrícula: $it" } ?: ""
        vista.findViewById<android.widget.TextView>(R.id.tvMotivoLlegada).text =
            viaje?.motivo?.let { "Motivo: $it" } ?: ""

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(vista)
            .setCancelable(false)
            .create()

        // Quitamos el fondo blanco por defecto del dialog para que se vea la CardView limpia
        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        vista.findViewById<android.widget.Button>(R.id.btnRecogido).setOnClickListener {
            dialog.dismiss()

            // Coordenadas del destino final desde la tabla servicios
            val destinoLat = viaje?.destinoLat ?: 0.0
            val destinoLng = viaje?.destinoLng ?: 0.0

            if (destinoLat == 0.0 && destinoLng == 0.0) {
                android.widget.Toast.makeText(
                    this,
                    "El servicio no tiene destino definido",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Marcamos vehiculo_recogido=true en Supabase y relanzamos MapGPS en fase ENTREGA.
            // Así, si el usuario sale de la app, al volver a pulsar "Continuar navegación"
            // el MainActivity le llevará directamente al destino final en lugar de empezar de nuevo.
            lifecycleScope.launch {
                try {
                    if (recogidaID != -1) {
                        SupabaseClient.client.from("servicios").update(
                            { set("vehiculo_recogido", true) }
                        ) {
                            filter { eq("id", recogidaID) }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MAPGPS", "Error marcando vehiculo_recogido", e)
                } finally {
                    val i = Intent(this@MapGPS, MapGPS::class.java).apply {
                        putExtra(EXTRA_LAT, destinoLat)
                        putExtra(EXTRA_LNG, destinoLng)
                        putExtra(EXTRA_ID,  recogidaID)
                        putExtra(EXTRA_FASE, FASE_ENTREGA)
                    }
                    startActivity(i)
                    finish()
                }
            }
        }

        dialog.show()
    }

    /**
     * Popup que se muestra al llegar al destino final (fase ENTREGA).
     * Al pulsar "Entregado" actualiza el estado del servicio a "Terminado"
     * en Supabase y vuelve a MainActivity.
     */
    private fun mostrarPopupEntrega() {
        val recogidaID = intent.getIntExtra(EXTRA_ID, -1)
        val viaje = RecogidasRepo.Recogidas.find { it.id == recogidaID }

        val vista = layoutInflater.inflate(R.layout.dialog_entrega, null)

        vista.findViewById<android.widget.TextView>(R.id.tvClienteEntrega).text =
            viaje?.cliente?.let { "Cliente: $it" } ?: ""
        vista.findViewById<android.widget.TextView>(R.id.tvMatriculaEntrega).text =
            viaje?.matricula?.let { "Matrícula: $it" } ?: ""
        vista.findViewById<android.widget.TextView>(R.id.tvMotivoEntrega).text =
            viaje?.motivo?.let { "Motivo: $it" } ?: ""

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(vista)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        vista.findViewById<android.widget.Button>(R.id.btnEntregado).setOnClickListener {
            dialog.dismiss()

            // Actualizamos en Supabase el estado del servicio a "Terminado"
            lifecycleScope.launch {
                try {
                    if (recogidaID != -1) {
                        SupabaseClient.client.from("servicios").update(
                            { set("estado", "Terminado") }
                        ) {
                            filter { eq("id", recogidaID) }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MAPGPS", "Error actualizando estado a Terminado", e)
                    android.widget.Toast.makeText(
                        this@MapGPS,
                        "Error al cerrar el servicio: ${e.message ?: e.javaClass.simpleName}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } finally {
                    // Vuelve al MainActivity
                    startActivity(Intent(this@MapGPS, MainActivity::class.java))
                    finish()
                }
            }
        }

        dialog.show()
    }

    private fun parsearInstruccion(step: JSONObject): String {
        val maniobra  = if (step.has("maneuver")) step.getString("maneuver") else ""
        val html      = step.getString("html_instructions")
        val distancia = step.getJSONObject("distance").getString("text")

        // Limpiar el HTML completo para obtener el texto real
        val textoLimpio = html
            .replace(Regex("<div[^>]*>"), " ")   // <div ...> → espacio
            .replace("</div>", "")
            .replace(Regex("<b>|</b>"), "")      // quitar negritas
            .replace(Regex("<[^>]*>"), "")       // cualquier otra etiqueta
            .replace(Regex("\\s+"), " ")         // espacios dobles → uno
            .trim()

        // Extraer solo el nombre en negrita si existe (nombre de calle real)
        val calle = Regex("<b>(.*?)</b>").findAll(html).lastOrNull()?.groupValues?.get(1) ?: ""

        // Para rotondas extraer el número de salida
        val salida = if (maniobra.contains("roundabout"))
            Regex("(\\d+)").find(textoLimpio)?.value ?: "" else ""

        val texto = when {
            maniobra.contains("turn-right")  ->
                if (calle.isNotEmpty()) "Gira a la derecha en $calle"
                else textoLimpio

            maniobra.contains("turn-left")   ->
                if (calle.isNotEmpty()) "Gira a la izquierda en $calle"
                else textoLimpio

            maniobra.contains("roundabout")  ->
                if (salida.isNotEmpty() && calle.isNotEmpty())
                    "En la rotonda toma la ${salida}ª salida hacia $calle"
                else if (salida.isNotEmpty())
                    "En la rotonda toma la ${salida}ª salida"
                else textoLimpio

            maniobra.contains("keep-right")  ->
                if (calle.isNotEmpty()) "Manténgase a la derecha hacia $calle"
                else textoLimpio

            maniobra.contains("keep-left")   ->
                if (calle.isNotEmpty()) "Manténgase a la izquierda hacia $calle"
                else textoLimpio

            maniobra.contains("ramp") ||
                    maniobra.contains("fork")        ->
                if (calle.isNotEmpty()) "Coja la salida hacia $calle"
                else textoLimpio

            maniobra.contains("uturn")       -> "Dé la vuelta"
            maniobra.contains("arrive")      -> "Ha llegado al destino"

            else -> textoLimpio  // Usar el texto de Google directamente
        }

        return "$texto · $distancia"
    }

    private fun dibujarRuta(puntos: List<LatLng>) {
        rutaPolyline?.remove()
        rutaPolyline = mMap.addPolyline(
            PolylineOptions()
                .addAll(puntos)
                .width(12f)
                .color(Color.parseColor("#4285F4"))
                .geodesic(true)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )
    }

    private fun decodificarPolyline(encoded: String): List<LatLng> {
        val puntos = mutableListOf<LatLng>()
        var index = 0; var lat = 0; var lng = 0
        while (index < encoded.length) {
            var b: Int; var shift = 0; var result = 0
            do { b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift); shift += 5 } while (b >= 0x20)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1
            shift = 0; result = 0
            do { b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift); shift += 5 } while (b >= 0x20)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1
            puntos.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return puntos
    }

    private fun crearIconoUsuario(): BitmapDescriptor {
        val size   = 60
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Círculo exterior semitransparente
        Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.parseColor("#554285F4")  // Azul con 33% transparencia
            canvas.drawCircle(30f, 30f, 28f, it)
        }

        // Círculo interior azul sólido
        Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.parseColor("#4285F4")
            canvas.drawCircle(30f, 30f, 14f, it)
        }

        // Borde blanco
        Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color       = Color.WHITE
            it.style       = Paint.Style.STROKE
            it.strokeWidth = 3f
            canvas.drawCircle(30f, 30f, 14f, it)
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onRequestPermissionsResult( // Permisos
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            iniciarGPS()
        }
    }

    private fun <T> ChangeActivity(context: Context, cls: Class<T>){
        context.startActivity(Intent(context, cls))

        if(context is Activity){
            context.finish()
        }
    }
}