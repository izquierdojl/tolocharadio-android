package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import com.izquierdojl.tolocharadio.domain.playback.InterruptionCause
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
    fun `404 y 503 se clasifican como emisora no disponible`() {
        assertEquals(
            InterruptionCause.STATION_UNAVAILABLE,
            PlaybackErrorClassifier.classify(httpError(404)),
        )
        assertEquals(
            InterruptionCause.STATION_UNAVAILABLE,
            PlaybackErrorClassifier.classify(httpError(503)),
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
