package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.PlaybackStatusDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Reproducción por proxy del servidor.
 * Aquí solo el precheck de disponibilidad; el player consume las URLs directas.
 */
interface PlaybackApi {
    @GET("playback/{stationId}/status")
    suspend fun status(
        @Path("stationId") stationId: String,
    ): Response<PlaybackStatusDto>
}

/**
 * URL de la ruta de reserva `/playback/{stationId}`.
 *
 * [attempt] se añade como `?r=` para evitar respuestas cacheadas por
 * intermediarios; el servidor lo ignora y nunca contiene credenciales.
 */
fun streamUrl(
    baseUrl: String,
    stationId: String,
    attempt: Int? = null,
): String = withAttempt(baseUrl.trimEnd('/') + "/api/v1/playback/$stationId", attempt)

/**
 * URL del flujo continuo `/playback/{stationId}/live`.
 *
 * [attempt] se añade como `?r=` en cada conexión o reconexión; el servidor lo
 * ignora. No se incluye ninguna credencial en la URL.
 */
fun liveStreamUrl(
    baseUrl: String,
    stationId: String,
    attempt: Int? = null,
): String = withAttempt(baseUrl.trimEnd('/') + "/api/v1/playback/$stationId/live", attempt)

private fun withAttempt(
    url: String,
    attempt: Int?,
): String = if (attempt == null) url else "$url?r=$attempt"
