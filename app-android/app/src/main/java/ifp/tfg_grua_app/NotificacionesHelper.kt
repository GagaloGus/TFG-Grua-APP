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

// Ayudas para lanzar notificaciones locales cuando llega una nueva recogida.
object NotificacionesHelper {

    private const val CANAL_ID     = "nuevas_recogidas"
    private const val CANAL_NOMBRE = "Nuevas recogidas"
    private const val CANAL_DESC   = "Avisos cuando te asignan una recogida nueva"

    // Crea el canal de notificación (solo hace falta en Android 8+).
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID, CANAL_NOMBRE, NotificationManager.IMPORTANCE_HIGH
            ).apply { description = CANAL_DESC }

            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(canal)
        }
    }

    // Muestra una notificación con título y texto. Al pulsarla abre MainActivity.
    fun mostrar(context: Context, titulo: String, texto: String, id: Int) {
        // En Android 13+ hace falta permiso POST_NOTIFICATIONS en tiempo de ejecución
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permiso = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            )
            if (permiso != PackageManager.PERMISSION_GRANTED) return
        }

        // Al tocar la notificación → abrimos MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(context).notify(id, notif)
    }
}
