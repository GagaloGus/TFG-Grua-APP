package ifp.tfg_grua_app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

//Lanza notificaciones locales cuando llega una nueva recogida.
object NotificacionesHelper {

    private const val CANAL_ID = "nuevas_recogidas"

    //Crea el canal
    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL_ID, "Nuevas recogidas", NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(canal)
    }

    fun mostrar(context: Context, titulo: String, texto: String, id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return

        val pending = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Icono grande usando el launcher de la app
        val iconoGrande = ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
            ?.toBitmap()

        val notif = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome) // silueta blanca en la barra de estado
            .setLargeIcon(iconoGrande) // icono a color en la notif
            .setContentTitle(titulo)
            .setContentText(texto)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(context).notify(id, notif)
    }
}
