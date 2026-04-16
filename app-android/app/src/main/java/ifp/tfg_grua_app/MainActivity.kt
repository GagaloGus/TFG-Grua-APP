package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import ifp.tfg_grua_app.databinding.ActivityMainBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnCerrarSesion.setOnClickListener {
            // Borra la sesión guardada y vuelve al Login
            SesionUsuario.cerrarSesion(this)
            ChangeActivity(this, Login::class.java)
        }

        binding.btnExit.setOnClickListener {
            finish()
        }

        // Carga las recogidas desde Supabase
        cargarRecogidas()
    }

    /**
     * Descarga todas las filas de la tabla "recogidas" de Supabase,
     * actualiza el cache global y pinta una tarjeta por cada una.
     */
    private fun cargarRecogidas() {
        lifecycleScope.launch {
            try {
                // Cogemos el num_empleado del usuario logueado
                val numEmpleado = SesionUsuario.getNumEmpleado(this@MainActivity)
                if (numEmpleado == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Sesión sin num_empleado, vuelve a iniciar sesión",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Solo las recogidas asignadas a ese empleado
                val recogidas = SupabaseClient.client
                    .from("servicios")
                    .select {
                        filter { eq("num_empleado", numEmpleado) }
                    }
                    .decodeList<Recogida>()

                // Ordenar: urgentes primero
                val ordenadas = recogidas.sortedByDescending { it.urgente }

                // Actualizamos el cache para que MapGPS pueda buscar por id
                RecogidasRepo.Recogidas = ordenadas

                // Limpiar la lista (por si se vuelve a cargar) y crear las tarjetas
                binding.listaViajes.removeAllViews()
                for (viaje in ordenadas) {
                    añadirTarjeta(viaje)
                }
            } catch (e: Exception) {
                android.util.Log.e("MAIN", "Error cargando recogidas", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error cargando recogidas: ${e.message ?: e.javaClass.simpleName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun añadirTarjeta(viaje: Recogida) {
        val tarjeta = layoutInflater.inflate(R.layout.item_viaje, binding.listaViajes, false)

        // Rellenar datos (con fallback a "" para evitar imprimir "null")
        tarjeta.findViewById<TextView>(R.id.tvCliente).text = viaje.cliente ?: ""
        tarjeta.findViewById<TextView>(R.id.tvMatricula).text = viaje.matricula ?: ""
        tarjeta.findViewById<TextView>(R.id.tvMotivo).text = viaje.motivo ?: ""
        tarjeta.findViewById<TextView>(R.id.tvTelefono).text = viaje.telefono ?: ""

        // Mostrar badge urgente si aplica
        val tvUrgente = tarjeta.findViewById<TextView>(R.id.tvUrgente)
        tvUrgente.visibility = if (viaje.urgente) View.VISIBLE else View.GONE

        // Botón navegar: lanza MapGPS con los datos del viaje
        tarjeta.findViewById<Button>(R.id.btnNavegar).setOnClickListener {
            val intent = Intent(this, MapGPS::class.java).apply {
                putExtra(MapGPS.EXTRA_LAT, viaje.lat)
                putExtra(MapGPS.EXTRA_LNG, viaje.lng)
                putExtra(MapGPS.EXTRA_ID,  viaje.id ?: -1)   // forzamos Int no-nullable
            }
            startActivity(intent)
            finish()
        }

        binding.listaViajes.addView(tarjeta)
    }

    private fun <T> ChangeActivity(context: Context, cls: Class<T>){
        context.startActivity(Intent(context, cls))

        if(context is Activity){
            context.finish()
        }
    }
}
