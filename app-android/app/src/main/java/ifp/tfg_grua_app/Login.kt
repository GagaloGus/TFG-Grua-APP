package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import ifp.tfg_grua_app.databinding.ActivityLoginBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si marcó "Recuérdame" antes, salta directo al menú
        if (SesionUsuario.haySesionPersistente(this)) {
            ChangeActivity(this,MenuActivity::class.java)
            return
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        binding.btnLogin.setOnClickListener { validarLogin() }
    }

    private fun validarLogin() {
        val correo = binding.etvMail.text.toString().trim()
        val clave  = binding.etvPassword.text.toString().trim()

        //Si los campos no son validos en el EditText no comprueba en Supa
        if (!camposValidos(correo, clave)){
            return
        }
        else{
            lifecycleScope.launch {
                val usuario = SupabaseClient.client.from("usuarios")
                    .select { filter { eq("email", correo); eq("password", clave) } }
                    .decodeList<Usuario>().firstOrNull()

                if(usuario == null ){
                    toast("Usuario o contraseña incorrectos")
                }
                else if (usuario.rol?.uppercase() !in listOf("T", "A")){
                    toast("Tu cuenta no tiene permisos para acceder a la aplicación")
                }
                else{
                    SesionUsuario.guardar(this@Login, usuario, binding.btnRemind.isChecked)
                    toast("Bienvenido, ${usuario.nombre ?: usuario.mail}")
                    ChangeActivity(this@Login, MenuActivity::class.java)
                }
            }
        }
    }

    //Comprueba si son validos los valores en los EditTexts
    private fun camposValidos(correo: String, clave: String): Boolean = when {
        correo.isEmpty() ->
            error(binding.etvMail, "Introduce tu correo")
        !Patterns.EMAIL_ADDRESS.matcher(correo).matches() ->
            error(binding.etvMail, "Correo no válido")
        clave.isEmpty() ->
            error(binding.etvPassword, "Introduce tu contraseña")
        clave.length < 6 ->
            error(binding.etvPassword, "La contraseña debe tener al menos 6 caracteres")
        else -> true
    }

    //Funcion para generar error en EditText
    private fun error(campo: EditText, mensaje: String): Boolean {
        campo.error = mensaje
        campo.requestFocus()
        return false
    }

    //Funciones utiles
    private fun toast(texto: String) =
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()

    private fun <T> ChangeActivity(context: Context, cls: Class<T>) {
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}
