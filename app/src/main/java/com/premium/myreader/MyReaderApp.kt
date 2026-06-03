package com.premium.myreader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.jan_tennert.supabase.SupabaseClient
import io.github.jan-tennert.supabase.createSupabaseClient
import io.github.jan-tennert.supabase.postgrest.Postgrest
import io.github.jan-tennert.supabase.storage.Storage

@HiltAndroidApp
class MyReaderApp : Application() {

    companion object {
        lateinit var supabase: SupabaseClient
    }

    override fun onCreate() {
        super.onCreate()

        supabase = createSupabaseClient(
            supabaseUrl = "https://phkjmdmjmoxpdduoyfxp.supabase.co",
            supabaseKey = "sb_publishable_tMNtA0sQs4o3m8iXr2meqg_dIRjCaIN"
        ) {
            install(Postgrest)
            install(Storage)
        }
    }
}
