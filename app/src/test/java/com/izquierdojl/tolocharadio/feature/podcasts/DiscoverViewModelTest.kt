package com.izquierdojl.tolocharadio.feature.podcasts

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.dto.BulkSubscriptionResult
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.data.repo.PodcastSubscriptionsRepo
import com.izquierdojl.tolocharadio.domain.podcasts.ImportOpmlUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.SubscribePodcastUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.UnsubscribePodcastUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Descubrir: búsqueda, alta manual y OPML (T031, Red→Green). */
class DiscoverViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo: PodcastSubscriptionsRepo = mockk()
    private val subscribe: SubscribePodcastUseCase = mockk()
    private val unsubscribe: UnsubscribePodcastUseCase = mockk()
    private val importOpml: ImportOpmlUseCase = mockk()

    private fun vm(): DiscoverViewModel {
        every { repo.subscribedFeedUrls } returns kotlinx.coroutines.flow.MutableStateFlow(emptySet())
        coEvery { repo.list() } returns ApiResult.Ok(emptyList())
        return DiscoverViewModel(repo, subscribe, unsubscribe, importOpml)
    }

    @Test
    fun `search publica resultados`() =
        runTest {
            val v = vm()
            val r1 = PodcastSearchResult("A", null, "https://a.example/f.xml")
            coEvery { repo.search("tol") } returns ApiResult.Ok(listOf(r1))
            v.onQueryChange("tol")
            v.search()
            assertEquals(listOf(r1), v.ui.value.results)
            assertEquals(false, v.ui.value.searching)
        }

    @Test
    fun `search en blanco no llama a red y avisa`() =
        runTest {
            val v = vm()
            v.onQueryChange("   ")
            v.search()
            assertTrue(v.ui.value.message != null)
        }

    @Test
    fun `alta manual valida la url`() =
        runTest {
            val v = vm()
            coEvery { repo.resolveFeed("no-es-url") } returns
                ApiResult.Err(com.izquierdojl.tolocharadio.core.network.DomainError.Validation(emptyList()))
            v.resolveManual("no-es-url")
            assertTrue(v.ui.value.message != null)
        }

    @Test
    fun `importar OPML informa del resultado`() =
        runTest {
            val v = vm()
            coEvery { importOpml("<opml/>") } returns ApiResult.Ok(BulkSubscriptionResult(imported = 1))
            v.importOpml("<opml/>")
            assertTrue(v.ui.value.message?.contains("1") == true)
        }
}
