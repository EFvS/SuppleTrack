package com.efvs.suppletrack.notifications

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

fun createReminderNotificationChannel(context: Context) {
    val channel = NotificationChannelCompat.Builder(
        "suppletrack_reminders",
        NotificationManagerCompat.IMPORTANCE_HIGH
    )
        .setName("Supplement Reminders")
        .setDescription("Notifications for supplement and medication reminders")
        .build()
    NotificationManagerCompat.from(context).createNotificationChannel(channel)
}