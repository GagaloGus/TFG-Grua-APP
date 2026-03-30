package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MapGPS : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapGpsBinding

    // Coordenadas
    private val origen  = LatLng(40.34225, -3.52372)   // Taller 1
    private val destino = LatLng(40.43394, -3.63135)   // Taller 2

    private var marcadorUsuario: Marker? = null
    private var posicionActual = origen

    private var marcadorOrigen:  Marker? = null
    private var marcadorDestino: Marker? = null

    private var rutaPolyline:    Polyline? = null

    companion object {
        const val API_KEY = "AIzaSyAxLBlpI6vtCy658BaQDMH8Mpmepl6CafM"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMapGpsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnCancelar.setOnClickListener {
            ChangeActivity(this, MainActivity::class.java)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Marcador origen
        marcadorOrigen = mMap.addMarker(
            MarkerOptions()
                .position(origen)
                .title("Taller 1")
                .icon(getScaledMarker(R.drawable.prueba))
        )

        // Marcador destino
        marcadorDestino = mMap.addMarker(
            MarkerOptions()
                .position(destino)
                .title("Taller 2")
                .icon(getScaledMarker(R.drawable.prueba))
        )
        // Click en el mapa
        mMap.setOnMapClickListener { puntoClickado ->
            moverUsuarioYRecalcular(puntoClickado)
        }

        val bounds = LatLngBounds.Builder()
            .include(origen)
            .include(destino)
            .build()
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))

        // Calcular y dibujar la ruta real por calles
        calcularRuta()
    }

    private fun calcularRuta() {
        binding.tvInstruccion.text = "Calculando ruta..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${origen.latitude},${origen.longitude}" +
                        "&destination=${destino.latitude},${destino.longitude}" +
                        "&mode=driving" +
                        "&language=es" +
                        "&key=$API_KEY"

                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val json = client.newCall(request).execute().body?.string() ?: return@launch

                val obj = JSONObject(json)

                if (obj.getString("status") != "OK") {
                    withContext(Dispatchers.Main) {

                        val status = obj.getString("status")
                        val mensaje = if (obj.has("error_message"))
                            obj.getString("error_message")
                        else "Sin mensaje"
                        binding.tvInstruccion.text = "$status: $mensaje"
                    }
                    return@launch
                }

                val route = obj.getJSONArray("routes").getJSONObject(0)
                val leg = route.getJSONArray("legs").getJSONObject(0)

                // Distancia y tiempo totales
                val distancia = leg.getJSONObject("distance").getString("text")
                val tiempo = leg.getJSONObject("duration").getString("text")

                val primeraInstruccion = leg.getJSONArray("steps")
                    .getJSONObject(0)
                    .getString("html_instructions")
                    .replace(Regex("<[^>]*>"), " ")
                    .trim()

                // Navegación real por calles
                val polylineEncoded = route
                    .getJSONObject("overview_polyline")
                    .getString("points")
                val puntos = decodificarPolyline(polylineEncoded)

                withContext(Dispatchers.Main) {
                    // Dibujar ruta azul en el mapa
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

                    // Actualizar UI
                    binding.tvInstruccion.text = primeraInstruccion
                    binding.tvDistancia.text = distancia
                    binding.tvTiempo.text = tiempo

                    if (marcadorUsuario == null) {
                        marcadorUsuario = mMap.addMarker(
                            MarkerOptions()
                                .position(posicionActual)
                                .title("Tú")
                                .icon(crearIconoUsuario())
                                .zIndex(10f)
                        )
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvInstruccion.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun decodificarPolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int; var shift = 0; var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            shift = 0; result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }

    // Mueve el marcador al punto clickado y recalcula la ruta desde ahí
    private fun moverUsuarioYRecalcular(nuevaPosicion: LatLng) {
        posicionActual = nuevaPosicion

        // Mover o crear el marcador de usuario
        if (marcadorUsuario == null) {
            marcadorUsuario = mMap.addMarker(
                MarkerOptions()
                    .position(nuevaPosicion)
                    .title("Tú")
                    .icon(crearIconoUsuario())
                    .zIndex(10f)
            )
        } else {
            val handler = Handler(Looper.getMainLooper())
            val inicio = marcadorUsuario!!.position
            val pasos = 20
            var paso = 0

            val runnable = object : Runnable {
                override fun run() {
                    paso++
                    val t = paso.toFloat() / pasos
                    val tEase = t * t * (3 - 2 * t)  // Ease in-out suave

                    val lat = inicio.latitude  + (nuevaPosicion.latitude  - inicio.latitude)  * tEase
                    val lng = inicio.longitude + (nuevaPosicion.longitude - inicio.longitude) * tEase

                    marcadorUsuario?.position = LatLng(lat, lng)

                    if (paso < pasos) handler.postDelayed(this, 16)
                }
            }
            handler.post(runnable)
        }

        // Mover la cámara al nuevo punto
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nuevaPosicion, 18f))

        // Recalcular la ruta desde la nueva posición al destino
        calcularRutaDesde(nuevaPosicion)
    }

    // Igual que calcularRuta()
    private fun calcularRutaDesde(nuevoOrigen: LatLng) {
        binding.tvInstruccion.text = "Recalculando..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${nuevoOrigen.latitude},${nuevoOrigen.longitude}" +
                        "&destination=${destino.latitude},${destino.longitude}" +
                        "&mode=driving&language=es&key=$API_KEY"

                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val json = client.newCall(request).execute().body?.string() ?: return@launch

                val obj = JSONObject(json)

                if (obj.getString("status") != "OK") {
                    val status  = obj.getString("status")
                    val mensaje = if (obj.has("error_message"))
                        obj.getString("error_message") else "Sin mensaje"
                    withContext(Dispatchers.Main) {
                        binding.tvInstruccion.text = "$status: $mensaje"
                    }
                    return@launch
                }

                val route = obj.getJSONArray("routes").getJSONObject(0)
                val leg = route.getJSONArray("legs").getJSONObject(0)
                val distancia = leg.getJSONObject("distance").getString("text")
                val tiempo = leg.getJSONObject("duration").getString("text")



                val primeraInstruccion = leg.getJSONArray("steps")
                    .getJSONObject(0)
                    .getString("html_instructions")
                    .replace(Regex("<[^>]*>"), " ")
                    .trim()

                val puntos = decodificarPolyline(
                    route.getJSONObject("overview_polyline").getString("points")
                )

                withContext(Dispatchers.Main) {
                    // Redibujar ruta desde la nueva posición
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

                    binding.tvInstruccion.text = primeraInstruccion
                    binding.tvDistancia.text = distancia
                    binding.tvTiempo.text = tiempo
                    if (distancia < "10m") {
                        binding.tvInstruccion.text = "Has llegado"
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvInstruccion.text = "Error: ${e.message}"
                }
            }
        }
    }

    // Círculo azul
    private fun crearIconoUsuario(): BitmapDescriptor {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Halo exterior semitransparente
        Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.parseColor("#4D4285F4")
            canvas.drawCircle(24f, 24f, 22f, it)
        }

        // Círculo azul sólido
        Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.parseColor("#4285F4")
            canvas.drawCircle(24f, 24f, 14f, it)
        }

        // Borde blanco
        Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.WHITE
            it.style  = Paint.Style.STROKE
            it.strokeWidth = 3f
            canvas.drawCircle(24f, 24f, 14f, it)
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun getScaledMarker(drawable: Int): BitmapDescriptor {
        val bitmap = BitmapFactory.decodeResource(resources, drawable)
        val scaled = Bitmap.createScaledBitmap(bitmap, 80, 80, false)
        return BitmapDescriptorFactory.fromBitmap(scaled)
    }

    private fun <T> ChangeActivity(context: Context, cls: Class<T>){
        context.startActivity(Intent(context, cls))

        if(context is Activity){
            context.finish()
        }
    }
}