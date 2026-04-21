package ifp.tfg_grua_app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import ifp.tfg_grua_app.databinding.ActivityMapGpsBinding
import ifp.tfg_grua_app.databinding.DialogPopupBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

class MapGPS : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapGpsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var destino: LatLng
    private var posicionActual: LatLng? = null
    private var marcadorUsuario: Marker? = null
    private var rutaPolyline: Polyline? = null

    private var recogidaID: Int = -1
    private var fase: String = FASE_RECOGIDA
    private var popupMostrado = false   // Evita que el popup salga más de una vez

    // Voz
    private var tts: TextToSpeech? = null
    private var ttsListo = false
    private var ultimaInstruccionDicha: String? = null   // Evita repetir la misma indicación

    companion object {
        const val API_KEY = "AIzaSyAxLBlpI6vtCy658BaQDMH8Mpmepl6CafM"
        const val EXTRA_LAT  = "destino_lat"
        const val EXTRA_LNG  = "destino_lng"
        const val EXTRA_ID   = "viaje_id"
        const val EXTRA_FASE = "fase"
        const val FASE_RECOGIDA = "RECOGIDA"
        const val FASE_ENTREGA  = "ENTREGA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapGpsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Datos recibidos del Intent
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
        recogidaID = intent.getIntExtra(EXTRA_ID, -1)
        fase = intent.getStringExtra(EXTRA_FASE) ?: FASE_RECOGIDA
        destino = LatLng(lat, lng)

        // Panel inferior con los datos del viaje
        val viaje = RecogidasRepo.Recogidas.find { it.id == recogidaID }
        viaje?.let {
            binding.tvCliente.text   = "Cliente: ${it.cliente}"
            binding.tvMatricula.text = "Matrícula: ${it.matricula}"
            binding.tvMotivo.text    = "Motivo: ${it.motivo}"
            binding.tvTelefono.text  = "Tel: ${it.telefono}"
        }

        // GPS + Mapa
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Voz (TTS) en español
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                ttsListo = true
            }
        }

        binding.btnSalir.setOnClickListener { ChangeActivity(this, MainActivity::class.java) }
    }

    // Dice una frase por el altavoz (solo si el TTS ya está listo)
    private fun hablar(texto: String) {
        if (ttsListo && texto.isNotBlank()) {
            tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Anuncia 'frase' solo si la clave cambió respecto a la última vez.
    // Se usa para no repetir la misma zona/maniobra en cada tick.
    private fun anunciar(clave: String, frase: String) {
        if (clave != ultimaInstruccionDicha) {
            ultimaInstruccionDicha = clave
            hablar(frase)
        }
    }

    // "Gira a la derecha..." → "gira a la derecha..."
    // Lo usamos para que "En 200 metros, gira..." suene natural.
    private fun enMinuscula(s: String): String =
        if (s.isEmpty()) s else s[0].lowercaseChar() + s.substring(1)

    // 250 → "250 m", 1500 → "1.5 km"
    private fun formatearDistancia(metros: Int): String = when {
        metros < 1000 -> "$metros m"
        else          -> String.format(Locale.US, "%.1f km", metros / 1000.0)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMapToolbarEnabled       = false
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.isTrafficEnabled = false

        // Click en el mapa mueve al usuario (modo prueba)
        mMap.setOnMapClickListener { punto ->
            posicionActual = punto
            marcadorUsuario?.position = punto
            calcularRuta()
        }

        // Permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            iniciarGPS()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    private fun iniciarGPS() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(2f).build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                posicionActual = LatLng(loc.latitude, loc.longitude)

                if (marcadorUsuario == null) {
                    marcadorUsuario = mMap.addMarker(
                        MarkerOptions()
                            .position(posicionActual!!)
                            .anchor(0.5f, 0.5f)
                            .flat(true)
                            .icon(crearIconoUsuario())
                    )
                } else {
                    marcadorUsuario!!.position = posicionActual!!
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionActual!!, 15f))
                calcularRuta()
            }
        }, Looper.getMainLooper())
    }

    // Llama a la API de Google Directions, parsea la respuesta y actualiza la UI
    private fun calcularRuta() {
        val origen = posicionActual ?: return
        binding.tvInstruccion.text = "Calculando ruta..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = "https://maps.googleapis.com/maps/api/directions/json" +
                        "?origin=${origen.latitude},${origen.longitude}" +
                        "&destination=${destino.latitude},${destino.longitude}" +
                        "&mode=driving&language=es&key=$API_KEY"

                val json = OkHttpClient()
                    .newCall(Request.Builder().url(url).build())
                    .execute().body?.string() ?: return@launch

                val obj = JSONObject(json)
                if (obj.getString("status") != "OK") {
                    withContext(Dispatchers.Main) {
                        binding.tvInstruccion.text = "Error: ${obj.getString("status")}"
                    }
                    return@launch
                }

                // Datos que nos interesan de la ruta
                val tramo           = obj.getJSONArray("routes").getJSONObject(0)
                                         .getJSONArray("legs").getJSONObject(0)
                val distancia       = tramo.getJSONObject("distance").getString("text")
                val distanciaMetros = tramo.getJSONObject("distance").getInt("value")
                val tiempo          = tramo.getJSONObject("duration").getString("text")
                val segundos        = tramo.getJSONObject("duration").getInt("value")
                val steps           = tramo.getJSONArray("steps")

                // Metros que te quedan hasta la PRÓXIMA maniobra (no al destino)
                val metrosHastaManiobra = steps.getJSONObject(0)
                    .getJSONObject("distance").getInt("value")
                val hayManiobraProxima  = steps.length() > 1
                val instruccionActual   = parsearInstruccion(steps.getJSONObject(0))
                    .substringBefore(" · ")
                val instruccionSiguiente = if (hayManiobraProxima)
                    parsearInstruccion(steps.getJSONObject(1)).substringBefore(" · ")
                else ""
                // "Después" ya muestra lo que viene tras la próxima maniobra
                val siguiente = if (steps.length() > 2)
                    "Después: ${parsearInstruccion(steps.getJSONObject(2)).substringBefore(" · ")}"
                else "Después: llegarás al destino"

                val puntos = decodificarPolyline(
                    obj.getJSONArray("routes").getJSONObject(0)
                        .getJSONObject("overview_polyline").getString("points")
                )

                withContext(Dispatchers.Main) {
                    dibujarRuta(puntos)

                    when {
                        // Detección de llegada
                        distanciaMetros <= 5 -> {
                            val aviso = "Ya has llegado"
                            binding.tvInstruccion.text = aviso
                            anunciar("llegada", aviso)
                            if (!popupMostrado) {
                                popupMostrado = true
                                if (fase == FASE_RECOGIDA) mostrarPopupRecogida()
                                else                      mostrarPopupEntrega()
                            }
                        }
                        // Hay una maniobra próxima: avisos por zonas
                        hayManiobraProxima -> {
                            val distTexto = formatearDistancia(metrosHastaManiobra)
                            binding.tvInstruccion.text = "$distTexto · $instruccionSiguiente"

                            val zona = when {
                                metrosHastaManiobra <= 30  -> "ahora"
                                metrosHastaManiobra <= 150 -> "150"
                                metrosHastaManiobra <= 500 -> "500"
                                else                        -> "lejos"
                            }
                            // Clave = maniobra + zona → cada zona se anuncia solo una vez
                            val clave = "$instruccionSiguiente|$zona"
                            val frase = when (zona) {
                                "ahora" -> instruccionSiguiente
                                "150"   -> "En 150 metros, ${enMinuscula(instruccionSiguiente)}"
                                "500"   -> "En 500 metros, ${enMinuscula(instruccionSiguiente)}"
                                else    -> instruccionActual   // "Continúa por Calle X"
                            }
                            anunciar(clave, frase)
                        }
                        // Recta final sin más maniobras antes del destino
                        else -> {
                            binding.tvInstruccion.text = instruccionActual
                            anunciar(instruccionActual, instruccionActual)
                        }
                    }

                    binding.tvDistancia.text = distancia
                    binding.tvTiempo.text    = tiempo
                    binding.tvSiguiente.text = siguiente

                    // Hora estimada de llegada (ahora + segundos)
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.SECOND, segundos)
                    binding.tvHoraLlegada.text = String.format("%02d:%02d",
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvInstruccion.text = "Error: ${e.message}"
                }
            }
        }
    }

    // --- Popups de llegada ---------------------------------------------------

    // Muestra el popup al llegar al punto de recogida.
    // Al pulsar "Recogido": marca vehiculo_recogido=true y relanza MapGPS hacia el destino.
    private fun mostrarPopupRecogida() {
        val viaje = RecogidasRepo.Recogidas.find { it.id == recogidaID }
        mostrarPopup("¿Has recogido el vehículo?", "Recogido", "#1565C0") {
            val destLat = viaje?.destinoLat ?: 0.0
            val destLng = viaje?.destinoLng ?: 0.0
            if (destLat == 0.0 && destLng == 0.0) {
                Toast.makeText(this, "El servicio no tiene destino definido", Toast.LENGTH_LONG).show()
                return@mostrarPopup
            }
            lifecycleScope.launch {
                marcarVehiculoRecogido()
                abrirNavegacion(destLat, destLng, FASE_ENTREGA)
            }
        }
    }

    // Muestra el popup al llegar al destino final.
    // Al pulsar "Entregado": marca el servicio como "Terminado" y vuelve al Main.
    private fun mostrarPopupEntrega() {
        mostrarPopup("¿Has entregado el vehículo?", "Entregado", "#2E7D32") {
            lifecycleScope.launch {
                marcarEstado("Terminado")
                ChangeActivity(this@MapGPS, MainActivity::class.java)
            }
        }
    }

    // Helper común: infla dialog_popup.xml, rellena datos y muestra el AlertDialog.
    private fun mostrarPopup(
        pregunta: String,
        textoBoton: String,
        colorBotonHex: String,
        onAccion: () -> Unit
    ) {
        val viaje = RecogidasRepo.Recogidas.find { it.id == recogidaID }
        val popup = DialogPopupBinding.inflate(layoutInflater)

        popup.tvPreguntaPopup.text  = pregunta
        popup.tvClientePopup.text   = viaje?.cliente?.let { "Cliente: $it" } ?: ""
        popup.tvMatriculaPopup.text = viaje?.matricula?.let { "Matrícula: $it" } ?: ""
        popup.tvMotivoPopup.text    = viaje?.motivo?.let { "Motivo: $it" } ?: ""

        popup.btnAccionPopup.text = textoBoton
        popup.btnAccionPopup.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(colorBotonHex))

        val dialog = AlertDialog.Builder(this)
            .setView(popup.root)
            .setCancelable(false)
            .create()
        // Fondo transparente para que se vea la CardView limpia
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        popup.btnAccionPopup.setOnClickListener {
            dialog.dismiss()
            onAccion()
        }
        dialog.show()
    }

    // Marca en Supabase el servicio actual como "vehículo recogido"
    private suspend fun marcarVehiculoRecogido() {
        if (recogidaID == -1) return
        try {
            SupabaseClient.client.from("servicios").update(
                { set("vehiculo_recogido", true) }
            ) {
                filter { eq("id", recogidaID) }
            }
        } catch (e: Exception) {
            android.util.Log.e("MAPGPS", "Error marcando vehiculo_recogido", e)
        }
    }

    // Cambia el campo "estado" del servicio actual (ej: "Terminado")
    private suspend fun marcarEstado(estado: String) {
        if (recogidaID == -1) return
        try {
            SupabaseClient.client.from("servicios").update(
                { set("estado", estado) }
            ) {
                filter { eq("id", recogidaID) }
            }
        } catch (e: Exception) {
            android.util.Log.e("MAPGPS", "Error actualizando estado", e)
        }
    }

    // Relanza MapGPS en otra fase (ej: pasar de RECOGIDA a ENTREGA).
    private fun abrirNavegacion(lat: Double, lng: Double, nuevaFase: String) {
        val i = Intent(this, MapGPS::class.java).apply {
            putExtra(EXTRA_LAT,  lat)
            putExtra(EXTRA_LNG,  lng)
            putExtra(EXTRA_ID,   recogidaID)
            putExtra(EXTRA_FASE, nuevaFase)
        }
        startActivity(i)
        finish()
    }

    // --- Parseo de instrucciones y ruta --------------------------------------

    private fun parsearInstruccion(step: JSONObject): String {
        val maniobra  = if (step.has("maneuver")) step.getString("maneuver") else ""
        val html      = step.getString("html_instructions")
        val distancia = step.getJSONObject("distance").getString("text")

        // Limpia el HTML para quedarnos con el texto plano
        val textoLimpio = html
            .replace(Regex("<div[^>]*>"), " ")
            .replace("</div>", "")
            .replace(Regex("<b>|</b>"), "")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Todas las <b>...</b>. Filtramos los puntos cardinales para quedarnos con calles.
        val cardinales = setOf("norte","sur","este","oeste",
            "noreste","noroeste","sureste","suroeste")
        val bolds        = Regex("<b>(.*?)</b>").findAll(html).map { it.groupValues[1] }.toList()
        val calle        = bolds.lastOrNull  { it.lowercase() !in cardinales } ?: ""
        val primeraCalle = bolds.firstOrNull { it.lowercase() !in cardinales } ?: ""
        // Para rotondas, sacamos el número de salida
        val salida = if (maniobra.contains("roundabout"))
            Regex("(\\d+)").find(textoLimpio)?.value ?: "" else ""

        val texto = when {
            // uturn y arrive primero: "uturn-right" contiene "turn-right"
            maniobra.contains("uturn")  -> "Dé la vuelta"
            maniobra.contains("arrive") -> "Ha llegado al destino"
            maniobra.contains("roundabout") -> when {
                salida.isNotEmpty() && calle.isNotEmpty() -> "En la rotonda toma la ${salida}ª salida hacia $calle"
                salida.isNotEmpty()                       -> "En la rotonda toma la ${salida}ª salida"
                else                                      -> textoLimpio
            }
            maniobra.contains("sharp-right") ->
                if (calle.isNotEmpty()) "Gira cerrado a la derecha en $calle" else textoLimpio
            maniobra.contains("sharp-left") ->
                if (calle.isNotEmpty()) "Gira cerrado a la izquierda en $calle" else textoLimpio
            maniobra.contains("slight-right") ->
                if (calle.isNotEmpty()) "Gira ligeramente a la derecha hacia $calle" else textoLimpio
            maniobra.contains("slight-left") ->
                if (calle.isNotEmpty()) "Gira ligeramente a la izquierda hacia $calle" else textoLimpio
            maniobra.contains("turn-right") ->
                if (calle.isNotEmpty()) "Gira a la derecha en $calle" else textoLimpio
            maniobra.contains("turn-left") ->
                if (calle.isNotEmpty()) "Gira a la izquierda en $calle" else textoLimpio
            maniobra.contains("keep-right") ->
                if (calle.isNotEmpty()) "Manténgase a la derecha hacia $calle" else textoLimpio
            maniobra.contains("keep-left") ->
                if (calle.isNotEmpty()) "Manténgase a la izquierda hacia $calle" else textoLimpio
            maniobra.contains("ramp") || maniobra.contains("fork") ->
                if (calle.isNotEmpty()) "Coja la salida hacia $calle" else textoLimpio
            maniobra.contains("merge") ->
                if (calle.isNotEmpty()) "Incorpórate a $calle" else textoLimpio
            // Sin maniobra / depart / straight → "Continúa por X" (antes salía "ve al norte...")
            maniobra.isEmpty() || maniobra.contains("depart") || maniobra.contains("straight") ->
                if (primeraCalle.isNotEmpty()) "Continúa por $primeraCalle" else "Sigue recto"
            else -> textoLimpio
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

    // Icono azul para el marcador del usuario
    private fun crearIconoUsuario(): BitmapDescriptor {
        val bitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.parseColor("#554285F4")   // halo azul translúcido
            canvas.drawCircle(30f, 30f, 28f, it)
        }
        Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.parseColor("#4285F4")     // azul sólido
            canvas.drawCircle(30f, 30f, 14f, it)
        }
        Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color       = Color.WHITE               // borde blanco
            it.style       = Paint.Style.STROKE
            it.strokeWidth = 3f
            canvas.drawCircle(30f, 30f, 14f, it)
        }
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            iniciarGPS()
        }
    }

    private fun <T> ChangeActivity(context: Context, cls: Class<T>){
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}
