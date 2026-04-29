package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import ifp.tfg_grua_app.databinding.ActivityPerfilBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding

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
        cargarImagenDesdeSupabase()
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
    private fun cargarImagenDesdeSupabase() {
        val numeroEmpleado = SesionUsuario.getNumEmpleado(this) ?: return

        lifecycleScope.launch {
            val usuario = SupabaseClient.client
                .from("usuarios")
                .select {
                    filter {
                        eq("num_empleado", numeroEmpleado)
                    }
                }
                .decodeSingle<Usuario>()

            Glide.with(this@PerfilActivity)
                .load(usuario.avatarUrl)
                .placeholder(R.drawable.avatar_circulo)
                .error(R.drawable.avatar_circulo)
                .circleCrop()
                .into(binding.ivPerfil)
        }
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener {
            ChangeActivity(this, MenuActivity::class.java)
        }

        binding.ivPerfil.setOnClickListener {
            Toast.makeText(this, "La foto viene desde Supabase", Toast.LENGTH_SHORT).show()
        }
    }

    // Funciones utiles
    private fun <T> ChangeActivity(context: Context, cls: Class<T>) {
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}
