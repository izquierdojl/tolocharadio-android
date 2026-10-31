package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import com.izquierdojl.tolocharadio.domain.playback.InterruptionCause

/** Clasificación pura de errores de Media3 (spec 0042, R4/R6). */
object PlaybackErrorClassifier {
    private const val HTTP_UNAUTHORIZED = 401
    private const val HTTP_FORBIDDEN = 403
    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_UNAVAILABLE = 503

    fun classify(error: PlaybackException): InterruptionCause {
        val httpCode = (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode
        return when {
            httpCode == HTTP_UNAUTHORIZED || httpCode == HTTP_FORBIDDEN -> InterruptionCause.AUTH_ERROR
            httpCode == HTTP_NOT_FOUND || httpCode == HTTP_UNAVAILABLE -> InterruptionCause.STATION_UNAVAILABLE
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
                InterruptionCause.NETWORK_ERROR
            else -> InterruptionCause.UNKNOWN
        }
    }
}
