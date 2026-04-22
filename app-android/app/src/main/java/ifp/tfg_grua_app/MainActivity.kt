package ifp.tfg_grua_app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import ifp.tfg_grua_app.databinding.ActivityMainBinding
import ifp.tfg_grua_app.databinding.ItemViajeBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    // Canal de Supabase Realtime: escucha INSERTs en "servicios" del empleado actual
    private var canalRecogidas: RealtimeChannel? = null
    private var jobRealtime: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Saludo con el nombre del usuario logueado
        val nombre = SesionUsuario.getNombre(this)
        binding.tvTitle.text = if (!nombre.isNullOrBlank()) "Hola, $nombre" else "Hola"

        binding.btnCerrarSesion.setOnClickListener {
            SesionUsuario.cerrarSesion(this)
            ChangeActivity(this, Login::class.java)
        }

        binding.btnExit.setOnClickListener { finish() }

        // Botón actualizar: recarga los servicios desde Supabase
        binding.btnActualizar.setOnClickListener { cargarRecogidas() }

        // Prepara el canal de notificaciones y pide permiso (Android 13+)
        NotificacionesHelper.crearCanal(this)
        pedirPermisoNotificaciones()

        cargarRecogidas()
    }

    // onStart: suscripción Realtime para nuevas recogidas
    override fun onStart() {
        super.onStart()
        suscribirseARealtime()
    }

    // onStop: cortamos la suscripción (no queremos escuchar cuando la Activity no está visible)
    override fun onStop() {
        super.onStop()
        cancelarSuscripcion()
    }

    // Pide permiso POST_NOTIFICATIONS en Android 13+
    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permiso = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            )
            if (permiso != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2002
                )
            }
        }
    }

    // Se conecta al Realtime de Supabase y escucha INSERTs en "servicios" filtrados
    // por num_empleado. Cuando llega un evento lanza una notificación y refresca la lista.
    private fun suscribirseARealtime() {
        val numEmpleado = SesionUsuario.getNumEmpleado(this) ?: return
        if (canalRecogidas != null) return   // ya suscritos

        jobRealtime = lifecycleScope.launch {
            try {
                val canal = SupabaseClient.client.channel("recogidas-emp-$numEmpleado")

                val flow = canal.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "servicios"
                    filter = "num_empleado=eq.$numEmpleado"
                }

                SupabaseClient.client.realtime.connect()
                canal.subscribe()
                canalRecogidas = canal

                flow.collect { insert ->
                    val recogida = insert.decodeRecord<Recogida>()
                    val titulo = "Nueva recogida asignada"
                    val texto  = listOfNotNull(recogida.cliente, recogida.matricula)
                        .joinToString(" · ")
                        .ifBlank { "Tienes una recogida nueva" }

                    NotificacionesHelper.mostrar(
                        this@MainActivity, titulo, texto, recogida.id ?: 0
                    )
                    cargarRecogidas()
                }
            } catch (e: Exception) {
                android.util.Log.e("MAIN", "Error suscribiéndose a Realtime", e)
            }
        }
    }

    private fun cancelarSuscripcion() {
        jobRealtime?.cancel()
        jobRealtime = null
        val canal = canalRecogidas ?: return
        canalRecogidas = null
        lifecycleScope.launch {
            try {
                canal.unsubscribe()
            } catch (e: Exception) {
                android.util.Log.e("MAIN", "Error al desuscribirse", e)
            }
        }
    }

    // Descarga los servicios del empleado logueado y pinta una tarjeta por cada uno
    private fun cargarRecogidas() {
        lifecycleScope.launch {
            try {
                val numEmpleado = SesionUsuario.getNumEmpleado(this@MainActivity)
                if (numEmpleado == null) {
                    Toast.makeText(this@MainActivity,
                        "Sesión sin num_empleado, vuelve a iniciar sesión",
                        Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Solo "Sin empezar" o "En curso" (los "Terminado" no se muestran)
                val recogidas = SupabaseClient.client
                    .from("servicios")
                    .select { filter { eq("num_empleado", numEmpleado) } }
                    .decodeList<Recogida>()
                    .filter { it.estado == "Sin empezar" || it.estado == "En curso" }
                    .sortedByDescending { it.urgente }    // urgentes arriba

                RecogidasRepo.Recogidas = recogidas       // lo usa MapGPS para buscar por id

                binding.listaViajes.removeAllViews()
                for (viaje in recogidas) añadirTarjeta(viaje)

            } catch (e: Exception) {
                android.util.Log.e("MAIN", "Error cargando recogidas", e)
                Toast.makeText(this@MainActivity,
                    "Error cargando recogidas: ${e.message ?: e.javaClass.simpleName}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun añadirTarjeta(viaje: Recogida) {
        val tarjeta = ItemViajeBinding.inflate(layoutInflater, binding.listaViajes, false)

        tarjeta.tvCliente.text   = viaje.cliente ?: ""
        tarjeta.tvMatricula.text = viaje.matricula ?: ""
        tarjeta.tvMotivo.text    = viaje.motivo ?: ""
        tarjeta.tvTelefono.text  = viaje.telefono ?: ""

        tarjeta.tvUrgente.visibility = if (viaje.urgente) View.VISIBLE else View.GONE

        // El texto del botón cambia si el servicio ya está empezado
        val enCurso = viaje.estado == "En curso"
        tarjeta.btnNavegar.text = if (enCurso) "Continuar navegación" else "Navegar"

        tarjeta.btnNavegar.setOnClickListener {
            lifecycleScope.launch {
                if (!enCurso) cambiarEstado(viaje.id, "En curso")
                abrirMapGPS(viaje)
            }
        }

        binding.listaViajes.addView(tarjeta.root)
    }

    // Cambia el estado del servicio en Supabase (ej: "En curso")
    private suspend fun cambiarEstado(id: Int?, estado: String) {
        if (id == null) return
        try {
            SupabaseClient.client.from("servicios").update(
                { set("estado", estado) }
            ) {
                filter { eq("id", id) }
            }
        } catch (e: Exception) {
            android.util.Log.e("MAIN", "Error actualizando estado", e)
            Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Lanza MapGPS en la fase correcta: si el vehículo ya está recogido → ENTREGA
    private fun abrirMapGPS(viaje: Recogida) {
        val fase: String
        val lat: Double
        val lng: Double
        if (viaje.vehiculoRecogido) {
            fase = MapGPS.FASE_ENTREGA
            lat = viaje.destinoLat
            lng = viaje.destinoLng
        } else {
            fase = MapGPS.FASE_RECOGIDA
            lat = viaje.lat
            lng = viaje.lng
        }

        val intent = Intent(this, MapGPS::class.java).apply {
            putExtra(MapGPS.EXTRA_LAT, lat)
            putExtra(MapGPS.EXTRA_LNG, lng)
            putExtra(MapGPS.EXTRA_ID,  viaje.id ?: -1)
            putExtra(MapGPS.EXTRA_FASE, fase)
        }
        startActivity(intent)
        finish()
    }

    private fun <T> ChangeActivity(context: Context, cls: Class<T>){
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}
