package com.izquierdojl.tolocharadio.cast

import org.junit.Assert.assertEquals
import org.junit.Test

/** Política de fallback Cast (bugs 0031/0032): nunca duplicar audio local y remoto. */
class CastFallbackPolicyTest {
    @Test
    fun `parada manual con reproduccion activa reanuda local sonando`() {
        assertEquals(
            CastFallbackDecision.RESUME_LOCAL,
            CastFallbackPolicy.decide(userInitiated = true, remoteWasPlaying = true),
        )
    }

    @Test
    fun `parada manual con remoto pausado prepara local en pausa`() {
        assertEquals(
            CastFallbackDecision.RESUME_LOCAL_PAUSED,
            CastFallbackPolicy.decide(userInitiated = true, remoteWasPlaying = false),
        )
    }

    @Test
    fun `perdida de conexion no reanuda local aunque estuviera sonando`() {
        assertEquals(
            CastFallbackDecision.DO_NOT_RESUME_LOCAL,
            CastFallbackPolicy.decide(userInitiated = false, remoteWasPlaying = true),
        )
    }

    @Test
    fun `perdida de conexion con remoto pausado no reanuda local`() {
        assertEquals(
            CastFallbackDecision.DO_NOT_RESUME_LOCAL,
            CastFallbackPolicy.decide(userInitiated = false, remoteWasPlaying = false),
        )
    }
}
