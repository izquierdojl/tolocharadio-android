package com.izquierdojl.tolocharadio.feature.player

import android.util.Log
import com.izquierdojl.tolocharadio.domain.playback.InterruptionCause
import com.izquierdojl.tolocharadio.domain.playback.PlaybackFailure
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Test

class PlaybackRecoveryLoggerTest {
    @Test
    fun `el fallback registra causa y codigos sin PII`() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        try {
            PlaybackRecoveryLogger().fallback(
                stationId = "s1",
                from = PlaybackRoute.CONTINUOUS,
                to = PlaybackRoute.LEGACY,
                failure = PlaybackFailure(InterruptionCause.UNSUPPORTED_FORMAT, 415, 2001),
                attempt = 2,
            )
            verify {
                Log.i(
                    "PlaybackRecovery",
                    match {
                        it.contains("event=live_fallback") &&
                            it.contains("httpStatus=415") &&
                            it.contains("playerErrorCode=2001") &&
                            !it.contains("http://") &&
                            !it.contains("https://")
                    },
                )
            }
        } finally {
            unmockkStatic(Log::class)
        }
    }

    @Test
    fun `el exito registra http y error code del fallo`() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        try {
            PlaybackRecoveryLogger().success(
                failure = PlaybackFailure(InterruptionCause.STATION_UNAVAILABLE, 503, 2004),
                attempt = 3,
                stationId = "s1",
                durationMs = 42,
            )
            verify {
                Log.i(
                    "PlaybackRecovery",
                    match {
                        it.contains("event=recovery_success") &&
                            it.contains("httpStatus=503") &&
                            it.contains("playerErrorCode=2004") &&
                            it.contains("attempt=3")
                    },
                )
            }
        } finally {
            unmockkStatic(Log::class)
        }
    }

    @Test
    fun `el fallo final registra http y error code`() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        try {
            PlaybackRecoveryLogger().failure(
                failure = PlaybackFailure(InterruptionCause.AUTH_ERROR, 401, 2004),
                attempts = 6,
                stationId = "s1",
                durationMs = 120_000,
            )
            verify {
                Log.w(
                    "PlaybackRecovery",
                    match<String> {
                        it.contains("event=recovery_failed") &&
                            it.contains("httpStatus=401") &&
                            it.contains("playerErrorCode=2004") &&
                            it.contains("attempts=6")
                    },
                )
            }
        } finally {
            unmockkStatic(Log::class)
        }
    }
}
