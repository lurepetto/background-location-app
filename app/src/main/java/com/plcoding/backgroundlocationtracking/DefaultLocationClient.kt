package com.plcoding.backgroundlocationtracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class DefaultLocationClient(
    private val context: Context,
    // Este tipo de dato es el que se implementa especifcamente
    // para obtener la ubicacion en android
    private val client: FusedLocationProviderClient
): LocationClient {
    // Acá declaramos la función de la clase que nos obtendrá
    // La ubicación
    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        // Usamos un callbackFlow para poder
        // Ir actualizando la ubicación, junto con sus permisos
        return callbackFlow {
            // Hay que revisar si el usuario aceptó los permisos
            // de localización
            if(!context.hasLocationPermission()) {
                throw LocationClient.LocationException("Missing location permission")
            }
            // Revisamos si es accesible
            // la ubicación del dispositivo
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if(!isGpsEnabled && !isNetworkEnabled) {
                throw LocationClient.LocationException("GPS is disabled")
            }
            // Creamos peticiones para la ubi
            // definimos cada cuanto lo haremos,
            // con el intervalo más rápido, y el
            // intervalo promedio
            val request = LocationRequest.create()
                .setInterval(interval)
                .setFastestInterval(interval)

            // Creamos un callback que será llamado
            // cada vez que fusedLocationProvideClient
            // busque una nueva locacion(linea 19)
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    // Ahora el último elemento de la lista de locations
                    // será la última locación que fue buscada
                    // Así que validamos por la existencia de el último valor
                    result.locations.lastOrNull()?.let { location ->
                        launch { send(location) }
                    }
                }
            }
            // request de actualizacion de ubicacion
            client.requestLocationUpdates(
                request, // nuestra variable request L. 47
                locationCallback, // nuestra variable de callback L.54
                Looper.getMainLooper() // Loop del callback flow
            )
            // Como es un ciclo de callbacks, falta definir
            // el como parar
            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}