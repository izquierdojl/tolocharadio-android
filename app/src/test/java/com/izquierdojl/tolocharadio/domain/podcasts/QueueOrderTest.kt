package com.izquierdojl.tolocharadio.domain.podcasts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueOrderTest {
    @Test
    fun `merge repone el episodio oculto en su posicion`() {
        assertEquals(listOf(1L, 2L, 3L), mergeQueueOrder(listOf(1L, 2L, 3L), listOf(1L, 3L), 2L))
    }

    @Test
    fun `merge sin oculto devuelve el orden visible`() {
        assertEquals(listOf(3L, 1L), mergeQueueOrder(listOf(1L, 2L, 3L), listOf(3L, 1L), null))
    }

    @Test
    fun `merge con oculto ausente en la lista devuelve el visible`() {
        assertEquals(listOf(1L), mergeQueueOrder(listOf(1L), listOf(1L), 9L))
    }

    @Test
    fun `apply conserva el orden pedido y descarta desconocidos`() {
        assertEquals(listOf(3L, 1L), applyQueueOrder(listOf(1L, 2L, 3L), listOf(3L, 1L, 9L)))
    }

    @Test
    fun `next tras terminar devuelve el siguiente distinto`() {
        assertEquals(2L, nextQueueEpisodeId(listOf(1L, 2L, 3L), 1L))
        assertEquals(1L, nextQueueEpisodeId(listOf(1L, 2L, 3L), 3L))
    }

    @Test
    fun `next con un unico episodio terminado es nulo`() {
        assertNull(nextQueueEpisodeId(listOf(5L), 5L))
        assertNull(nextQueueEpisodeId(emptyList(), 5L))
    }
}
