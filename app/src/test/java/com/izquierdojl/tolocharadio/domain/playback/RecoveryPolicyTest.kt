package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryPolicyTest {
    private val policy = RecoveryPolicy()

    @Test
    fun `usa seis intentos y dos minutos`() {
        assertEquals(6, policy.maxAttempts)
        assertEquals(120_000, policy.totalTimeoutMs)
        assertEquals(15_000, policy.attemptTimeoutMs)
        assertEquals(15_000, policy.stallThresholdMs)
        assertEquals(15_000, policy.initialBufferTimeoutMs)
        assertEquals(60_000, policy.stableWindowMs)
    }

    @Test
    fun `aplica backoff creciente`() {
        assertEquals(
            listOf(0L, 1_000L, 2_000L, 4_000L, 8_000L, 15_000L),
            (1..6).map(policy::delayForAttempt),
        )
        assertEquals(15_000L, policy.delayForAttempt(99))
    }

    @Test
    fun `no permite un septimo intento`() {
        assertTrue(policy.canAttempt(0, 0))
        assertTrue(policy.canAttempt(5, 119_999))
        assertFalse(policy.canAttempt(6, 0))
        assertFalse(policy.canAttempt(0, 120_000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza mas de seis intentos`() {
        RecoveryPolicy(maxAttempts = 7)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza un timeout total mayor de dos minutos`() {
        RecoveryPolicy(totalTimeoutMs = 120_001)
    }
}
