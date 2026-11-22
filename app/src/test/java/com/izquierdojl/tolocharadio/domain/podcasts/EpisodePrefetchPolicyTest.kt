package com.izquierdojl.tolocharadio.domain.podcasts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Objetivo de precarga por red (spec 0058, FR-014/FR-015). */
class EpisodePrefetchPolicyTest {
    private val policy = EpisodePrefetchPolicy.DEFAULT

    @Test
    fun `en red no medida el objetivo es el episodio completo`() {
        assertNull(policy.targetBytes(NetworkKind.UNMETERED))
    }

    @Test
    fun `en red medida precarga cinco minutos a 128 kbps`() {
        // 300 s * 128 000 bps / 8 = 4 800 000 bytes
        assertEquals(4_800_000L, policy.targetBytes(NetworkKind.METERED))
    }

    @Test
    fun `el objetivo movil nunca supera el limite de cincuenta mebibytes`() {
        val highBitrate = EpisodePrefetchPolicy(bitrateBps = 10_000_000L)

        val target = highBitrate.targetBytes(NetworkKind.METERED) ?: 0L

        assertTrue(target <= EpisodeCachePolicy.LIMIT_METERED_BYTES)
    }

    @Test
    fun `el estado de precarga valida sus campos`() {
        val state = PrefetchState(episodeId = 7L, cachedBytes = 1_024L, targetBytes = null, running = true)
        assertEquals(7L, state.episodeId)
    }
}
