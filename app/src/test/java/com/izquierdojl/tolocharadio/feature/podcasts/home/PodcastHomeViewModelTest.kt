package com.izquierdojl.tolocharadio.feature.podcasts.home

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastHome
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastQueueSummary
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.EpisodesRepo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PodcastHomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo: EpisodesRepo = mockk()

    private fun podcast(id: Long) =
        PodcastSummary(
            id = id,
            feedUrl = "https://feed.example/$id",
            title = "Podcast $id",
        )

    private fun home(vararg ids: Long) =
        PodcastHome(
            subscriptions = ids.map(::podcast),
            queue = PodcastQueueSummary(count = ids.size),
        )

    @Test
    fun `la recarga manual mantiene el contenido y lo sustituye al terminar`() =
        runTest {
            coEvery { repo.home() } returns ApiResult.Ok(home(1))
            val viewModel = PodcastHomeViewModel(repo)

            coEvery { repo.home() } returns ApiResult.Ok(home(1, 2))
            viewModel.refreshManually()

            val content = viewModel.ui.value as PodcastHomeUi.Content
            assertEquals(2, content.home.subscriptions.size)
            assertFalse(viewModel.refreshing.value)
        }

    @Test
    fun `un error de recarga conserva el contenido y publica el mensaje`() =
        runTest {
            coEvery { repo.home() } returns ApiResult.Ok(home(1))
            val viewModel = PodcastHomeViewModel(repo)

            coEvery { repo.home() } returns ApiResult.Err(DomainError.Unavailable("Sin conexión"))
            viewModel.refreshManually()

            assertTrue(viewModel.ui.value is PodcastHomeUi.Content)
            assertEquals("Sin conexión", viewModel.message.value)
            assertFalse(viewModel.refreshing.value)
        }

    @Test
    fun `sin contenido previo la recarga no cambia el estado de error`() =
        runTest {
            coEvery { repo.home() } returns ApiResult.Err(DomainError.Unavailable("Sin conexión"))
            val viewModel = PodcastHomeViewModel(repo)

            viewModel.refreshManually()

            assertTrue(viewModel.ui.value is PodcastHomeUi.Error)
            assertFalse(viewModel.refreshing.value)
            coVerify(exactly = 1) { repo.home() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `una segunda recarga se ignora mientras la primera esta en vuelo`() =
        runTest {
            coEvery { repo.home() } returns ApiResult.Ok(home(1))
            val viewModel = PodcastHomeViewModel(repo)
            val gate = CompletableDeferred<ApiResult<PodcastHome>>()
            var calls = 0
            coEvery { repo.home() } coAnswers {
                calls++
                gate.await()
            }

            viewModel.refreshManually()
            assertTrue(viewModel.refreshing.value)
            viewModel.refreshManually()

            assertEquals(1, calls)

            gate.complete(ApiResult.Ok(home(1, 2)))
            advanceUntilIdle()

            assertFalse(viewModel.refreshing.value)
            assertEquals(2, (viewModel.ui.value as PodcastHomeUi.Content).home.subscriptions.size)
        }

    @Test
    fun `una recarga sin datos pasa al estado vacio`() =
        runTest {
            coEvery { repo.home() } returns ApiResult.Ok(home(1))
            val viewModel = PodcastHomeViewModel(repo)

            coEvery { repo.home() } returns ApiResult.Ok(PodcastHome())
            viewModel.refreshManually()

            assertTrue(viewModel.ui.value is PodcastHomeUi.Empty)
            assertFalse(viewModel.refreshing.value)
        }
}
