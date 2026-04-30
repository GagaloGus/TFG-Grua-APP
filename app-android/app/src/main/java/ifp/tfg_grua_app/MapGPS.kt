package ifp.tfg_grua_app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
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

    // Voz (text to speech)
    private var tts: TextToSpeech? = null
    private var ttsListo = false
    private var ultimaInstruccionDicha: String? = null   // Evita repetir la misma frase

    companion object {
        const val API_KEY = "AIzaSyAxLBlpI6vtCy658BaQDMH8Mpmepl6CafM"
        const val EXTRA_LAT = "destino_lat"
        const val EXTRA_LNG = "destino_lng"
        const val EXTRA_ID = "viaje_id"
        const val EXTRA_FASE = "fase"
        const val FASE_RECOGIDA = "RECOGIDA"
        const val FASE_ENTREGA = "ENTREGA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapGpsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pantalla completa (sin barra de estado)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Datos que llegan desde MainActivity por Intent
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
        recogidaID = intent.getIntExtra(EXTRA_ID, -1)
        fase = intent.getStringExtra(EXTRA_FASE) ?: FASE_RECOGIDA
        destino = LatLng(lat, lng)

        // Rellena el panel inferior con los datos del viaje
        val viaje = RecogidasRepo.Recogidas.find { it.id == recogidaID }
        if (viaje != null) {
            binding.tvCliente.text = "Cliente: ${viaje.cliente}"
            binding.tvMatricula.text = "Matrícula: ${viaje.matricula}"
            binding.tvMotivo.text = "Motivo: ${viaje.motivo}"
            binding.tvTelefono.text = "Tel: ${viaje.telefono}"
        }

        // Inicializa GPS y carga el mapa
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicializa la voz en español
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                ttsListo = true
            }
        }

        binding.btnSalir.setOnClickListener {
            ChangeActivity(this, MainActivity::class.java)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    // Voz (TTS)
    // Lee una frase por el altavoz si la voz ya está lista
    private fun hablar(texto: String) {
        if (ttsListo && texto.isNotBlank()) {
            tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Solo dice la frase si la clave es nueva (evita repetir cada actualización)
    private fun anunciar(clave: String, frase: String) {
        if (clave != ultimaInstruccionDicha) {
            ultimaInstruccionDicha = clave
            hablar(frase)
        }
    }

    // "Gira a la derecha" → "gira a la derecha"
    private fun enMinuscula(s: String): String {
        if (s.isEmpty()) {
            return s
        }
        return s[0].lowercaseChar() + s.substring(1)
    }

    // 250 → "250 m"  ·  1500 → "1.5 km"
    private fun formatearDistancia(metros: Int): String {
        if (metros < 1000) {
            return "$metros m"
        }
        return String.format(Locale.US, "%.1f km", metros / 1000.0)
    }

    // Mapa y GPS

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.isTrafficEnabled = false

        // Tocar en el mapa mueve al usuario (modo prueba)
        mMap.setOnMapClickListener { punto ->
            posicionActual = punto
            marcadorUsuario?.position = punto
            calcularRuta()
        }

        // Pide permiso de ubicación si aún no está concedido
        val concedido = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (concedido) {
            iniciarGPS()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001
            )
        }
    }

    // Empieza a recibir actualizaciones del GPS cada segundo o cada 2 metros
    private fun iniciarGPS() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        val concedido = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!concedido) {
            return
        }

        fusedLocationClient.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val nueva = LatLng(loc.latitude, loc.longitude)
                posicionActual = nueva

                // Crea el marcador la primera vez, mueve el existente las siguientes
                if (marcadorUsuario == null) {
                    marcadorUsuario = mMap.addMarker(
                        MarkerOptions()
                            .position(nueva)
                            .anchor(0.5f, 0.5f)
                            .flat(true)
                            .icon(crearIconoUsuario())
                    )
                } else {
                    marcadorUsuario!!.position = nueva
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nueva, 15f))
                calcularRuta()
            }
        }, Looper.getMainLooper())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            iniciarGPS()
        }
    }

    // Cálculo de la ruta
    // Llama a la API de Directions, parsea el JSON y actualiza la pantalla
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

                // Datos del primer "leg" de la primera ruta
                val tramo = obj.getJSONArray("routes").getJSONObject(0)
                                         .getJSONArray("legs").getJSONObject(0)
                val distancia = tramo.getJSONObject("distance").getString("text")
                val distanciaMetros = tramo.getJSONObject("distance").getInt("value")
                val tiempo = tramo.getJSONObject("duration").getString("text")
                val segundos = tramo.getJSONObject("duration").getInt("value")
                val steps = tramo.getJSONArray("steps")

                // Metros y maniobra inmediatamente siguiente
                val metrosHastaManiobra = steps.getJSONObject(0)
                    .getJSONObject("distance").getInt("value")
                val hayManiobraProxima = steps.length() > 1
                val instruccionActual = parsearInstruccion(steps.getJSONObject(0))
                    .substringBefore(" · ")

                val instruccionSiguiente: String
                if (hayManiobraProxima) {
                    instruccionSiguiente = parsearInstruccion(steps.getJSONObject(1))
                        .substringBefore(" · ")
                } else {
                    instruccionSiguiente = ""
                }

                // "Después" muestra lo que viene tras la próxima maniobra
                val siguiente: String
                if (steps.length() > 2) {
                    val texto = parsearInstruccion(steps.getJSONObject(2)).substringBefore(" · ")
                    siguiente = "Después: $texto"
                } else {
                    siguiente = "Después: llegarás al destino"
                }

                val puntos = decodificarPolyline(
                    obj.getJSONArray("routes").getJSONObject(0)
                        .getJSONObject("overview_polyline").getString("points")
                )

                withContext(Dispatchers.Main) {
                    dibujarRuta(puntos)
                    actualizarUI(
                        distancia, tiempo, siguiente, segundos,
                        distanciaMetros, hayManiobraProxima,
                        metrosHastaManiobra, instruccionActual, instruccionSiguiente
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvInstruccion.text = "Error: ${e.message}"
                }
            }
        }
    }

    // Aplica todos los datos de la ruta al panel inferior y a la voz
    private fun actualizarUI(
        distancia: String, tiempo: String, siguiente: String, segundos: Int,
        distanciaMetros: Int, hayManiobraProxima: Boolean,
        metrosHastaManiobra: Int, instruccionActual: String, instruccionSiguiente: String
    ) {
        // Has llegado al destino
        if (distanciaMetros <= 5) {
            val aviso = "Ya has llegado"
            binding.tvInstruccion.text = aviso
            anunciar("llegada", aviso)

            if (!popupMostrado) {
                popupMostrado = true
                if (fase == FASE_RECOGIDA) {
                    mostrarPopupRecogida()
                } else {
                    mostrarPopupEntrega()
                }
            }
        }
        // Hay una maniobra próxima: avisos por zonas
        else if (hayManiobraProxima) {
            val distTexto = formatearDistancia(metrosHastaManiobra)
            binding.tvInstruccion.text = "$distTexto · $instruccionSiguiente"

            // Calculamos en qué "zona" está el conductor respecto a la maniobra
            val zona: String
            if (metrosHastaManiobra <= 30) {
                zona = "ahora"
            } else if (metrosHastaManiobra <= 150) {
                zona = "150"
            } else if (metrosHastaManiobra <= 500) {
                zona = "500"
            } else {
                zona = "lejos"
            }

            // Frase a decir según la zona
            val frase: String
            if (zona == "ahora") {
                frase = instruccionSiguiente
            } else if (zona == "150") {
                frase = "En 150 metros, ${enMinuscula(instruccionSiguiente)}"
            } else if (zona == "500") {
                frase = "En 500 metros, ${enMinuscula(instruccionSiguiente)}"
            } else {
                frase = instruccionActual   // "Continúa por Calle X"
            }

            // Cada par maniobra+zona se anuncia una sola vez
            anunciar("$instruccionSiguiente|$zona", frase)
        }
        // Recta final, no hay más maniobras antes del destino
        else {
            binding.tvInstruccion.text = instruccionActual
            anunciar(instruccionActual, instruccionActual)
        }

        binding.tvDistancia.text = distancia
        binding.tvTiempo.text = tiempo
        binding.tvSiguiente.text = siguiente

        // Hora estimada de llegada = ahora + duración de la ruta
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, segundos)
        binding.tvHoraLlegada.text = String.format("%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE))
    }

    // Popups
    // Popup al llegar al cliente. Al pulsar "Recogido" pasa a fase ENTREGA.
    private fun mostrarPopupRecogida() {
        val viaje = RecogidasRepo.Recogidas.find { it.id == recogidaID }

        mostrarPopup("¿Has recogido el vehículo?", "Recogido", "#1565C0") {
            val destLat: Double
            val destLng: Double
            if (viaje != null) {
                destLat = viaje.destinoLat
                destLng = viaje.destinoLng
            } else {
                destLat = 0.0
                destLng = 0.0
            }

            if (destLat == 0.0 && destLng == 0.0) {
                toast("El servicio no tiene destino definido")
                return@mostrarPopup
            }

            lifecycleScope.launch {
                marcarVehiculoRecogido()
                abrirNavegacion(destLat, destLng, FASE_ENTREGA)
            }
        }
    }

    // Popup al llegar al destino. Al pulsar "Entregado" cierra el servicio.
    private fun mostrarPopupEntrega() {
        mostrarPopup("¿Has entregado el vehículo?", "Entregado", "#2E7D32") {
            lifecycleScope.launch {
                marcarEstado("Terminado")
                ChangeActivity(this@MapGPS, MainActivity::class.java)
            }
        }
    }

    // Helper común: infla dialog_popup.xml, lo rellena y lo muestra
    private fun mostrarPopup(
        pregunta: String,
        textoBoton: String,
        colorBotonHex: String,
        onAccion: () -> Unit
    ) {
        val viaje = RecogidasRepo.Recogidas.find { it.id == recogidaID }
        val popup = DialogPopupBinding.inflate(layoutInflater)

        popup.tvPreguntaPopup.text = pregunta
        if (viaje != null) {
            popup.tvClientePopup.text = "Cliente: ${viaje.cliente}"
            popup.tvMatriculaPopup.text = "Matrícula: ${viaje.matricula}"
            popup.tvMotivoPopup.text = "Motivo: ${viaje.motivo}"
        }

        popup.btnAccionPopup.text = textoBoton
        popup.btnAccionPopup.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(colorBotonHex))

        val dialog = AlertDialog.Builder(this)
            .setView(popup.root)
            .setCancelable(false)
            .create()

        // Fondo transparente para que se vea solo la CardView
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        popup.btnAccionPopup.setOnClickListener {
            dialog.dismiss()
            onAccion()
        }
        dialog.show()
    }

    // Acciones sobre Supabase
    // Marca en la BBDD que el vehículo ha sido recogido
    private suspend fun marcarVehiculoRecogido() {
        if (recogidaID == -1) {
            return
        }
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

    // Cambia el campo "estado" del servicio actual (por ejemplo "Terminado")
    private suspend fun marcarEstado(estado: String) {
        if (recogidaID == -1) {
            return
        }
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

    // Relanza esta misma activity con la nueva fase (RECOGIDA → ENTREGA)
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

    // Parseo de instrucciones de Google
    // Convierte un step de Directions a un texto en castellano + " · distancia"
    private fun parsearInstruccion(step: JSONObject): String {
        val maniobra: String
        if (step.has("maneuver")) {
            maniobra = step.getString("maneuver")
        } else {
            maniobra = ""
        }
        val html = step.getString("html_instructions")
        val distancia = step.getJSONObject("distance").getString("text")

        // Limpia el HTML para quedarnos con texto plano
        val textoLimpio = html
            .replace(Regex("<div[^>]*>"), " ")
            .replace("</div>", "")
            .replace(Regex("<b>|</b>"), "")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Saca todas las cadenas <b>...</b> del HTML
        val cardinales = setOf("norte", "sur", "este", "oeste",
            "noreste", "noroeste", "sureste", "suroeste")
        val bolds = Regex("<b>(.*?)</b>").findAll(html).map { it.groupValues[1] }.toList()

        // "calle" = última cadena en negrita que no sea un punto cardinal
        var calle = ""
        for (b in bolds) {
            if (b.lowercase() !in cardinales) {
                calle = b
            }
        }

        // "primeraCalle" = primera cadena en negrita que no sea un punto cardinal
        var primeraCalle = ""
        for (b in bolds) {
            if (b.lowercase() !in cardinales) {
                primeraCalle = b
                break
            }
        }

        // Para rotondas saca el número de salida (1, 2, 3...)
        val salida: String
        if (maniobra.contains("roundabout")) {
            val match = Regex("(\\d+)").find(textoLimpio)
            if (match != null) {
                salida = match.value
            } else {
                salida = ""
            }
        } else {
            salida = ""
        }

        // Hay 15 tipos de maniobra distintos: el when es la forma más legible
        val texto = when {
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
            maniobra.isEmpty() || maniobra.contains("depart") || maniobra.contains("straight") ->
                if (primeraCalle.isNotEmpty()) "Continúa por $primeraCalle" else "Sigue recto"
            else -> textoLimpio
        }

        return "$texto · $distancia"
    }

    // Pintar ruta y marcador
    // Pinta la línea azul de la ruta sobre el mapa
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

    // Convierte la "polyline encoded" de Google en una lista de coordenadas
    private fun decodificarPolyline(encoded: String): List<LatLng> {
        val puntos = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            // Decodifica latitud
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            if (result and 1 != 0) {
                lat += (result shr 1).inv()
            } else {
                lat += result shr 1
            }

            // Decodifica longitud
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            if (result and 1 != 0) {
                lng += (result shr 1).inv()
            } else {
                lng += result shr 1
            }

            puntos.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return puntos
    }

    // Crea un círculo azul con halo y borde blanco para el marcador del usuario
    private fun crearIconoUsuario(): BitmapDescriptor {
        val bitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Halo azul translúcido
        val halo = Paint(Paint.ANTI_ALIAS_FLAG)
        halo.color = Color.parseColor("#554285F4")
        canvas.drawCircle(30f, 30f, 28f, halo)

        // Círculo central azul sólido
        val centro = Paint(Paint.ANTI_ALIAS_FLAG)
        centro.color = Color.parseColor("#4285F4")
        canvas.drawCircle(30f, 30f, 14f, centro)

        // Borde blanco alrededor del círculo central
        val borde = Paint(Paint.ANTI_ALIAS_FLAG)
        borde.color = Color.WHITE
        borde.style = Paint.Style.STROKE
        borde.strokeWidth = 3f
        canvas.drawCircle(30f, 30f, 14f, borde)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Funciones útiles
    private fun toast(texto: String) =
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()

    private fun <T> ChangeActivity(context: Context, cls: Class<T>) {
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}
