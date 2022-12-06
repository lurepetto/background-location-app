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
    // Este es para obtener la ubicacion desde la dependencia de android
    private val client: FusedLocationProviderClient
): LocationClient {

    @SuppressLint("MissingPermission")
    // Implementacion del metodo de la interfaz, donde retornaremos el flow
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        // transforma un callback a un flow
        return callbackFlow {
            // validar aceptación de permisos de usuario
            if(!context.hasLocationPermission()) {
                throw LocationClient.LocationException("Missing location permission")
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            // Revisamos si es accesible la ubicación del dispositivo
            if(!isGpsEnabled && !isNetworkEnabled) {
                throw LocationClient.LocationException("GPS is disabled")
            }

            // Peticion de ubi. Definimos cada cuanto y con qué exactitud
            val request = LocationRequest.create()
                .setInterval(interval)
                .setFastestInterval(interval)

            // Acá se crea el callback con el que proveeremos la ultima ubicacion
            val locationCallback = object : LocationCallback() {
                //  esta función será llamada cada vez que el proveedor fusedLocation
                // nos entregue la info
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    // result contiene una lista de locations
                    // y la ultima es la más reciente, por lo que
                    // enviamos esta a través del flow
                    result.locations.lastOrNull()?.let { location ->
                        launch { send(location) }
                    }
                }
            }

            // Acá ocurre la magia
            // como explicamos anteriormente, callbackFlow sirve para callbacks
            // con ciclo de vida. Este es el inicio
            client.requestLocationUpdates(
                request, // nuestra variable request L. 42
                locationCallback, // nuestra variable de callback L.47
                Looper.getMainLooper() // Loop del callback flow
            )
            // y cuando hagamos esta llamada se terminará el flow
            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}