package ifp.tfg_grua_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MenuActivity : AppCompatActivity() {

    private lateinit var txNombre: TextView
    private lateinit var txInicial: TextView
    private lateinit var txRol: TextView

    private lateinit var cardServicios: CardView
    private lateinit var cardHistorial: CardView
    private lateinit var cardPerfil: CardView

    private lateinit var btnLlamar: Button
    private lateinit var btnWhatsapp: Button
    private lateinit var btnCerrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        cargarDatosUsuario()
        eventos()
    }

    private fun initViews() {
        txNombre = findViewById(R.id.txNombre)
        txInicial = findViewById(R.id.txInicial)
        txRol = findViewById(R.id.txRol)

        cardServicios = findViewById(R.id.cardServicios)
        cardHistorial = findViewById(R.id.cardHistorial)
        cardPerfil = findViewById(R.id.cardPerfil)

        btnLlamar = findViewById(R.id.btnLlamar)
        btnWhatsapp = findViewById(R.id.btnWhatsapp)
        btnCerrar = findViewById(R.id.btnCerrar)
    }

    private fun cargarDatosUsuario() {
        val nombre = SesionUsuario.getNombre(this)
        val rol = SesionUsuario.getRol(this)

        if (!nombre.isNullOrEmpty()) {
            txNombre.text = "Hola, $nombre"
            txInicial.text = obtenerIniciales(nombre)
        } else {
            txNombre.text = "Hola"
            txInicial.text = ""
        }

        if (!rol.isNullOrEmpty()) {
            txRol.text = rol
        }
    }

    private fun eventos() {
        cardServicios.setOnClickListener {
            Toast.makeText(this, "Servicios asignados", Toast.LENGTH_SHORT).show()
            // ChangeActivity(this, ServiciosActivity::class.java)
        }

        cardHistorial.setOnClickListener {
            Toast.makeText(this, "Servicios realizados", Toast.LENGTH_SHORT).show()
            // ChangeActivity(this, HistorialActivity::class.java)
        }

        cardPerfil.setOnClickListener {
            Toast.makeText(this, "Mi perfil", Toast.LENGTH_SHORT).show()
            // ChangeActivity(this, PerfilActivity::class.java)
        }

        btnLlamar.setOnClickListener {
            llamarEmpresa()
        }

        btnWhatsapp.setOnClickListener {
            abrirWhatsapp()
        }

        btnCerrar.setOnClickListener {
            cerrarSesion()
        }
    }

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