package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ifp.tfg_grua_app.databinding.ActivityPerfilBinding

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding

    // Selector de imagen del galería: cuando el usuario elige una foto,
    // la mostramos en el ImageView y guardamos su URI para la próxima vez
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            binding.ivPerfil.setImageURI(uri)

            getSharedPreferences("perfil", MODE_PRIVATE)
                .edit()
                .putString("imagen_uri", uri.toString())
                .apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        cargarDatosUsuario()
        cargarImagenGuardada()
        configurarEventos()
    }

    // Rellena los TextView del perfil con los datos del usuario logueado
    private fun cargarDatosUsuario() {
        val numeroEmpleado = SesionUsuario.getNumEmpleado(this)
        val nombre = SesionUsuario.getNombre(this)
        val rol = SesionUsuario.getRol(this)
        val correo = SesionUsuario.getMail(this)

        binding.tvNumeroEmpleado.text = numeroEmpleado?.toString() ?: "-"
        binding.tvNombrePerfil.text = nombre ?: "-"
        binding.tvCorreoPerfil.text = correo ?: "-"

        // Traduce el código de rol (T / A) al texto que ve el usuario
        if (rol?.uppercase() == "T") {
            binding.tvRolPerfil.text = "Conductor"
        } else if (rol?.uppercase() == "A") {
            binding.tvRolPerfil.text = "Admin"
        } else {
            binding.tvRolPerfil.text = "-"
        }
    }

    // Si el usuario eligió una foto en una sesión anterior, la cargamos
    private fun cargarImagenGuardada() {
        val uriString = getSharedPreferences("perfil", MODE_PRIVATE)
            .getString("imagen_uri", null)

        if (uriString != null) {
            binding.ivPerfil.setImageURI(Uri.parse(uriString))
        }
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener {
            ChangeActivity(this, MenuActivity::class.java)
        }

        // Tocar la foto abre la galería para cambiarla
        binding.ivPerfil.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    // Funciones utiles
    private fun <T> ChangeActivity(context: Context, cls: Class<T>) {
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}
