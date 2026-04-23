package ifp.tfg_grua_app

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import android.content.Context
import android.content.Intent

class PerfilActivity : AppCompatActivity() {

    private lateinit var tvNumeroEmpleado: TextView
    private lateinit var tvNombre: TextView
    private lateinit var tvRol: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var btnVolver: TextView

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
        cargaDatosUsuario()
        configurarEventos()
    }
    private fun initViews(){
        tvNumeroEmpleado = findViewById(R.id.tvNumeroEmpleado)
        tvNombre = findViewById(R.id.tvNombre)
        tvRol = findViewById(R.id.tvRol)
        tvCorreo = findViewById(R.id.tvCorreo)
        btnVolver = findViewById(R.id.btnVolver)
    }

    private fun cargaDatosUsuario(){
        val numeroEmpleado = SesionUsuario.getNumEmpleado(this)
        val nombre = SesionUsuario.getNombre(this)
        val rol = SesionUsuario.getRol(this)
        val correo = SesionUsuario.getMail(this)

        tvNumeroEmpleado.text = numeroEmpleado?.toString() ?: "-"
        tvNombre .text = nombre ?: "-"
        tvRol.text = rol ?: "-"
        tvCorreo.text = correo ?: "-"

    }

    private fun configurarEventos(){
        btnVolver.setOnClickListener {
            ChangeActivity(this, MenuActivity::class.java)
        }
    }
    private fun <T> ChangeActivity(context: Context, cls: Class<T>) {
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }

}