package com.plcoding.backgroundlocationtracking

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationService: Service() {

    // este service scope esta vinculado al ciclo de vida de nuestro servicio
    // supervisor job es para que cada scope funcione de manera independiente
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Client interface
    private lateinit var locationClient: LocationClient

    // Es necesario bindear la clase
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    // creamos el servicio
    override fun onCreate() {
        super.onCreate()
        // le pasamos el contexto a nuestra interface
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    // funcion para revisar que acción es llamada y lanzar el metodo correspondiente
    // cada intent (evento de usuario) disparara esta funcion
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    // hay que generar una notificación que muestre la info
    private fun start() {
        // generamos la notificacion de la app con la info
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // lanzamos el callback flow
        locationClient
            // Cada 10 segundos dame una actualización de la ubi
            .getLocationUpdates(10000L)
                // Capturamos los errores
            .catch { e -> e.printStackTrace() }
                // Asignamos la latitud y longitud de la ubi
            .onEach { location ->
                val lat = location.latitude.toString()
                val long = location.longitude.toString()
                val updatedNotification = notification.setContentText(
                    "Location: ($lat, $long)"
                )
                // actualizamos la notificacion existente
                notificationManager.notify(1, updatedNotification.build())
            }
            .launchIn(serviceScope)

        // le pedimos al sistema que corra este servicio en primer plano
        startForeground(1, notification.build())
    }

    private fun stop() {
        // removemos la notificacion cuando pare el servicio
        stopForeground(true)
        // paramos el servicio
        stopSelf()
    }

    // funcion para cancelar el scope de corrutina
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // variables para enviar y detener el envío
    // de información al service
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}