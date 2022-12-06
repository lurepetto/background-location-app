package com.plcoding.backgroundlocationtracking

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

// Este es el canal de notificaciones al que le enviamos las mismas
class LocationApp: Application() {

    override fun onCreate() {
        super.onCreate()
        // validaciones de versionamiento
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                // le asignamos un id, un name y el nivel de importancia a la notificacion
                "location",
                "Location",
                NotificationManager.IMPORTANCE_HIGH
            )
            // hacemos una referencia a nuestro notification manager
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // le pasamos los valores al canal
            notificationManager.createNotificationChannel(channel)
        }
    }
}