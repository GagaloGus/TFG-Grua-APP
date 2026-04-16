package ifp.tfg_grua_app

import android.content.Context

/**
 * Guarda los datos del usuario logueado en SharedPreferences,
 * para que sigan disponibles aunque se cierre y abra la app.
 *
 * El flag "recordar" controla si la sesión sobrevive al cierre de la app:
 *   - true  → al volver a abrir la app salta directo a MainActivity.
 *   - false → al cerrar la app se borra la sesión y vuelve a pedir login.
 */
object SesionUsuario {
    private const val PREFS = "sesion_prefs"
    private const val KEY_ID = "user_id"
    private const val KEY_MAIL = "user_mail"
    private const val KEY_NOMBRE = "user_nombre"
    private const val KEY_ROL = "user_rol"
    private const val KEY_NUM_EMPLEADO = "user_num_empleado"
    private const val KEY_RECORDAR = "user_recordar"

    fun guardar(context: Context, usuario: Usuario, recordar: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ID, usuario.id)
            .putString(KEY_MAIL, usuario.mail)
            .putString(KEY_NOMBRE, usuario.nombre)
            .putString(KEY_ROL, usuario.rol)
            .putInt(KEY_NUM_EMPLEADO, usuario.numEmpleado ?: -1)
            .putBoolean(KEY_RECORDAR, recordar)
            .apply()
    }

    fun cerrarSesion(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    /** Devuelve true solo si hay una sesión guardada Y el usuario marcó "Recuérdame". */
    fun haySesionPersistente(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ID, null) != null &&
                prefs.getBoolean(KEY_RECORDAR, false)
    }

    fun getId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ID, null)

    fun getMail(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MAIL, null)

    fun getNombre(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NOMBRE, null)

    fun getRol(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ROL, null)

    /** Devuelve el num_empleado del usuario logueado, o null si no está guardado. */
    fun getNumEmpleado(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val v = prefs.getInt(KEY_NUM_EMPLEADO, -1)
        return if (v == -1) null else v
    }
}
