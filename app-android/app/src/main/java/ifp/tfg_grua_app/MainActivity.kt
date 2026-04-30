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

    private lateinit var binding: ActivityMainBinding

    // Canal Realtime que escucha cambios en "servicios" del empleado
    private var canalRecogidas: RealtimeChannel? = null

    // Refresco automático cada 20s mientras la pantalla está visible
    private var jobRefresco: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        binding.btnVolver.setOnClickListener {
            ChangeActivity(this, MenuActivity::class.java)
        }
        binding.btnActualizar.setOnClickListener {
            cargarRecogidas()
        }

        // Prepara las notificaciones: crea el canal y pide permiso en Android
        NotificacionesHelper.crearCanal(this)
        pedirPermisoNotificaciones()

        // Carga inicial de la lista
        cargarRecogidas()
    }

    // Cuando la pantalla se hace visible empezamos a escuchar Realtime y a refrescar
    override fun onStart() {
        super.onStart()
        suscribirseARealtime()
        iniciarRefrescoAutomatico()
    }

    // Cuando la pantalla se oculta paramos
    override fun onStop() {
        super.onStop()
        desuscribirRealtime()
        jobRefresco?.cancel()
        jobRefresco = null
    }

    // Bucle que recarga la lista cada 20 segundos
    private fun iniciarRefrescoAutomatico() {
        //Si la corutina esta activa cancela
        if (jobRefresco?.isActive == true) {
            return
        }
        jobRefresco = lifecycleScope.launch {
            while (isActive) {
                delay(20_000)
                cargarRecogidas()
            }
        }
    }

    // Pide el permiso POST_NOTIFICATIONS
    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val concedido = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!concedido) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2002
            )
        }
    }

    // Se suscribe al canal Realtime de la tabla "servicios" filtrando por empleado
    private fun suscribirseARealtime() {
        val numEmpleado = SesionUsuario.getNumEmpleado(this) ?: return

        // Si ya estábamos suscritos, no hacemos nada
        if (canalRecogidas != null) {
            return
        }

        lifecycleScope.launch {
            val canal = SupabaseClient.client.channel("recogidas-emp-$numEmpleado")
            canalRecogidas = canal

            val flow = canal.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "servicios"
                filter = "num_empleado=eq.$numEmpleado"
            }
            canal.subscribe()

            // Cada vez que llega un cambio de la BBDD lo procesamos
            flow.collect { action ->
                procesarCambio(action)
            }
        }
    }

    // Decide si hay que mostrar notificación según el tipo de cambio que llega
    private fun procesarCambio(action: PostgresAction) {
        var recogida: Recogida? = null
        var notificar = false

        if (action is PostgresAction.Insert) {
            recogida = action.decodeRecord<Recogida>()
            // Insert: avisamos si llega ya en estado "Sin empezar"
            notificar = recogida.estado == "Sin empezar"
        }
        else if (action is PostgresAction.Update) {
            recogida = action.decodeRecord<Recogida>()
            val estadoAnterior = (action.oldRecord["estado"] as? JsonPrimitive)?.contentOrNull
            // Update: avisamos solo si el estado pasa A "Sin empezar"
            notificar = recogida.estado == "Sin empezar" && estadoAnterior != "Sin empezar"
        }

        // Si la acción no era ni Insert ni Update, salimos
        if (recogida == null) {
            return
        }

        val id = recogida.id ?: return

        if (notificar) {
            val texto = listOfNotNull(recogida.cliente, recogida.matricula)
                .joinToString(" · ")
                .ifBlank { "Tienes una recogida nueva" }
            NotificacionesHelper.mostrar(this, "Nueva recogida asignada", texto, id)
        }

        // Aunque no haya notificación, refrescamos la lista en pantalla
        cargarRecogidas()
    }

    // Cierra el canal de Realtime
    private fun desuscribirRealtime() {
        val canal = canalRecogidas ?: return
        canalRecogidas = null
        lifecycleScope.launch {
            runCatching { canal.unsubscribe() }
        }
    }

    // Descarga los servicios del empleado y pinta una tarjeta por cada uno
    private fun cargarRecogidas() {
        lifecycleScope.launch {
            val numEmpleado = SesionUsuario.getNumEmpleado(this@MainActivity)
            if (numEmpleado == null) {
                toast("Sesión sin num_empleado, vuelve a iniciar sesión")
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
            for (viaje in recogidas) {
                    añadirTarjeta(viaje)
            }
        }
    }

    // Crea la tarjeta visual de un servicio y su botón de Navegar
    private fun añadirTarjeta(viaje: Recogida) {
        val tarjeta = ItemViajeBinding.inflate(layoutInflater, binding.listaViajes, false)

        tarjeta.tvCliente.text = viaje.cliente ?: ""
        tarjeta.tvMatricula.text = viaje.matricula ?: ""
        tarjeta.tvMotivo.text = viaje.motivo ?: ""
        tarjeta.tvTelefono.text  = viaje.telefono ?: ""

        // Si es urgente, mostramos la etiqueta; si no, la ocultamos
        if (viaje.urgente) {
            tarjeta.tvUrgente.visibility = View.VISIBLE
        } else {
            tarjeta.tvUrgente.visibility = View.GONE
        }

        // Texto del botón según el estado del servicio
        val enCurso = viaje.estado == "En curso"
        if (enCurso) {
            tarjeta.btnNavegar.text = "Continuar navegación"
        } else {
            tarjeta.btnNavegar.text = "Navegar"
        }

        // Al pulsar Navegar: si aún no está en curso lo marcamos, y abrimos el mapa
        tarjeta.btnNavegar.setOnClickListener {
            lifecycleScope.launch {
                if (!enCurso) {
                    cambiarEstado(viaje.id, "En curso")
                }
                abrirMapGPS(viaje)
            }
        }

        binding.listaViajes.addView(tarjeta.root)
    }

    // Cambia el estado del servicio en la BBDD
    private suspend fun cambiarEstado(id: Int?, estado: String) {
        if (id == null) {
            return
        }

        SupabaseClient.client.from("servicios").update(
            { set("estado", estado) }
        ) {
            filter { eq("id", id) }
        }
    }

    // Abre la pantalla de navegación con la fase que toca (recogida o entrega)
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

    // Funciones utiles
    private fun toast(texto: String) =
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()

    private fun <T> ChangeActivity(context: Context, cls: Class<T>) {
        context.startActivity(Intent(context, cls))
        if (context is Activity) context.finish()
    }
}
