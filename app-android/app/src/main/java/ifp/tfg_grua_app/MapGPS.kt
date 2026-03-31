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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MapGPS : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapGpsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var destino: LatLng
    private var posicionActual: LatLng? = null
    private var marcadorUsuario: Marker? = null
    private var marcadorDestino: Marker? = null
    private var rutaPolyline: Polyline? = null

    companion object {
        const val API_KEY = "AIzaSyAxLBlpI6vtCy658BaQDMH8Mpmepl6CafM"
        const val EXTRA_LAT = "destino_lat"
        const val EXTRA_LNG = "destino_lng"
        const val EXTRA_ID = "viaje_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapGpsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recoge datos del Intent
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
        val recogidaID = intent.getIntExtra(EXTRA_ID, -1)
        destino = LatLng(lat, lng)

        // Mostrar datos del viaje en el panel
        val viaje = RecogidasRepo.Recogidas.find { it.id == recogidaID }
        viaje?.let {
            binding.tvCliente.text = "Cliente: ${it.cliente}"
            binding.tvMatricula.text = "Matrícula: ${it.matricula}"
            binding.tvMotivo.text = "Motivo: ${it.motivo}"
            binding.tvTelefono.text = "Tel: ${it.telefono}"
        }

        // GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnSalir.setOnClickListener {
            ChangeActivity(this, MainActivity::class.java)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Marcador destino
        marcadorDestino = mMap.addMarker(
            MarkerOptions().position(destino).title("Destino")
        )

        mMap.setOnMapClickListener { puntoClickado ->
            posicionActual = puntoClickado
            marcadorUsuario?.position = puntoClickado
            calcularRuta()
        }

        // Permisos y arranca GPS
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            iniciarGPS()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    private fun iniciarGPS() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 4000L
        ).setMinUpdateDistanceMeters(5f).build()

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                posicionActual = LatLng(loc.latitude, loc.longitude)

                if (marcadorUsuario == null) {
                    marcadorUsuario = mMap.addMarker(
                        MarkerOptions()
                            .position(posicionActual!!)
                            .title("Tú")
                            .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    // Calcular ruta y centrar cámara
                    calcularRuta()
                } else {
                    marcadorUsuario!!.position = posicionActual!!
                }

                // Cámara sigue al usuario
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(posicionActual!!, 15f)
                )
            }
        }, Looper.getMainLooper())
    }

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

                // Parsear datos
                val ruta = obj.getJSONArray("routes").getJSONObject(0)
                val tramo = ruta.getJSONArray("legs").getJSONObject(0)
                val distancia = tramo.getJSONObject("distance").getString("text")
                val tiempo = tramo.getJSONObject("duration").getString("text")
                val instruccion = parsearInstruccion(tramo.getJSONArray("steps").getJSONObject(0))
                val puntos = decodificarPolyline(
                    ruta.getJSONObject("overview_polyline").getString("points")
                )

                withContext(Dispatchers.Main) {
                    dibujarRuta(puntos)
                    binding.tvInstruccion.text = instruccion
                    binding.tvDistancia.text = distancia
                    binding.tvTiempo.text = tiempo
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvInstruccion.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun parsearInstruccion(step: JSONObject): String {
        val maniobra = if (step.has("maneuver")) step.getString("maneuver") else ""
        val html = step.getString("html_instructions")
        val distancia = step.getJSONObject("distance").getString("text")
        val calle = Regex("<b>(.*?)</b>").find(html)?.groupValues?.get(1) ?: ""

        val texto = when {
            maniobra.contains("turn-right") -> "Gira a la derecha${if (calle.isNotEmpty()) " en $calle" else ""}"
            maniobra.contains("turn-left") -> "Gira a la izquierda${if (calle.isNotEmpty()) " en $calle" else ""}"
            maniobra.contains("roundabout") -> {
                val salida = Regex("(\\d+)").find(html)?.value ?: ""
                "En la rotonda, toma la ${salida}ª salida${if (calle.isNotEmpty()) " hacia $calle" else ""}"
            }
            maniobra.contains("keep-right") -> "Manténgase en el carril derecho${if (calle.isNotEmpty()) " hacia $calle" else ""}"
            maniobra.contains("keep-left") -> "Manténgase en el carril izquierdo${if (calle.isNotEmpty()) " hacia $calle" else ""}"
            maniobra.contains("ramp") ||
                    maniobra.contains("fork") -> "Coja la salida${if (calle.isNotEmpty()) " hacia $calle" else ""}"
            maniobra.contains("uturn") -> "Dé la vuelta"
            maniobra.contains("arrive") -> "Ha llegado al destino"
            else  -> "Continúe recto${if (calle.isNotEmpty()) " por $calle" else ""}"
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

        if(context is Activity){
            context.finish()
        }
    }
}