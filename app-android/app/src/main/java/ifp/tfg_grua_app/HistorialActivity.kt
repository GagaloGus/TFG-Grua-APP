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
import ifp.tfg_grua_app.databinding.ActivityHistorialBinding
import ifp.tfg_grua_app.databinding.ItemViajeBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class HistorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        binding.btnVolverBottom.setOnClickListener {
            ChangeActivity(this, MenuActivity::class.java)
        }

        binding.btnActualizarHistorial.setOnClickListener {
            cargarServiciosRealizados()
        }

        cargarServiciosRealizados()
    }

    // Descarga los servicios "Terminado" del empleado y los pinta
    private fun cargarServiciosRealizados() {
        lifecycleScope.launch {
            try {
                val numEmpleado = SesionUsuario.getNumEmpleado(this@HistorialActivity)
                if (numEmpleado == null) {
                    toast("Sesión sin número de empleado")
                    return@launch
                }

                val servicios = SupabaseClient.client
                    .from("servicios")
                    .select { filter { eq("num_empleado", numEmpleado) } }
                    .decodeList<Recogida>()
                    .filter { it.estado == "Terminado" }

                binding.listaHistorial.removeAllViews()

                if (servicios.isEmpty()) {
                    binding.tvVacio.visibility = View.VISIBLE
                } else {
                    binding.tvVacio.visibility = View.GONE
                    for (viaje in servicios) {
                        anadirTarjeta(viaje)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HISTORIAL", "Error cargando historial", e)
                toast("Error: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    // Crea una tarjeta visual con los datos del servicio (sin botón Navegar)
    private fun anadirTarjeta(viaje: Recogida) {
        val tarjeta = ItemViajeBinding.inflate(layoutInflater, binding.listaHistorial, false)

        tarjeta.tvCliente.text = viaje.cliente ?: ""
        tarjeta.tvMatricula.text = viaje.matricula ?: ""
        tarjeta.tvMotivo.text = viaje.motivo ?: ""
        tarjeta.tvTelefono.text = viaje.telefono ?: ""

        // En el historial no hace falta marcar urgente ni tener botón
        tarjeta.tvUrgente.visibility = View.GONE
        tarjeta.btnNavegar.visibility = View.GONE

        binding.listaHistorial.addView(tarjeta.root)
    }

    // Funciones utiles
    private fun toast(texto: String) =
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()

    private fun <T> ChangeActivity(context: Context, cls: Class<T>) {
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}
