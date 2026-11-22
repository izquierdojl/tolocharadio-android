package com.izquierdojl.tolocharadio.data.local.podcasts

import android.content.Context
import androidx.media3.datasource.DataSource
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.FakeConnectivityMonitor
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCachePolicy
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCacheStats
import com.izquierdojl.tolocharadio.domain.podcasts.NetworkKind
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Degradación y contrato de la caché (spec 0058, R4/U1 / FR-005). */
class SimpleCacheEpisodeCacheTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val context =
        mockk<Context>(relaxed = true) {
            every { cacheDir } returns File("/tmp/opencode/tolocha-cache-test")
        }
    private val connectivity = FakeConnectivityMonitor(initialOnline = true)
    private val policy = EpisodeCachePolicy.DEFAULT
    private var created = 0
    private val plain =
        object : DataSource.Factory {
            override fun createDataSource(): DataSource {
                created += 1
                return mockk(relaxed = true)
            }
        }

    private fun cache(scope: kotlinx.coroutines.CoroutineScope) =
        SimpleCacheEpisodeCache(
            context = context,
            plainDataSource = plain,
            connectivity = connectivity,
            policy = policy,
            scope = scope,
        )

    @Test
    fun `sin SimpleCache disponible degrada al datasource plano`() =
        runTest {
            val cache = cache(backgroundScope)

            assertSame(plain, cache.passthrough())
            assertNull(cache.cacheDataSource(1L))
            cache.factoryFor(1L).createDataSource()
            assertEquals(1, created)
            assertEquals(EpisodeCacheStats.EMPTY, cache.stats.first())
        }

    @Test
    fun `clear y cambio de red no fallan sin cache`() =
        runTest {
            val cache = cache(backgroundScope)

            cache.clear()
            cache.setNetworkKind(NetworkKind.METERED)

            assertEquals(EpisodeCacheStats.EMPTY, cache.stats.first())
        }
}
