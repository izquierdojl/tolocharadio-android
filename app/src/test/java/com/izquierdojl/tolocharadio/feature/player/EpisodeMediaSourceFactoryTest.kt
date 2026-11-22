package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.datasource.DataSource
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

/** Clave de caché por episodio y exclusión de locales (spec 0058, FR-007/FR-009). */
class EpisodeMediaSourceFactoryTest {
    private val cache = mockk<EpisodeCache>(relaxed = true)
    private val factory = EpisodeMediaSourceFactory(cache)

    private fun dataSourceFactory(): DataSource.Factory =
        object : DataSource.Factory {
            override fun createDataSource(): DataSource = mockk(relaxed = true)
        }

    @Test
    fun `la clave de cache es el episodio y no la url`() {
        assertEquals("episode-42", factory.keyFor(42L))
    }

    @Test
    fun `streaming usa la cache del episodio`() {
        every { cache.factoryFor(42L) } returns dataSourceFactory()

        factory.dataSourceFor(episodeId = 42L, local = false)

        verify(exactly = 1) { cache.factoryFor(42L) }
        verify(exactly = 0) { cache.passthrough() }
    }

    @Test
    fun `un archivo local no usa cache`() {
        every { cache.passthrough() } returns dataSourceFactory()

        factory.dataSourceFor(episodeId = 42L, local = true)

        verify(exactly = 1) { cache.passthrough() }
        verify(exactly = 0) { cache.factoryFor(any()) }
    }
}
