package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {

    private var loginJob: Job? = null
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var btnEntrar: Button
    private lateinit var cbRecuerdame: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si el usuario marcó "Recuérdame" en un login anterior,
        // saltamos directamente a MainActivity sin mostrar la pantalla de login.
        if (SesionUsuario.haySesionPersistente(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        email = findViewById(R.id.email)
        password = findViewById(R.id.Password)
        btnEntrar = findViewById(R.id.btnEntrar)
        cbRecuerdame = findViewById(R.id.txRecuerdame)

        btnEntrar.setOnClickListener {
            validarLogin()
        }
    }

    private fun validarLogin(){
        val correo = email.text.toString().trim()
        val clave = password.text.toString().trim()

        if (correo.isEmpty()) {
            email.error = "Introduce tu correo"
            email.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            email.error = "Correo no valido"
            email.requestFocus()
            return
        }

        if (clave.isEmpty()){
            password.error = "Introduce tu contraseña"
            password.requestFocus()
            return
        }

        if (clave.length < 6 ){
            password.error = "La contraseña debe tener almenos 6 caracteres"
            password.requestFocus()
            return
        }

        loginJob = lifecycleScope.launch {
            try {
                // Consulta a la tabla "usuarios": busca uno cuyo mail y password coincidan
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
                    // Ningún usuario coincide con ese email + contraseña
                    Toast.makeText(
                        this@Login,
                        "Usuario o contraseña incorrectos",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Login OK: guardamos la sesión y entramos
                    val usuario = resultado.first()
                    SesionUsuario.guardar(this@Login, usuario, cbRecuerdame.isChecked)
                    Toast.makeText(
                        this@Login,
                        "Bienvenido, ${usuario.nombre ?: usuario.mail}",
                        Toast.LENGTH_SHORT
                    ).show()
                    ChangeActivity(this@Login, MainActivity::class.java)
                }

            } catch (e: Exception) {
                // DEBUG: log completo + mensaje real en pantalla
                android.util.Log.e("LOGIN", "Fallo consultando Supabase", e)
                Toast.makeText(
                    this@Login,
                    "Error: ${e.message ?: e.javaClass.simpleName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        loginJob?.cancel()  // Cancela si se cierra la Activity
    }
    private fun <T> ChangeActivity(context: Context, cls: Class<T>){
        context.startActivity(Intent(context, cls))

        if(context is Activity){
            context.finish()
        }
    }
}