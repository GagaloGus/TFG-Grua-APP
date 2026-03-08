package ifp.tfg_grua_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import ifp.tfg_grua_app.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

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

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // recorrer lista de ubicaciones
        for (pos in ubicaciones) {

            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("Ubicación")
            )

            if (marker != null) {
                markers.add(marker)
            }
        }

        marker = mMap.addMarker(
            MarkerOptions().position(currentPosition).title("Tu ubicación")
        )

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 16f))

        startFakeMovement()
    }

    private fun startFakeMovement() {

        val handler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {

                // mover un poco a la derecha
                currentPosition = LatLng(
                    currentPosition.latitude,
                    currentPosition.longitude + 0.0005
                )

                marker?.position = currentPosition

                mMap.animateCamera(CameraUpdateFactory.newLatLng(currentPosition))

                handler.postDelayed(this, 5000) // cada 5 segundos
            }
        }

        handler.post(runnable)
    }
}