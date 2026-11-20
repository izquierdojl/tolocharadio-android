package com.izquierdojl.tolocharadio.feature.podcasts.stats

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.repo.PodcastStatsRepo
import com.izquierdojl.tolocharadio.feature.stats.StatsErrorLogger
import com.izquierdojl.tolocharadio.support.testPodcastCategories
import com.izquierdojl.tolocharadio.support.testPodcastHabits
import com.izquierdojl.tolocharadio.support.testPodcastRecents
import com.izquierdojl.tolocharadio.support.testPodcastStatsSummary
import com.izquierdojl.tolocharadio.support.testPodcastTimeline
import com.izquierdojl.tolocharadio.support.testPodcastTopEpisodes
import com.izquierdojl.tolocharadio.support.testPodcastTopPodcasts
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PodcastStatsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo = mockk<PodcastStatsRepo>()
    private val logger = mockk<StatsErrorLogger>(relaxed = true)

    private fun prepareSuccess() {
        coEvery { repo.summary() } returns ApiResult.Ok(testPodcastStatsSummary())
        coEvery { repo.topPodcasts() } returns ApiResult.Ok(testPodcastTopPodcasts())
        coEvery { repo.topEpisodes() } returns ApiResult.Ok(testPodcastTopEpisodes())
        coEvery { repo.timeline() } returns ApiResult.Ok(testPodcastTimeline("day", "2026-11-19" to 1_000L))
        coEvery { repo.habits() } returns ApiResult.Ok(testPodcastHabits(Triple(1, 8, 2_000L)))
        coEvery { repo.categories() } returns ApiResult.Ok(testPodcastCategories("Ciencia" to 1_000L))
        coEvery { repo.recent() } returns ApiResult.Ok(testPodcastRecents(null to 1_000L))
    }

    private fun vm() = PodcastStatsViewModel(repo, logger)

    @Test
    fun `carga el conjunto con matriz de habitos`() =
        runTest {
            prepareSuccess()
            val viewModel = vm()
            advanceUntilIdle()

            val state = viewModel.ui.value as PodcastStatsUi.Content
            assertEquals(7, state.habitsMatrix.size)
            assertEquals(2_000L, state.habitsMatrix[1][8])
        }

    @Test
    fun `un bloque fallido no se sustituye silenciosamente`() =
        runTest {
            prepareSuccess()
            coEvery { repo.categories() } returns
                ApiResult.Err(DomainError.Unavailable("offline"))
            val viewModel = vm()
            advanceUntilIdle()

            assertTrue(viewModel.ui.value is PodcastStatsUi.Error)
        }

    @Test
    fun `error de credenciales se marca como auth`() =
        runTest {
            prepareSuccess()
            coEvery { repo.summary() } returns
                ApiResult.Err(DomainError.Unauthorized("UNAUTHORIZED"))
            val viewModel = vm()
            advanceUntilIdle()

            val state = viewModel.ui.value as PodcastStatsUi.Error
            assertTrue(state.isAuthError)
        }

    @Test
    fun `sin tiempo ni episodios ni recientes muestra vacio`() =
        runTest {
            prepareSuccess()
            coEvery { repo.summary() } returns
                ApiResult.Ok(testPodcastStatsSummary(totalMs = 0L, startedEpisodes = 0, podcastCount = 0))
            coEvery { repo.recent() } returns ApiResult.Ok(testPodcastRecents())
            val viewModel = vm()
            advanceUntilIdle()

            assertEquals(PodcastStatsUi.Empty, viewModel.ui.value)
        }

    @Test
    fun `retry recupera tras un error`() =
        runTest {
            coEvery { repo.summary() } returns
                ApiResult.Err(DomainError.Unavailable("offline"))
            coEvery { repo.topPodcasts() } returns ApiResult.Ok(testPodcastTopPodcasts())
            coEvery { repo.topEpisodes() } returns ApiResult.Ok(testPodcastTopEpisodes())
            coEvery { repo.timeline() } returns ApiResult.Ok(testPodcastTimeline())
            coEvery { repo.habits() } returns ApiResult.Ok(testPodcastHabits())
            coEvery { repo.categories() } returns ApiResult.Ok(testPodcastCategories())
            coEvery { repo.recent() } returns ApiResult.Ok(testPodcastRecents())
            val viewModel = vm()
            advanceUntilIdle()
            assertTrue(viewModel.ui.value is PodcastStatsUi.Error)

            prepareSuccess()
            viewModel.retry()
            advanceUntilIdle()

            assertTrue(viewModel.ui.value is PodcastStatsUi.Content)
        }
}
