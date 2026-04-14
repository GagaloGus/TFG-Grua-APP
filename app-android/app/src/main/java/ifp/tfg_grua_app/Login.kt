package ifp.tfg_grua_app

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Login : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var btnEntrar: Button
    private lateinit var cbRecuerdame: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        if (cbRecuerdame.isChecked){
            Toast.makeText(this,"Login correcto. Opción activada", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this, "Login correcto", Toast.LENGTH_SHORT).show()
        }
    }
}