package ifp.tfg_grua_app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Una fila de la tabla "servicios" de Supabase.
// Los @SerialName conectan la columna real con el nombre que usamos en Kotlin.
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
    @SerialName("vehiculo_recogido")      val vehiculoRecogido: Boolean = false,
    val urgente: Boolean = false,
    val estado: String? = null
)

// Cache en memoria: lo llena MainActivity al cargar y lo lee MapGPS por id.
object RecogidasRepo {
    var Recogidas: List<Recogida> = emptyList()
}
