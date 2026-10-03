package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository

class LinguaGlassApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        preferencesRepository = UserPreferencesRepository(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FLOATING_CHANNEL_ID,
                "Lingua Glass Floating Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active floating translator bubble notification"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val FLOATING_CHANNEL_ID = "lingua_glass_floating_channel"
        lateinit var instance: LinguaGlassApp
            private set
    }
}
