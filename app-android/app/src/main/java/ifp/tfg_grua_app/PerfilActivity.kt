package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PerfilActivity : AppCompatActivity() {

    private lateinit var tvNumeroEmpleado: TextView
    private lateinit var tvNombre: TextView
    private lateinit var tvRol: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var btnVolver: Button
    private lateinit var imageViewPerfil: ImageView

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageViewPerfil.setImageURI(it)

            getSharedPreferences("perfil", MODE_PRIVATE)
                .edit()
                .putString("imagen_uri", it.toString())
                .apply()
        }
    }

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
        cargarImagenGuardada()
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

    private fun cargarImagenGuardada() {
        val uriString = getSharedPreferences("perfil", MODE_PRIVATE)
            .getString("imagen_uri", null)

        if (uriString != null) {
            imageViewPerfil.setImageURI(Uri.parse(uriString))
        }
    }

    private fun configurarEventos() {
        btnVolver.setOnClickListener {
            ChangeActivity(this, MenuActivity::class.java)
        }

        imageViewPerfil.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun <T> ChangeActivity(context: Context, cls: Class<T>) {
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}