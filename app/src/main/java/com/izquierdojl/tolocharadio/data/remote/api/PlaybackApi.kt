package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.PlaybackStatusDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Reproducción por proxy del servidor, sin credenciales de usuario.
 * Aquí solo el precheck de disponibilidad.
 */
interface PlaybackApi {
    @GET("playback/{stationId}/status")
    suspend fun status(
        @Path("stationId") stationId: String,
    ): Response<PlaybackStatusDto>
}

/** URL del proxy de stream. No se envía ninguna credencial (FR-011). */
fun streamUrl(
    baseUrl: String,
    stationId: String,
): String = baseUrl.trimEnd('/') + "/api/v1/playback/$stationId"
