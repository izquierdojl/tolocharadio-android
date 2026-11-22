package com.izquierdojl.tolocharadio.domain.podcasts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Límites 500/50 MB y margen de 5 min (spec 0058, FR-008/FR-015). */
class EpisodeCachePolicyTest {
    @Test
    fun `los limites por defecto son quinientos y cincuenta mebibytes`() {
        val policy = EpisodeCachePolicy.DEFAULT

        assertEquals(524_288_000L, policy.limitUnmeteredBytes)
        assertEquals(52_428_800L, policy.limitMeteredBytes)
        assertEquals(300_000L, policy.prefetchAheadMeteredMs)
    }

    @Test
    fun `limitFor devuelve el tope de la red activa`() {
        val policy = EpisodeCachePolicy.DEFAULT

        assertEquals(524_288_000L, policy.limitFor(NetworkKind.UNMETERED))
        assertEquals(52_428_800L, policy.limitFor(NetworkKind.METERED))
    }

    @Test
    fun `el limite medido no puede superar al no medido`() {
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeCachePolicy(limitUnmeteredBytes = 1_000L, limitMeteredBytes = 2_000L)
        }
    }

    @Test
    fun `el margen de precarga no supera cinco minutos`() {
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeCachePolicy(prefetchAheadMeteredMs = 300_001L)
        }
    }

    @Test
    fun `las estadisticas no admiten valores negativos`() {
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeCacheStats(sizeBytes = -1L, entryCount = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeCacheStats(sizeBytes = 0L, entryCount = -1)
        }
    }
}
