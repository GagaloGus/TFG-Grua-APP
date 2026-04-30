package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ifp.tfg_grua_app.databinding.ActivityMenuBinding

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarDatosUsuario()
        eventos()
    }

    private fun cargarDatosUsuario() {
        val nombre = SesionUsuario.getNombre(this)
        val rol = SesionUsuario.getRol(this)

        if (!nombre.isNullOrEmpty()) {
            binding.tvNombre.text = "Hola, $nombre"
            binding.tvInicial.text = obtenerIniciales(nombre)
        } else {
            binding.tvNombre.text = "Hola"
            binding.tvInicial.text = ""
        }

        // Traduce el código de rol (T / A) al texto que ve el usuario
        if(rol?.uppercase() == "T"){
            binding.tvRol.text = "Conductor"
        }
        else{
            binding.tvRol.text = "Admin"
        }
    }

    private fun eventos() {
        binding.cardServicios.setOnClickListener {
            ChangeActivity(this, MainActivity::class.java)
        }

        binding.cardHistorial.setOnClickListener {
            ChangeActivity(this, HistorialActivity::class.java)
        }

        binding.cardPerfil.setOnClickListener {
            ChangeActivity(this, PerfilActivity::class.java)
        }

        binding.btnLlamar.setOnClickListener {
            llamarEmpresa()
        }

        binding.btnWhatsApp.setOnClickListener {
            abrirWhatsapp()
        }

        binding.btnLogOut.setOnClickListener {
            cerrarSesion()
        }
    }

    //Funcion para sacar las iniciales de un nombre
    private fun obtenerIniciales(nombreCompleto: String): String {
        val partes = nombreCompleto.trim().split(" ").filter { it.isNotEmpty() }

        return when {
            partes.isEmpty() -> ""
            partes.size == 1 -> partes[0].substring(0, 1).uppercase()
            else -> (partes[0].substring(0, 1) + partes[1].substring(0, 1)).uppercase()
        }
    }

    private fun llamarEmpresa() {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:600000000"))
        startActivity(intent)
    }

    private fun abrirWhatsapp() {
        val telefono = "34600000000"
        val mensaje = "Hola, necesito ayuda con un servicio."
        val url = "https://wa.me/$telefono?text=" + Uri.encode(mensaje)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun cerrarSesion() {
        SesionUsuario.cerrarSesion(this)
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        ChangeActivity(this, Login::class.java)
    }

    private fun <T> ChangeActivity(context: Context, cls: Class<T>) {
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}
