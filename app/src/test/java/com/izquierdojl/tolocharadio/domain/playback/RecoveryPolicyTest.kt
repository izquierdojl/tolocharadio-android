package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryPolicyTest {
    private val policy = RecoveryPolicy()

    @Test
    fun `usa cinco intentos y sesenta segundos`() {
        assertEquals(5, policy.maxAttempts)
        assertEquals(60_000, policy.totalTimeoutMs)
        assertEquals(7_000, policy.attemptTimeoutMs)
        assertEquals(5_000, policy.stallThresholdMs)
        assertEquals(10_000, policy.initialBufferTimeoutMs)
    }

    @Test
    fun `aplica backoff creciente`() {
        assertEquals(
            listOf(0L, 1_000L, 2_000L, 4_000L, 8_000L),
            (1..5).map(policy::delayForAttempt),
        )
        assertEquals(8_000L, policy.delayForAttempt(99))
    }

    @Test
    fun `no permite un sexto intento`() {
        assertTrue(policy.canAttempt(0, 0))
        assertTrue(policy.canAttempt(4, 59_999))
        assertFalse(policy.canAttempt(5, 0))
        assertFalse(policy.canAttempt(0, 60_000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza mas de cinco intentos`() {
        RecoveryPolicy(maxAttempts = 6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza un timeout total mayor de sesenta segundos`() {
        RecoveryPolicy(totalTimeoutMs = 60_001)
    }
}
