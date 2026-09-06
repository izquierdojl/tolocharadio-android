package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.PlaybackStatusDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Reproducción por proxy autenticado. El stream se obtiene con header
 * Bearer (ver `AuthDataSourceFactory`); aquí solo el precheck.
 */
interface PlaybackApi {
    @GET("playback/{stationId}/status")
    suspend fun status(
        @Path("stationId") stationId: String,
    ): Response<PlaybackStatusDto>
}

/** URL del proxy de stream. El token viaja en header, jamás en la URL (FR-007). */
fun streamUrl(
    baseUrl: String,
    stationId: String,
): String = baseUrl.trimEnd('/') + "/api/v1/playback/$stationId"

