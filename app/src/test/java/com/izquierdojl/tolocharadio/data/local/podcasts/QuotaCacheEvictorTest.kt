package com.izquierdojl.tolocharadio.data.local.podcasts

import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Evictor LRU con cuota dinámica (spec 0058, R8 / FR-008). */
class QuotaCacheEvictorTest {
    private val cache = mockk<Cache>(relaxed = true)
    private var space = 0L
    private var limit = 60L
    private var now = 1_000L

    private fun evictor() = QuotaCacheEvictor(limitProvider = { limit }, clock = { now })

    private fun prepareCache() {
        every { cache.cacheSpace } answers { space }
        every { cache.removeResource(any()) } answers {
            space = (space - SPAN_BYTES).coerceAtLeast(0)
            Unit
        }
    }

    private fun span(
        id: Long,
        length: Long = SPAN_BYTES,
    ) = CacheSpan("${EpisodeCache.KEY_PREFIX}$id", 0L, length)

    @Test
    fun `desaloja la entrada menos usada al superar el limite`() {
        prepareCache()
        val evictor = evictor()

        space = SPAN_BYTES
        evictor.onSpanAdded(cache, span(1))
        space = SPAN_BYTES * 2
        evictor.onSpanAdded(cache, span(2))
        space = SPAN_BYTES * 3
        evictor.onSpanAdded(cache, span(3))

        verify(exactly = 1) { cache.removeResource("${EpisodeCache.KEY_PREFIX}1") }
        verify(exactly = 0) { cache.removeResource("${EpisodeCache.KEY_PREFIX}2") }
        assertTrue(space <= limit)
        assertEquals(2, evictor.entries().size)
    }

    @Test
    fun `bajar el limite recorta hasta el nuevo tope`() {
        prepareCache()
        limit = 500
        val evictor = evictor()

        space = SPAN_BYTES
        evictor.onSpanAdded(cache, span(1))
        evictor.onSpanAdded(cache, span(2))
        evictor.onSpanAdded(cache, span(3))
        space = SPAN_BYTES * 3
        now = 2_000L

        limit = 30
        evictor.evict(cache)

        verify { cache.removeResource("${EpisodeCache.KEY_PREFIX}1") }
        verify { cache.removeResource("${EpisodeCache.KEY_PREFIX}2") }
        assertTrue(space <= limit)
    }

    @Test
    fun `al tocar una entrada deja de ser la primera candidata`() {
        prepareCache()
        val evictor = evictor()
        space = SPAN_BYTES
        evictor.onSpanAdded(cache, span(1))
        evictor.onSpanAdded(cache, span(2))
        evictor.onSpanTouched(cache, span(1), span(1))
        space = SPAN_BYTES * 3
        evictor.onSpanAdded(cache, span(3))

        verify(exactly = 0) { cache.removeResource("${EpisodeCache.KEY_PREFIX}1") }
        verify(exactly = 1) { cache.removeResource("${EpisodeCache.KEY_PREFIX}2") }
    }

    private companion object {
        const val SPAN_BYTES = 30L
    }
}
