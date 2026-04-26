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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    // Canal Realtime que escucha cambios en "servicios" del empleado actual
    private var canalRecogidas: RealtimeChannel? = null

    // Refresco automático cada 20s mientras la Activity está visible
    private var jobRefresco: Job? = null

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

        binding.btnActualizar.setOnClickListener { cargarRecogidas() }

        NotificacionesHelper.crearCanal(this)
        pedirPermisoNotificaciones()

        cargarRecogidas()
    }

    // Solo escuchamos Realtime y refrescamos mientras la Activity está visible
    override fun onStart() {
        super.onStart()
        suscribirseARealtime()
        iniciarRefrescoAutomatico()
    }

    override fun onStop() {
        super.onStop()
        desuscribirRealtime()
        jobRefresco?.cancel()
        jobRefresco = null
    }

    // Cada 20s recarga la lista desde Supabase (además del botón manual)
    private fun iniciarRefrescoAutomatico() {
        if (jobRefresco?.isActive == true) return
        jobRefresco = lifecycleScope.launch {
            while (isActive) {
                delay(20_000)
                cargarRecogidas()
            }
        }
    }

    // Pide permiso POST_NOTIFICATIONS en Android 13+
    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2002
            )
        }
    }

    // Escucha INSERTs y UPDATEs en "servicios" del empleado actual.
    // Solo notifica cuando:
    //   - INSERT con estado = "Sin empezar"
    //   - UPDATE donde la columna "estado" cambia y el nuevo valor es "Sin empezar"
    private fun suscribirseARealtime() {
        val numEmpleado = SesionUsuario.getNumEmpleado(this) ?: return
        if (canalRecogidas != null) return   // ya suscritos

        lifecycleScope.launch {
            try {
                val canal = SupabaseClient.client.channel("recogidas-emp-$numEmpleado")
                canalRecogidas = canal

                val flow = canal.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "servicios"
                    filter = "num_empleado=eq.$numEmpleado"
                }
                canal.subscribe()

                flow.collect { action ->
                    val (notificar, r) = when (action) {
                        is PostgresAction.Insert -> {
                            val nuevo = action.decodeRecord<Recogida>()
                            // Insert: avisamos si viene como "Sin empezar"
                            Pair(nuevo.estado == "Sin empezar", nuevo)
                        }
                        is PostgresAction.Update -> {
                            val nuevo = action.decodeRecord<Recogida>()
                            val estadoAnterior =
                                (action.oldRecord["estado"] as? JsonPrimitive)?.contentOrNull
                            // Update: avisamos solo si la columna estado cambió a "Sin empezar"
                            val pasaASinEmpezar = nuevo.estado == "Sin empezar"
                                    && estadoAnterior != "Sin empezar"
                            Pair(pasaASinEmpezar, nuevo)
                        }
                        else -> return@collect
                    }

                    val id = r.id ?: return@collect
                    if (notificar) {
                        val texto = listOfNotNull(r.cliente, r.matricula)
                            .joinToString(" · ")
                            .ifBlank { "Tienes una recogida nueva" }
                        NotificacionesHelper.mostrar(
                            this@MainActivity, "Nueva recogida asignada", texto, id
                        )
                    }
                    cargarRecogidas()   // refresca la lista en pantalla aunque no haya noti
                }
            } catch (e: Exception) {
                android.util.Log.e("MAIN", "Error Realtime", e)
            }
        }
    }

    private fun desuscribirRealtime() {
        val canal = canalRecogidas ?: return
        canalRecogidas = null
        lifecycleScope.launch { runCatching { canal.unsubscribe() } }
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

                val recogidas = SupabaseClient.client
                    .from("servicios")
                    .select { filter { eq("num_empleado", numEmpleado) } }
                    .decodeList<Recogida>()
                    .filter { it.estado == "Sin empezar" || it.estado == "En curso" }
                    .sortedByDescending { it.urgente }

                RecogidasRepo.Recogidas = recogidas

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
