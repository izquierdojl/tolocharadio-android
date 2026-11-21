package com.izquierdojl.tolocharadio.feature.podcasts

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.PaginationDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeProgress
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeSearchItem
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeSearchPage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.PodcastSubscriptionsRepo
import com.izquierdojl.tolocharadio.domain.podcasts.SearchSubscriptionEpisodesUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.UnsubscribePodcastUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SubscriptionsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo: PodcastSubscriptionsRepo = mockk()
    private val unsubscribe: UnsubscribePodcastUseCase = mockk()
    private val prefs: InstancePrefs = mockk()
    private val search: SearchSubscriptionEpisodesUseCase = mockk()

    private fun podcast(
        id: Long,
        title: String,
    ) = PodcastSummary(id = id, feedUrl = "https://feed.example/$id", title = title)

    private fun item(
        id: Long,
        podcastId: Long = 1,
        completed: Boolean = false,
    ) = PodcastEpisodeSearchItem(
        id = id,
        podcastId = podcastId,
        title = "Episodio $id",
        progress = PodcastEpisodeProgress(completed = completed),
        podcast = podcast(podcastId, "Podcast $podcastId"),
    )

    private fun page(
        items: List<PodcastEpisodeSearchItem>,
        hasMore: Boolean = false,
    ) = PodcastEpisodeSearchPage(
        items = items,
        pagination = PaginationDto(hasMore = hasMore),
    )

    private fun vm(items: List<PodcastSummary> = listOf(podcast(1, "Uno"))): SubscriptionsViewModel {
        every { repo.subscriptions } returns MutableStateFlow(items)
        every { prefs.podcastSubscriptionOrder } returns MutableStateFlow<List<Long>>(emptyList())
        coEvery { repo.list() } returns ApiResult.Ok(items)
        return SubscriptionsViewModel(repo, unsubscribe, prefs, search)
    }

    private fun searchFor(
        viewModel: SubscriptionsViewModel,
        term: String,
    ) {
        viewModel.setSearchScope(SubscriptionSearchScope.EPISODES)
        viewModel.setSearchTerm(term)
        viewModel.searchEpisodes()
    }

    @Test
    fun `busqueda de episodios publica resultados y paginacion`() =
        runTest {
            val viewModel = vm()
            coEvery { search("hola", 0, 20) } returns
                ApiResult.Ok(page(listOf(item(10)), hasMore = true))

            searchFor(viewModel, "hola")

            val state = viewModel.searchUi.value
            assertEquals(listOf(10L), state.results.map { it.episode.id })
            assertEquals("hola", state.submittedTerm)
            assertTrue(state.showResults)
            assertTrue(state.hasMore)
            assertFalse(state.loading)
        }

    @Test
    fun `cargar mas agrega la pagina sin duplicar episodios`() =
        runTest {
            val viewModel = vm()
            coEvery { search("hola", 0, 20) } returns
                ApiResult.Ok(page(listOf(item(10)), hasMore = true))
            searchFor(viewModel, "hola")

            coEvery { search("hola", 1, 20) } returns
                ApiResult.Ok(page(listOf(item(10), item(11)), hasMore = false))
            viewModel.loadMoreEpisodes()

            val state = viewModel.searchUi.value
            assertEquals(listOf(10L, 11L), state.results.map { it.episode.id })
            assertFalse(state.hasMore)
            assertFalse(state.loadingMore)
        }

    @Test
    fun `busqueda sin coincidencias mantiene resultados activos`() =
        runTest {
            val viewModel = vm()
            coEvery { search("nada", 0, 20) } returns ApiResult.Ok(page(emptyList()))

            searchFor(viewModel, "nada")

            val state = viewModel.searchUi.value
            assertTrue(state.showResults)
            assertTrue(state.results.isEmpty())
            assertEquals(null, state.error)
        }

    @Test
    fun `error permite reintentar y conserva el termino`() =
        runTest {
            val viewModel = vm()
            coEvery { search("hola", 0, 20) } returns
                ApiResult.Err(DomainError.Unavailable("Sin conexión"))
            searchFor(viewModel, "hola")

            val failed = viewModel.searchUi.value
            assertNotNull(failed.error)
            assertEquals("hola", failed.term)
            assertEquals("hola", failed.submittedTerm)
            assertTrue(failed.showResults)

            coEvery { search("hola", 0, 20) } returns ApiResult.Ok(page(listOf(item(10))))
            viewModel.searchEpisodes()

            assertEquals(listOf(10L), viewModel.searchUi.value.results.map { it.episode.id })
            assertEquals(null, viewModel.searchUi.value.error)
        }

    @Test
    fun `cambiar de ambito conserva el termino y limpia los resultados`() =
        runTest {
            val viewModel = vm()
            coEvery { search("hola", 0, 20) } returns ApiResult.Ok(page(listOf(item(10))))
            searchFor(viewModel, "hola")

            viewModel.setSearchScope(SubscriptionSearchScope.PODCASTS)

            val state = viewModel.searchUi.value
            assertEquals("hola", state.term)
            assertEquals(SubscriptionSearchScope.PODCASTS, state.scope)
            assertTrue(state.results.isEmpty())
            assertFalse(state.showResults)
        }

    @Test
    fun `vaciar el termino limpia los resultados y conserva el ambito`() =
        runTest {
            val viewModel = vm()
            coEvery { search("hola", 0, 20) } returns ApiResult.Ok(page(listOf(item(10))))
            searchFor(viewModel, "hola")

            viewModel.setSearchTerm("")

            val state = viewModel.searchUi.value
            assertEquals("", state.term)
            assertEquals(SubscriptionSearchScope.EPISODES, state.scope)
            assertTrue(state.results.isEmpty())
            assertFalse(state.showResults)
        }

    @Test
    fun `atras vuelve a podcasts y conserva el termino`() =
        runTest {
            val viewModel = vm()
            coEvery { search("hola", 0, 20) } returns ApiResult.Ok(page(listOf(item(10))))
            searchFor(viewModel, "hola")

            viewModel.closeEpisodeSearch()

            val state = viewModel.searchUi.value
            assertEquals("hola", state.term)
            assertEquals(SubscriptionSearchScope.PODCASTS, state.scope)
            assertTrue(state.results.isEmpty())
            assertFalse(state.showResults)
        }

    @Test
    fun `la baja elimina los episodios del podcast de los resultados`() =
        runTest {
            val viewModel = vm()
            coEvery { search("hola", 0, 20) } returns
                ApiResult.Ok(page(listOf(item(10, podcastId = 1), item(20, podcastId = 2))))
            searchFor(viewModel, "hola")
            coEvery { unsubscribe(1L) } returns ApiResult.Ok(Unit)

            viewModel.unsubscribe(1L)

            val state = viewModel.searchUi.value
            assertEquals(listOf(20L), state.results.map { it.episode.id })
        }

    @Test
    fun `el progreso local refleja el marcado de reproducido`() =
        runTest {
            val viewModel = vm()
            coEvery { search("hola", 0, 20) } returns
                ApiResult.Ok(page(listOf(item(10, completed = false))))
            searchFor(viewModel, "hola")

            viewModel.onEpisodeProgressChanged(10L)
            assertTrue(viewModel.searchUi.value.results.single().episode.progress?.completed == true)

            viewModel.onEpisodeProgressChanged(10L)
            assertFalse(viewModel.searchUi.value.results.single().episode.progress?.completed == true)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `una respuesta tardia no pisa la busqueda vigente`() =
        runTest {
            val viewModel = vm()
            val first = CompletableDeferred<ApiResult<PodcastEpisodeSearchPage>>()
            coEvery { search("uno", 0, 20) } coAnswers { first.await() }
            coEvery { search("dos", 0, 20) } returns ApiResult.Ok(page(listOf(item(20))))

            viewModel.setSearchScope(SubscriptionSearchScope.EPISODES)
            viewModel.setSearchTerm("uno")
            viewModel.searchEpisodes()
            viewModel.setSearchTerm("dos")
            viewModel.searchEpisodes()

            first.complete(ApiResult.Ok(page(listOf(item(10)))))
            advanceUntilIdle()

            assertEquals(listOf(20L), viewModel.searchUi.value.results.map { it.episode.id })
        }

    @Test
    fun `la recarga manual sustituye la lista sin perder el contenido`() =
        runTest {
            val viewModel = vm(listOf(podcast(1, "Uno")))
            coEvery { repo.list() } returns
                ApiResult.Ok(listOf(podcast(1, "Uno"), podcast(2, "Dos")))

            viewModel.refreshManually()

            val content = viewModel.ui.value as SubscriptionsUiState.Content
            assertEquals(listOf(1L, 2L), content.items.map { it.id })
            assertFalse(viewModel.refreshing.value)
        }

    @Test
    fun `un error de recarga conserva la lista y publica el mensaje`() =
        runTest {
            val viewModel = vm(listOf(podcast(1, "Uno")))
            coEvery { repo.list() } returns
                ApiResult.Err(DomainError.Unavailable("Sin conexión"))

            viewModel.refreshManually()

            assertTrue(viewModel.ui.value is SubscriptionsUiState.Content)
            assertEquals("Sin conexión", viewModel.message.value)
            assertFalse(viewModel.refreshing.value)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `una segunda recarga se ignora mientras la primera esta en vuelo`() =
        runTest {
            val viewModel = vm(listOf(podcast(1, "Uno")))
            val gate = CompletableDeferred<ApiResult<List<PodcastSummary>>>()
            var calls = 0
            coEvery { repo.list() } coAnswers {
                calls++
                gate.await()
            }

            viewModel.refreshManually()
            assertTrue(viewModel.refreshing.value)
            viewModel.refreshManually()

            assertEquals(1, calls)

            gate.complete(ApiResult.Ok(listOf(podcast(1, "Uno"), podcast(2, "Dos"))))
            advanceUntilIdle()

            assertFalse(viewModel.refreshing.value)
            assertEquals(
                listOf(1L, 2L),
                (viewModel.ui.value as SubscriptionsUiState.Content).items.map { it.id },
            )
        }

    @Test
    fun `la recarga conserva el termino local de podcasts`() =
        runTest {
            val viewModel = vm(listOf(podcast(1, "Uno")))
            viewModel.setSearchTerm("un")
            coEvery { repo.list() } returns
                ApiResult.Ok(listOf(podcast(1, "Uno"), podcast(2, "Dos")))

            viewModel.refreshManually()

            assertEquals("un", viewModel.searchUi.value.term)
            assertEquals(SubscriptionSearchScope.PODCASTS, viewModel.searchUi.value.scope)
            assertEquals(
                2,
                (viewModel.ui.value as SubscriptionsUiState.Content).items.size,
            )
        }

    @Test
    fun `la recarga repite la busqueda confirmada desde la primera pagina`() =
        runTest {
            val viewModel = vm()
            coEvery { search("hola", 0, 20) } returns
                ApiResult.Ok(page(listOf(item(10))))
            searchFor(viewModel, "hola")

            coEvery { search("hola", 0, 20) } returns
                ApiResult.Ok(page(listOf(item(11))))
            coEvery { repo.list() } returns ApiResult.Ok(listOf(podcast(1, "Uno")))

            viewModel.refreshManually()

            assertEquals(listOf(11L), viewModel.searchUi.value.results.map { it.episode.id })
            assertEquals("hola", viewModel.searchUi.value.submittedTerm)
            assertEquals(SubscriptionSearchScope.EPISODES, viewModel.searchUi.value.scope)
            coVerify(exactly = 2) { search("hola", 0, 20) }
        }
}
