package ifp.tfg_grua_app

import android.content.Context

// Guarda los datos del usuario logueado en SharedPreferences.
// El flag "recordar" decide si la sesión sobrevive al cierre de la app.
object SesionUsuario {
    private const val PREFS = "sesion_prefs"
    private const val KEY_ID = "user_id"
    private const val KEY_MAIL = "user_mail"
    private const val KEY_NOMBRE = "user_nombre"
    private const val KEY_ROL = "user_rol"
    private const val KEY_NUM_EMPLEADO = "user_num_empleado"
    private const val KEY_RECORDAR = "user_recordar"

    // Atajo para no repetir la llamada en cada getter
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun guardar(context: Context, usuario: Usuario, recordar: Boolean) {
        prefs(context).edit()
            .putString(KEY_ID, usuario.id)
            .putString(KEY_MAIL, usuario.mail)
            .putString(KEY_NOMBRE, usuario.nombre)
            .putString(KEY_ROL, usuario.rol)
            .putInt(KEY_NUM_EMPLEADO, usuario.numEmpleado ?: -1)
            .putBoolean(KEY_RECORDAR, recordar)
            .apply()
    }

    fun cerrarSesion(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // true si hay sesión guardada Y se marcó "Recuérdame"
    fun haySesionPersistente(context: Context): Boolean {
        val p = prefs(context)
        return p.getString(KEY_ID, null) != null && p.getBoolean(KEY_RECORDAR, false)
    }

    fun getId(context: Context): String?     = prefs(context).getString(KEY_ID, null)
    fun getMail(context: Context): String?   = prefs(context).getString(KEY_MAIL, null)
    fun getNombre(context: Context): String? = prefs(context).getString(KEY_NOMBRE, null)
    fun getRol(context: Context): String?    = prefs(context).getString(KEY_ROL, null)

    // num_empleado: -1 significa "no guardado" → devolvemos null
    fun getNumEmpleado(context: Context): Int? {
        val v = prefs(context).getInt(KEY_NUM_EMPLEADO, -1)
        return if (v == -1) null else v
    }
}
