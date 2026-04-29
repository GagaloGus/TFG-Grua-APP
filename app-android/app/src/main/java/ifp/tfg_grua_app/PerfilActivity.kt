package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class PerfilActivity : AppCompatActivity() {

    private lateinit var tvNumeroEmpleado: TextView
    private lateinit var tvNombre: TextView
    private lateinit var tvRol: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var btnVolver: Button
    private lateinit var imageViewPerfil: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        cargarDatosUsuario()
        cargarImagenDesdeSupabase()
        configurarEventos()
    }

    private fun initViews() {
        tvNumeroEmpleado = findViewById(R.id.tvNumeroEmpleado)
        tvNombre = findViewById(R.id.tvNombre)
        tvRol = findViewById(R.id.tvRol)
        tvCorreo = findViewById(R.id.tvCorreo)
        btnVolver = findViewById(R.id.btnVolver)
        imageViewPerfil = findViewById(R.id.imageView2)
    }

    private fun cargarDatosUsuario() {
        val numeroEmpleado = SesionUsuario.getNumEmpleado(this)
        val nombre = SesionUsuario.getNombre(this)
        val rol = SesionUsuario.getRol(this)
        val correo = SesionUsuario.getMail(this)

        tvNumeroEmpleado.text = numeroEmpleado?.toString() ?: "-"
        tvNombre.text = nombre ?: "-"
        tvRol.text = rol ?: "-"
        tvCorreo.text = correo ?: "-"
    }

    private fun cargarImagenDesdeSupabase() {
        val numeroEmpleado = SesionUsuario.getNumEmpleado(this) ?: return

        lifecycleScope.launch {
            try {
                val usuario = SupabaseClient.client
                    .from("usuarios")
                    .select {
                        filter {
                            eq("num_empleado", numeroEmpleado)
                        }
                    }
                    .decodeSingle<Usuario>()

                // Glide modificado: sin placeholder ni error
                Glide.with(this@PerfilActivity)
                    .load(usuario.avatarUrl)
                    .circleCrop()
                    .into(imageViewPerfil)

            } catch (e: Exception) {
                Toast.makeText(
                    this@PerfilActivity,
                    "Error cargando imagen",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun configurarEventos() {
        btnVolver.setOnClickListener {
            ChangeActivity(this, MenuActivity::class.java)
        }

        imageViewPerfil.setOnClickListener {
            Toast.makeText(this, "La foto viene desde Supabase", Toast.LENGTH_SHORT).show()
        }
    }

    private fun <T> ChangeActivity(context: Context, cls: Class<T>) {
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}