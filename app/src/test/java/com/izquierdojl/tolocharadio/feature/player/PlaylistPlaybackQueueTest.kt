package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.domain.playback.PlaybackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistPlaybackQueueTest {
    private val a = PlaybackSource.DirectProgressive("https://a/1.mp3")
    private val b = PlaybackSource.DirectProgressive("https://b/2.mp3")
    private val c = PlaybackSource.DirectProgressive("https://c/3.mp3")

    @Test
    fun `avanza hasta agotar`() {
        val queue = PlaylistPlaybackQueue(listOf(a, b, c))
        assertEquals(a, queue.current)
        assertFalse(queue.exhausted)
        assertTrue(queue.advance())
        assertEquals(b, queue.current)
        assertTrue(queue.advance())
        assertEquals(c, queue.current)
        assertTrue(queue.exhausted)
        assertFalse(queue.advance())
        assertEquals(c, queue.current)
    }

    @Test
    fun `lista de un elemento ya esta agotada`() {
        val queue = PlaylistPlaybackQueue(listOf(a))
        assertTrue(queue.exhausted)
        assertFalse(queue.advance())
    }
}
