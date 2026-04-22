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
import ifp.tfg_grua_app.databinding.ActivityLoginBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var loginJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si marcó "Recuérdame" antes, saltamos directo al Main
        if (SesionUsuario.haySesionPersistente(this)) {
            ChangeActivity(this, MenuActivity::class.java)
            return
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnEntrar.setOnClickListener { validarLogin() }
    }

    private fun validarLogin() {
        val correo = binding.email.text.toString().trim()
        val clave  = binding.Password.text.toString().trim()

        // Validaciones simples
        if (correo.isEmpty()) {
            binding.email.error = "Introduce tu correo"; binding.email.requestFocus(); return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.email.error = "Correo no valido"; binding.email.requestFocus(); return
        }
        if (clave.isEmpty()) {
            binding.Password.error = "Introduce tu contraseña"; binding.Password.requestFocus(); return
        }
        if (clave.length < 6) {
            binding.Password.error = "La contraseña debe tener almenos 6 caracteres"
            binding.Password.requestFocus(); return
        }

        // Busca en la tabla "usuarios" un registro con ese email + password
        loginJob = lifecycleScope.launch {
            try {
                val resultado = SupabaseClient.client
                    .from("usuarios")
                    .select {
                        filter {
                            eq("email", correo)
                            eq("password", clave)
                        }
                    }
                    .decodeList<Usuario>()

                if (resultado.isEmpty()) {
                    Toast.makeText(this@Login, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                } else {
                    val usuario = resultado.first()
                    SesionUsuario.guardar(this@Login, usuario, binding.txRecuerdame.isChecked)
                    Toast.makeText(this@Login, "Bienvenido, ${usuario.nombre ?: usuario.mail}", Toast.LENGTH_SHORT).show()
                    ChangeActivity(this@Login, MenuActivity::class.java)
                }
            } catch (e: Exception) {
                android.util.Log.e("LOGIN", "Fallo consultando Supabase", e)
                Toast.makeText(this@Login, "Error: ${e.message ?: e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loginJob?.cancel()
    }

    private fun <T> ChangeActivity(context: Context, cls: Class<T>){
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}
