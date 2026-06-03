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
        // সুপাবেস ক্লায়েন্ট প্লাগইন যা পুরো অ্যাপে এক্সেস করা যাবে
        lateinit var supabase: SupabaseClient
    }

    override fun onCreate() {
        super.onCreate()

        // সুপাবেস প্লাগইন ইনিশিয়ালের মূল কোড
        supabase = createSupabaseClient(
            supabaseUrl = "https://phkjmdmjmoxpdduoyfxp.supabase.co",
            supabaseKey = "sb_publishable_tMNtA0sQs4o3m8iXr2meqg_dIRjCaIN"
        ) {
            install(Postgrest) // ডাটাবেস প্লাগইন
            install(Storage)   // ফাইল স্টোরেজ প্লাগইন
        }
    }
}
