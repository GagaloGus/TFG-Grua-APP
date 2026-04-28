# Contexto del proyecto — TFG-Grua-APP

> Pega este texto entero al inicio de un nuevo chat en la app móvil de Claude para que pueda ayudarte con conocimiento del proyecto.

## Quién soy y qué hago
Soy Rubén, estudiante de DAM (Desarrollo de Aplicaciones Multiplataforma). Estoy haciendo mi TFG: una aplicación Android para los conductores de una empresa de grúas. La app permite recibir servicios asignados desde la oficina, navegar al cliente con Google Maps, recoger el vehículo y llevarlo al destino. La administración usa un panel web en Angular (que también desarrollo yo) y ambos comparten la misma base de datos.

## Stack técnico
- **Lenguaje**: Kotlin
- **IDE**: Android Studio
- **Build**: Gradle Kotlin DSL (build.gradle.kts)
- **minSdk / targetSdk**: 27 / 36
- **UI**: Vistas XML clásicas con View Binding (no Compose)
- **Backend**: Supabase (PostgreSQL + Auth + Realtime)
- **Mapas**: Google Maps SDK + Directions API
- **HTTP**: OkHttp 4.12 + Gson
- **Concurrencia**: Kotlin Coroutines + lifecycleScope
- **WebSockets**: Ktor con motor OkHttp (necesario para Supabase Realtime)
- **Serialización**: kotlinx-serialization

## Estructura de paquete
Todo en `ifp.tfg_grua_app`. Activities clave:
- `Login.kt` — login con email/contraseña vía Supabase GoTrue.
- `MenuActivity.kt` — menú principal después del login.
- `MainActivity.kt` — lista de servicios "Sin empezar" / "En curso" del conductor, suscripción Realtime y refresco automático cada 20 s.
- `MapGPS.kt` — pantalla de navegación con mapa, ruta calculada por Directions API y voz (TTS).
- `HistorialActivity.kt` — servicios "Terminado".
- `PerfilActivity.kt` — datos del usuario.

Helpers/modelos:
- `SupabaseClient.kt` — Singleton con módulos Postgrest, GoTrue, Realtime.
- `SesionUsuario.kt` — guarda en SharedPreferences nombre, num_empleado y rol.
- `Recogida.kt` — data class @Serializable que mapea la tabla `servicios`.
- `NotificacionesHelper.kt` — canal de notificaciones e icono monocromo.

## Base de datos en Supabase
Dos tablas:
- `empleados` (id, num_empleado, nombre, correo, rol).
- `servicios` (id, num_empleado, cliente, matricula, telefono, motivo, estado, urgente, lat, lng, destino_lat, destino_lng, vehiculo_recogido).

Habilitado en Realtime con:
```
ALTER PUBLICATION supabase_realtime ADD TABLE public.servicios;
ALTER TABLE public.servicios REPLICA IDENTITY FULL;
```

## Cómo funciona la navegación (resumen)
Maps SDK pinta el mapa, FusedLocationProvider da la posición cada 2 s, Directions API devuelve la ruta (polilínea + steps), OkHttp+Gson la traen y parsean, y TextToSpeech lee las indicaciones a 500 m, 150 m y en el cruce.

## Cómo funciona Supabase (resumen)
GoTrue autentica al usuario y devuelve un JWT. Postgrest hace consultas tipadas a la base de datos (`from("servicios").select{...}.decodeList<Recogida>()`). Realtime mantiene un WebSocket abierto que avisa al instante cuando el admin asigna un servicio nuevo o cambia el estado a "Sin empezar"; entonces se lanza una notificación local y se recarga la lista. Como red de seguridad, hay un timer cada 20 s que refresca la lista por si el WebSocket se cae.

## Decisiones técnicas que conviene recordar
- La clave de Maps **no está hardcodeada**: se inyecta desde `local.properties` mediante `secrets-gradle-plugin`. En el manifest aparece como `${MAPS_API_KEY}`.
- El motor HTTP de Supabase es **ktor-client-okhttp** (no ktor-client-android, porque ese no soporta WebSockets).
- La suscripción Realtime se hace en `onStart` y se cancela en `onStop` para no consumir batería.
- Solo se notifica cuando es **INSERT con estado "Sin empezar"** o **UPDATE en el que estado pasa a "Sin empezar"**, no en cualquier cambio de columna.
- El icono de notificación pequeño es `ic_launcher_monochrome` (silueta blanca obligatoria); el grande es el launcher redondo a color.
- `allowBackup="false"` en el manifest para no respaldar SharedPreferences con la sesión.

## Estado actual
- Login funcional con Supabase Auth.
- Pantalla principal lista servicios filtrados por num_empleado, ordenados por urgente.
- Navegación con voz funcionando.
- Notificaciones Realtime funcionando (mientras app abierta o segundo plano reciente).
- Auto-refresco 20 s.
- Historial con scroll arreglado recientemente.
- Memoria del TFG y guion de defensa generados como .docx.

## Pendiente / mejoras futuras
- Notificaciones push en segundo plano con FCM + Edge Function en Supabase (descartado por tiempo).
- Captura de fotos del vehículo en recogida/entrega (Supabase Storage).
- Modo offline con sincronización al recuperar conexión.
- Tests unitarios (JUnit) y de instrumentación (Espresso).
- Mejoras de accesibilidad (TalkBack, contraste).

## Cosas que NO puedes hacer desde la app móvil
- No tienes acceso a mi carpeta de proyecto en `F:\DAM\Android\PDMP\TFG-Grua-APP\app-android`.
- No puedes editar archivos ni ejecutar comandos.
- Para que me ayudes con código tendré que pegarte el archivo concreto en el chat.

## Cómo me gusta que respondas
- En español.
- Tono natural, sin formalismos.
- Sin bullets ni headers innecesarios para preguntas de conversación.
- Cuando me des código, explícamelo brevemente antes o después.
- Si una decisión técnica tiene alternativas, dime cuáles y por qué la elegida.
