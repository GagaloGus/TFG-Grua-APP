package ifp.tfg_grua_app

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://sgyidsdrwqkqtqimozvm.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNneWlkc2Ryd3FrcXRxaW1venZtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQwOTUwNDMsImV4cCI6MjA4OTY3MTA0M30.1SbAwco4QTi-BCU6BF6HGnJUjuZ1eC5yDcNlWRWQVzI"
    ) {
        install(Auth)
        install(Postgrest)
    }
}
