package ifp.tfg_grua_app

// Modelo de un recogida
data class Recogida(
    val id: Int,
    val cliente: String,
    val matricula: String,
    val motivo: String,
    val telefono: String,
    val lat: Double,
    val lng: Double,
    val urgente: Boolean = false
)

// Lista global de viajes de prueba (luego vendrá de una BD o API)
object RecogidasRepo {
    val Recogidas = listOf(
        Recogida(1, "Juan García",   "1234ABC", "Avería motor",   "600111222", 40.4200, -3.7050, urgente = true),
        Recogida(2, "María López",   "5678DEF", "Accidente",      "600333444", 40.4300, -3.6900),
        Recogida(3, "Carlos Ruiz",   "9999XYZ", "Pinchazo",       "600555666", 40.4100, -3.7100),
    )
}