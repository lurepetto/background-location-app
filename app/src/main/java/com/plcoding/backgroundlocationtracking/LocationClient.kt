package com.plcoding.backgroundlocationtracking

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    // parametro para saber acada cuanto queremos actualizar
    fun getLocationUpdates(interval: Long): Flow<Location>
    // clase para retornar mensajes de error
    class LocationException(message: String): Exception()
}