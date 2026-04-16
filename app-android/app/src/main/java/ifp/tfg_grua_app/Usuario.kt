package ifp.tfg_grua_app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa una fila de la tabla "usuarios" de Supabase.
 * IMPORTANTE: los nombres entre comillas en @SerialName deben coincidir EXACTAMENTE
 * con los nombres de las columnas de tu tabla en Supabase (mayúsculas/minúsculas).
 * Si alguna columna de tu tabla se llama distinto, ajusta el @SerialName.
 */
@Serializable
data class Usuario(
    val id: Int? = null,
    val nombre: String? = null,
    val apellido1: String? = null,
    val apellido2: String? = null,
    val telefono: String? = null,
    val mail: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("servicios_completados") val serviciosCompletados: Int? = null,
    @SerialName("licencia_conducir") val licenciaConducir: String? = null,
    val password: String? = null,
    val disponibilidad: Boolean? = null,
    val rol: String? = null,
    @SerialName("num_empleado") val numEmpleado: Int? = null,
    @SerialName("vehiculo_asignado") val vehiculoAsignado: String? = null
)
