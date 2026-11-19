package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.support.testEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Insignia de pendientes (US5, FR-031–FR-036). */
class QueueBadgeTest {
    @Test
    fun `cuenta todos los pendientes sin episodio en curso`() {
        val badge = queueBadgeFor(listOf(testEpisode(1), testEpisode(2), testEpisode(3)), playingId = null)
        assertEquals(3, badge.pendingCount)
        assertEquals("3", badge.displayText)
    }

    @Test
    fun `excluye el episodio en reproduccion`() {
        val badge = queueBadgeFor(listOf(testEpisode(1), testEpisode(2), testEpisode(3)), playingId = 2L)
        assertEquals(2, badge.pendingCount)
    }

    @Test
    fun `cola vacia no muestra insignia`() {
        val badge = queueBadgeFor(emptyList(), playingId = null)
        assertEquals(0, badge.pendingCount)
        assertNull(badge.displayText)
    }

    @Test
    fun `mas de 99 se compacta`() {
        val items = (1L..100L).map { testEpisode(it) }
        assertEquals("99+", queueBadgeFor(items, playingId = null).displayText)
    }

    @Test
    fun `descripcion accesible en singular y plural`() {
        assertEquals("1 episodio en cola", QueueBadge(1).contentDescription)
        assertEquals("2 episodios en cola", QueueBadge(2).contentDescription)
    }
}
