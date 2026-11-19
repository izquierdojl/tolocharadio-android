package com.izquierdojl.tolocharadio.domain.podcasts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Política del potenciador de voz (US4, FR-027/FR-029). */
class VoiceBoostPolicyTest {
    @Test
    fun `solo es efectivo con preferencia y episodio local`() {
        assertTrue(VoiceBoostPolicy.isEffective(preferenceEnabled = true, episodeActive = true, castConnected = false))
    }

    @Test
    fun `la preferencia desactivada nunca aplica`() {
        assertFalse(
            VoiceBoostPolicy.isEffective(
                preferenceEnabled = false,
                episodeActive = true,
                castConnected = false,
            ),
        )
    }

    @Test
    fun `sin episodio activo no aplica`() {
        assertFalse(
            VoiceBoostPolicy.isEffective(
                preferenceEnabled = true,
                episodeActive = false,
                castConnected = false,
            ),
        )
    }

    @Test
    fun `con Cast conectado no aplica`() {
        assertFalse(VoiceBoostPolicy.isEffective(preferenceEnabled = true, episodeActive = true, castConnected = true))
    }
}
