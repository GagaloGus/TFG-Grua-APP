package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import ifp.tfg_grua_app.databinding.ActivityMainBinding
import ifp.tfg_grua_app.databinding.ItemViajeBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Saludo con el nombre del usuario logueado
        val nombre = SesionUsuario.getNombre(this)
        binding.tvTitle.text = if (!nombre.isNullOrBlank()) "Hola, $nombre" else "Hola"

        binding.btnCerrarSesion.setOnClickListener {
            SesionUsuario.cerrarSesion(this)
            ChangeActivity(this, Login::class.java)
        }

        binding.btnExit.setOnClickListener { finish() }

        // Botón actualizar: recarga los servicios desde Supabase
        binding.btnActualizar.setOnClickListener { cargarRecogidas() }

        cargarRecogidas()
    }

    // Descarga los servicios del empleado logueado y pinta una tarjeta por cada uno
    private fun cargarRecogidas() {
        lifecycleScope.launch {
            try {
                val numEmpleado = SesionUsuario.getNumEmpleado(this@MainActivity)
                if (numEmpleado == null) {
                    Toast.makeText(this@MainActivity,
                        "Sesión sin num_empleado, vuelve a iniciar sesión",
                        Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Solo "Sin empezar" o "En curso" (los "Terminado" no se muestran)
                val recogidas = SupabaseClient.client
                    .from("servicios")
                    .select { filter { eq("num_empleado", numEmpleado) } }
                    .decodeList<Recogida>()
                    .filter { it.estado == "Sin empezar" || it.estado == "En curso" }
                    .sortedByDescending { it.urgente }    // urgentes arriba

                RecogidasRepo.Recogidas = recogidas       // lo usa MapGPS para buscar por id

                binding.listaViajes.removeAllViews()
                for (viaje in recogidas) añadirTarjeta(viaje)

            } catch (e: Exception) {
                android.util.Log.e("MAIN", "Error cargando recogidas", e)
                Toast.makeText(this@MainActivity,
                    "Error cargando recogidas: ${e.message ?: e.javaClass.simpleName}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun añadirTarjeta(viaje: Recogida) {
        val tarjeta = ItemViajeBinding.inflate(layoutInflater, binding.listaViajes, false)

        tarjeta.tvCliente.text   = viaje.cliente ?: ""
        tarjeta.tvMatricula.text = viaje.matricula ?: ""
        tarjeta.tvMotivo.text    = viaje.motivo ?: ""
        tarjeta.tvTelefono.text  = viaje.telefono ?: ""

        tarjeta.tvUrgente.visibility = if (viaje.urgente) View.VISIBLE else View.GONE

        // El texto del botón cambia si el servicio ya está empezado
        val enCurso = viaje.estado == "En curso"
        tarjeta.btnNavegar.text = if (enCurso) "Continuar navegación" else "Navegar"

        tarjeta.btnNavegar.setOnClickListener {
            lifecycleScope.launch {
                if (!enCurso) cambiarEstado(viaje.id, "En curso")
                abrirMapGPS(viaje)
            }
        }

        binding.listaViajes.addView(tarjeta.root)
    }

    // Cambia el estado del servicio en Supabase (ej: "En curso")
    private suspend fun cambiarEstado(id: Int?, estado: String) {
        if (id == null) return
        try {
            SupabaseClient.client.from("servicios").update(
                { set("estado", estado) }
            ) {
                filter { eq("id", id) }
            }
        } catch (e: Exception) {
            android.util.Log.e("MAIN", "Error actualizando estado", e)
            Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Lanza MapGPS en la fase correcta: si el vehículo ya está recogido → ENTREGA
    private fun abrirMapGPS(viaje: Recogida) {
        val fase: String
        val lat: Double
        val lng: Double
        if (viaje.vehiculoRecogido) {
            fase = MapGPS.FASE_ENTREGA
            lat = viaje.destinoLat
            lng = viaje.destinoLng
        } else {
            fase = MapGPS.FASE_RECOGIDA
            lat = viaje.lat
            lng = viaje.lng
        }

        val intent = Intent(this, MapGPS::class.java).apply {
            putExtra(MapGPS.EXTRA_LAT, lat)
            putExtra(MapGPS.EXTRA_LNG, lng)
            putExtra(MapGPS.EXTRA_ID,  viaje.id ?: -1)
            putExtra(MapGPS.EXTRA_FASE, fase)
        }
        startActivity(intent)
        finish()
    }

    private fun <T> ChangeActivity(context: Context, cls: Class<T>){
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}
