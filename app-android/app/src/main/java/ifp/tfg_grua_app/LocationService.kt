package ifp.tfg_grua_app

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var destino: LatLng

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val lat = intent?.getDoubleExtra("lat", 0.0) ?: 0.0
        val lng = intent?.getDoubleExtra("lng", 0.0) ?: 0.0

        destino = LatLng(lat, lng)

        startForegroundService()
        startLocationUpdates()

        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "canal")
            .setContentTitle("Seguimiento activo")
            .setContentText("Detectando llegada al destino")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 3000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                for (location in result.locations) {

                    val actual = LatLng(location.latitude, location.longitude)

                    if (haLlegado(actual, destino)) {
                        stopSelf() // detener servicio
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun haLlegado(actual: LatLng, destino: LatLng): Boolean {

        val results = FloatArray(1)

        Location.distanceBetween(
            actual.latitude, actual.longitude,
            destino.latitude, destino.longitude,
            results
        )

        return results[0] < 50
    }

    override fun onBind(intent: Intent?) = null
}