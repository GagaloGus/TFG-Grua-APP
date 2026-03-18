package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import ifp.tfg_grua_app.databinding.ActivityMapGpsBinding
import ifp.tfg_grua_app.databinding.ActivityMapsBinding

class MapGPS : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapGpsBinding

    private var currentPosition = LatLng(40.4168, -3.7038)
    private var marker: Marker? = null

    private val ubicaciones = listOf(
        LatLng(40.4168, -3.7038),
        LatLng(40.4175, -3.7020),
        LatLng(40.4185, -3.7000),
        LatLng(40.4195, -3.6980)
    )

    private val markers = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMapGpsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        binding.btnBackMain.setOnClickListener {
            val destino = LatLng(40.4168, -3.7038)// o el que quieras

            // 1. Iniciar servicio en segundo plano
            val serviceIntent = Intent(this, LocationService::class.java)
            serviceIntent.putExtra("lat", destino.latitude)
            serviceIntent.putExtra("lng", destino.longitude)

            startForegroundService(serviceIntent)

            // 2. Abrir Google Maps en modo navegación
            val uri = Uri.parse(
                "google.navigation:q=${destino.latitude},${destino.longitude}&mode=d"
            )

            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Ajustar tamaño del icono
    fun getScaledMarker(drawable: Int): BitmapDescriptor {
        val bitmap = BitmapFactory.decodeResource(resources, drawable)
        val scaled = Bitmap.createScaledBitmap(bitmap, 80, 80, false)
        return BitmapDescriptorFactory.fromBitmap(scaled)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

        // Crear marcadores de la lista
        for (pos in ubicaciones) {

            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("Ubicación")
                    .icon(getScaledMarker(R.drawable.prueba))
            )

            if (marker != null) {
                markers.add(marker)
            }
        }

        // Marcador que se mueve
        marker = mMap.addMarker(
            MarkerOptions()
                .position(currentPosition)
                .title("Tu ubicación")
                .icon(getScaledMarker(R.drawable.prueba))
        )

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 16f))

        startFakeMovement()
    }

    private fun startFakeMovement() {

        val handler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {

                currentPosition = LatLng(
                    currentPosition.latitude,
                    currentPosition.longitude + 0.0005
                )

                marker?.position = currentPosition
                mMap.animateCamera(CameraUpdateFactory.newLatLng(currentPosition))

                handler.postDelayed(this, 5000)
            }
        }

        handler.post(runnable)
    }
}