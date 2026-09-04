package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class FocusCraftApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w("FocusCraftApp", "Firebase auto-initialization skipped: ${e.message}")
        }

        try {
            com.example.sync.FocusSyncWorker.schedulePeriodicSync(this)
        } catch (e: Exception) {
            Log.w("FocusCraftApp", "Sync worker scheduling warning: ${e.message}")
        }
    }
}
