package com.izquierdojl.tolocharadio.data.local.podcasts

import com.izquierdojl.tolocharadio.core.network.FakeConnectivityMonitor
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCache
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackSource
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePrefetchPolicy
import com.izquierdojl.tolocharadio.domain.podcasts.PrefetchState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Ciclo de la precarga sin depender de un `CacheWriter` real (spec 0058, R6). */
class CacheWriterEpisodePrefetchTest {
    private val connectivity = FakeConnectivityMonitor(initialOnline = true)
    private val cache = mockk<EpisodeCache>(relaxed = true)
    private val policy = EpisodePrefetchPolicy.DEFAULT

    private fun prefetch(scope: CoroutineScope) =
        CacheWriterEpisodePrefetch(
            scope = scope,
            connectivity = connectivity,
            cache = cache,
            policy = policy,
        )

    private fun source(local: Boolean) = EpisodePlaybackSource("https://cdn/e.mp3", local = local, expiresAt = 0L)

    @Test
    fun `un episodio local no precarga`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val prefetch = prefetch(scope)

        prefetch.start(source(local = true), episodeId = 1L, positionMs = 0L)

        assertEquals(PrefetchState.IDLE, prefetch.state.value)
        verify(exactly = 0) { cache.cacheDataSource(any()) }
        scope.cancel()
    }

    @Test
    fun `stop deja el estado idle`() {
        every { cache.cacheDataSource(1L) } returns null
        val scope = CoroutineScope(Dispatchers.Default)
        val prefetch = prefetch(scope)
        prefetch.start(source(local = false), episodeId = 1L, positionMs = 0L)

        prefetch.stop()

        assertEquals(PrefetchState.IDLE, prefetch.state.value)
        scope.cancel()
    }

    @Test
    fun `sin cache disponible no queda en running`() =
        runBlocking {
            every { cache.cacheDataSource(1L) } returns null
            val scope = CoroutineScope(Dispatchers.Default)
            val prefetch = prefetch(scope)

            prefetch.start(source(local = false), episodeId = 1L, positionMs = 0L)
            val state = withTimeout(5_000) { prefetch.state.first { it.episodeId == 1L && !it.running } }

            assertFalse(state.running)
            scope.cancel()
        }
}
