package ifp.tfg_grua_app

import android.content.Context

/**
 * Guarda los datos del usuario logueado en SharedPreferences,
 * para que sigan disponibles aunque se cierre y abra la app.
 */
object SesionUsuario {
    private const val PREFS = "sesion_prefs"
    private const val KEY_ID = "user_id"
    private const val KEY_MAIL = "user_mail"
    private const val KEY_NOMBRE = "user_nombre"
    private const val KEY_ROL = "user_rol"

    fun guardar(context: Context, usuario: Usuario) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_ID, usuario.id ?: -1)
            .putString(KEY_MAIL, usuario.mail)
            .putString(KEY_NOMBRE, usuario.nombre)
            .putString(KEY_ROL, usuario.rol)
            .apply()
    }

    fun cerrarSesion(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    fun haySesion(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ID, -1) != -1
    }

    fun getId(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_ID, -1)

    fun getMail(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MAIL, null)

    fun getNombre(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NOMBRE, null)

    fun getRol(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ROL, null)
}
