package ifp.tfg_grua_app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de una recogida tal y como llega de la tabla "recogidas" de Supabase.
 * Los @SerialName mapean los nombres de columna reales con los nombres internos
 * que ya usaba el resto de la app (cliente, matricula, motivo, telefono, lat, lng).
 *
 * Si alguna columna en tu Supabase tiene otro nombre, ajústalo en su @SerialName.
 */
@Serializable
data class Recogida(
    val id: Int? = null,
    @SerialName("nombre_cliente")         val cliente: String? = null,
    @SerialName("vehiculo_matricula")     val matricula: String? = null,
    @SerialName("observaciones")          val motivo: String? = null,
    @SerialName("tel_cliente")            val telefono: String? = null,
    @SerialName("ubicacion_recogida_lat") val lat: Double = 0.0,
    @SerialName("ubicacion_recogida_lng") val lng: Double = 0.0,
    @SerialName("ubicacion_destino_lat")  val destinoLat: Double = 0.0,
    @SerialName("ubicacion_destino_lng")  val destinoLng: Double = 0.0,
    @SerialName("num_empleado")           val numEmpleado: Int? = null,
    val urgente: Boolean = false,
    val estado: String? = null,
    @SerialName("vehiculo_recogido")      val vehiculoRecogido: Boolean = false
)

/**
 * Cache global con las recogidas descargadas de Supabase.
 * Se rellena en MainActivity tras hacer login y se consulta luego desde MapGPS.
 */
object RecogidasRepo {
    var Recogidas: List<Recogida> = emptyList()
}
