package ifp.tfg_grua_app

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {
    //Clase puente entre App y Supabase
    val client = createSupabaseClient(
        //URL del proyecto de SupaBase
        supabaseUrl = "https://sgyidsdrwqkqtqimozvm.supabase.co",
        //Key de consultas a SupaBase
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNneWlkc2Ryd3FrcXRxaW1venZtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQwOTUwNDMsImV4cCI6MjA4OTY3MTA0M30.1SbAwco4QTi-BCU6BF6HGnJUjuZ1eC5yDcNlWRWQVzI"
    ) {
        //Modulos instalados del SDK de SupaBase
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}
