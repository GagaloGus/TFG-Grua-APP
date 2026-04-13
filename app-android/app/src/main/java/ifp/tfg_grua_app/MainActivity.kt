package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ifp.tfg_grua_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Ordenar: urgentes primero
        val viajes = RecogidasRepo.Recogidas.sortedByDescending { it.urgente }

        // Crear una tarjeta por cada viaje
        for (viaje in viajes) {
            añadirTarjeta(viaje)
        }

        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnLogin.setOnClickListener {
            ChangeActivity(this, Login::class.java)
        }

        binding.btnExit.setOnClickListener {
            finish()
        }
    }

    private fun añadirTarjeta(viaje: Recogida) {
        val tarjeta = layoutInflater.inflate(R.layout.item_viaje, binding.listaViajes, false)

        // Rellenar datos
        tarjeta.findViewById<TextView>(R.id.tvCliente).text = "${viaje.cliente}"
        tarjeta.findViewById<TextView>(R.id.tvMatricula).text = "${viaje.matricula}"
        tarjeta.findViewById<TextView>(R.id.tvMotivo).text = "${viaje.motivo}"
        tarjeta.findViewById<TextView>(R.id.tvTelefono).text = "${viaje.telefono}"

        // Mostrar badge urgente si aplica
        val tvUrgente = tarjeta.findViewById<TextView>(R.id.tvUrgente)
        tvUrgente.visibility = if (viaje.urgente) View.VISIBLE else View.GONE

        // Botón navegar: lanza MapGPS con los datos del viaje
        tarjeta.findViewById<Button>(R.id.btnNavegar).setOnClickListener {
            val intent = Intent(this, MapGPS::class.java).apply {
                putExtra(MapGPS.EXTRA_LAT, viaje.lat)
                putExtra(MapGPS.EXTRA_LNG, viaje.lng)
                putExtra(MapGPS.EXTRA_ID,  viaje.id)
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


