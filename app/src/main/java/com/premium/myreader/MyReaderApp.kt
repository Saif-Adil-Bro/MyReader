package com.premium.myreader

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // ফায়ারবেস চালু করার মূল কোড
        FirebaseApp.initializeApp(this)
    }
}
