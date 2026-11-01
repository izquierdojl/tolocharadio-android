package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import com.izquierdojl.tolocharadio.domain.playback.InterruptionCause
import com.izquierdojl.tolocharadio.domain.playback.PlaybackFailure
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute

/** Clasificación pura de errores de Media3 (spec 0042/0044). */
object PlaybackErrorClassifier {
    private const val HTTP_UNAUTHORIZED = 401
    private const val HTTP_FORBIDDEN = 403
    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_METHOD_NOT_ALLOWED = 405
    private const val HTTP_UNSUPPORTED_MEDIA_TYPE = 415
    private const val HTTP_SERVER_ERROR_MIN = 500
    private const val HTTP_SERVER_ERROR_MAX = 599

    /** Compatibilidad: clasifica como ruta continua. */
    fun classify(error: PlaybackException): InterruptionCause = classify(error, PlaybackRoute.CONTINUOUS).cause

    /** Clasifica un error según la ruta activa y conserva HTTP/errorCode para logs. */
    @Suppress("CyclomaticComplexMethod")
    fun classify(
        error: PlaybackException,
        route: PlaybackRoute,
    ): PlaybackFailure {
        val httpCode = httpStatus(error)
        val cause =
            when {
                httpCode == HTTP_UNAUTHORIZED || httpCode == HTTP_FORBIDDEN ->
                    InterruptionCause.AUTH_ERROR
                httpCode == HTTP_UNSUPPORTED_MEDIA_TYPE ->
                    InterruptionCause.UNSUPPORTED_FORMAT
                (httpCode == HTTP_NOT_FOUND || httpCode == HTTP_METHOD_NOT_ALLOWED) &&
                    route == PlaybackRoute.CONTINUOUS ->
                    InterruptionCause.LIVE_ROUTE_UNAVAILABLE
                httpCode == HTTP_NOT_FOUND ||
                    httpCode == HTTP_METHOD_NOT_ALLOWED ||
                    (httpCode != null && httpCode in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX) ->
                    InterruptionCause.STATION_UNAVAILABLE
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
                    InterruptionCause.NETWORK_ERROR
                error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                    error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                    InterruptionCause.UNSUPPORTED_FORMAT
                else -> InterruptionCause.UNKNOWN
            }
        return PlaybackFailure(
            cause = cause,
            httpStatus = httpCode,
            playerErrorCode = error.errorCode,
        )
    }

    /** Código HTTP de la primera [HttpDataSource.InvalidResponseCodeException] de la cadena. */
    fun httpStatus(error: PlaybackException): Int? =
        generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
            .firstOrNull()
            ?.responseCode
}
