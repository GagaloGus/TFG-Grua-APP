package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import ifp.tfg_grua_app.databinding.ItemViajeBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class HistorialActivity : AppCompatActivity() {

    private lateinit var tvVacio: TextView
    private lateinit var listaHistorial: LinearLayout
    private lateinit var btnActualizar: Button
    private lateinit var btnVolverBottom: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historial)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvVacio = findViewById(R.id.tvVacio)
        listaHistorial = findViewById(R.id.listaHistorial)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnVolverBottom = findViewById(R.id.btnVolverBottom)

        btnVolverBottom.setOnClickListener {
            changeActivity(this, MenuActivity::class.java)
        }

        btnActualizar.setOnClickListener {
            cargarServiciosRealizados()
        }

        cargarServiciosRealizados()
    }

    private fun cargarServiciosRealizados() {
        lifecycleScope.launch {
            try {
                val numEmpleado = SesionUsuario.getNumEmpleado(this@HistorialActivity)

                if (numEmpleado == null) {
                    Toast.makeText(
                        this@HistorialActivity,
                        "Sesión sin número de empleado",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val servicios = SupabaseClient.client
                    .from("servicios")
                    .select {
                        filter {
                            eq("num_empleado", numEmpleado)
                        }
                    }
                    .decodeList<Recogida>()
                    .filter { it.estado == "Terminado" }

                listaHistorial.removeAllViews()

                if (servicios.isEmpty()) {
                    tvVacio.visibility = View.VISIBLE
                } else {
                    tvVacio.visibility = View.GONE
                    for (viaje in servicios) {
                        anadirTarjeta(viaje)
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("HISTORIAL", "Error cargando historial", e)
                Toast.makeText(
                    this@HistorialActivity,
                    "Error: ${e.message ?: e.javaClass.simpleName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun anadirTarjeta(viaje: Recogida) {
        val tarjeta = ItemViajeBinding.inflate(layoutInflater, listaHistorial, false)

        tarjeta.tvCliente.text = viaje.cliente ?: ""
        tarjeta.tvMatricula.text = viaje.matricula ?: ""
        tarjeta.tvMotivo.text = viaje.motivo ?: ""
        tarjeta.tvTelefono.text = viaje.telefono ?: ""

        tarjeta.tvUrgente.visibility = View.GONE
        tarjeta.btnNavegar.visibility = View.GONE

        listaHistorial.addView(tarjeta.root)
    }

    private fun <T> changeActivity(context: Context, cls: Class<T>) {
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}