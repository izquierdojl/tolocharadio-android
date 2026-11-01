package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRoutePolicyTest {
    private val policy = RecoveryPolicy()

    @Test
    fun `415 en continua usa un unico intento legacy`() {
        val decision =
            PlaybackRoutePolicy.decide(
                PlaybackRoute.CONTINUOUS,
                InterruptionCause.UNSUPPORTED_FORMAT,
                fallbackAttempted = false,
                policy = policy,
            )
        assertTrue(decision is FallbackDecision.UseLegacy)
        assertEquals(1, (decision as FallbackDecision.UseLegacy).maxAttempts)
    }

    @Test
    fun `404 en continua usa la politica normal en legacy`() {
        val decision =
            PlaybackRoutePolicy.decide(
                PlaybackRoute.CONTINUOUS,
                InterruptionCause.LIVE_ROUTE_UNAVAILABLE,
                fallbackAttempted = false,
                policy = policy,
            )
        assertEquals(
            policy.maxAttempts,
            (decision as FallbackDecision.UseLegacy).maxAttempts,
        )
    }

    @Test
    fun `no hay segundo fallback`() {
        assertTrue(
            PlaybackRoutePolicy.decide(
                PlaybackRoute.LEGACY,
                InterruptionCause.UNSUPPORTED_FORMAT,
                fallbackAttempted = false,
                policy = policy,
            ) is FallbackDecision.None,
        )
        assertTrue(
            PlaybackRoutePolicy.decide(
                PlaybackRoute.CONTINUOUS,
                InterruptionCause.UNSUPPORTED_FORMAT,
                fallbackAttempted = true,
                policy = policy,
            ) is FallbackDecision.None,
        )
    }
}
