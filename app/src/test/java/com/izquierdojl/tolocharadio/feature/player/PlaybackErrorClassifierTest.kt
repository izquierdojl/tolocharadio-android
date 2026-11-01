package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import com.izquierdojl.tolocharadio.domain.playback.InterruptionCause
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorClassifierTest {
    @Test
    fun `401 y 403 se clasifican como auth`() {
        assertEquals(InterruptionCause.AUTH_ERROR, PlaybackErrorClassifier.classify(httpError(401)))
        assertEquals(InterruptionCause.AUTH_ERROR, PlaybackErrorClassifier.classify(httpError(403)))
    }

    @Test
    fun `404 en continua es fallback y en legacy es emisora no disponible`() {
        assertEquals(
            InterruptionCause.LIVE_ROUTE_UNAVAILABLE,
            PlaybackErrorClassifier.classify(httpError(404), PlaybackRoute.CONTINUOUS).cause,
        )
        assertEquals(
            InterruptionCause.STATION_UNAVAILABLE,
            PlaybackErrorClassifier.classify(httpError(404), PlaybackRoute.LEGACY).cause,
        )
        assertEquals(
            InterruptionCause.STATION_UNAVAILABLE,
            PlaybackErrorClassifier.classify(httpError(503)),
        )
    }

    @Test
    fun `415 se clasifica como formato no soportado`() {
        assertEquals(
            InterruptionCause.UNSUPPORTED_FORMAT,
            PlaybackErrorClassifier.classify(httpError(415)),
        )
    }

    @Test
    fun `un decoder no soportado en legacy se clasifica como formato`() {
        val error =
            PlaybackException(
                "decoder",
                null,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            )
        assertEquals(
            InterruptionCause.UNSUPPORTED_FORMAT,
            PlaybackErrorClassifier.classify(error, PlaybackRoute.LEGACY).cause,
        )
    }

    @Test
    fun `un parser no soportado en continua se clasifica como formato`() {
        val error =
            PlaybackException(
                "parser",
                null,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            )
        assertEquals(
            InterruptionCause.UNSUPPORTED_FORMAT,
            PlaybackErrorClassifier.classify(error, PlaybackRoute.CONTINUOUS).cause,
        )
    }

    @Test
    fun `un error de red se clasifica como network`() {
        val error =
            PlaybackException(
                "network",
                null,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            )
        assertEquals(InterruptionCause.NETWORK_ERROR, PlaybackErrorClassifier.classify(error))
    }

    @Test
    fun `recorre una causa anidada hasta el codigo HTTP`() {
        val nested = PlaybackException("outer", httpError(415), PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        assertEquals(
            InterruptionCause.UNSUPPORTED_FORMAT,
            PlaybackErrorClassifier.classify(nested, PlaybackRoute.CONTINUOUS).cause,
        )
    }

    private fun httpError(code: Int): PlaybackException =
        PlaybackException(
            "http",
            HttpDataSource.InvalidResponseCodeException(
                code,
                "error",
                null,
                emptyMap(),
                mockk<DataSpec>(relaxed = true),
                ByteArray(0),
            ),
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        )
}
