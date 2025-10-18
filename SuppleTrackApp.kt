package com.efvs.suppletrack

import android.app.Application
import com.efvs.suppletrack.notifications.createReminderNotificationChannel
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SuppleTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createReminderNotificationChannel(this)
    }
}