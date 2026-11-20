package com.izquierdojl.tolocharadio.feature.podcasts.queue

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.repo.PodcastSubscriptionsRepo
import com.izquierdojl.tolocharadio.data.repo.QueueRepo
import com.izquierdojl.tolocharadio.data.repo.QueueState
import com.izquierdojl.tolocharadio.support.testEpisode
import com.izquierdojl.tolocharadio.support.testProgress
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QueueViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val first = testEpisode(id = 1L, title = "En curso")
    private val second = testEpisode(id = 2L, title = "Pendiente")
    private val state =
        MutableStateFlow(
            QueueState(
                revision = 1L,
                items = listOf(first, second),
                limit = 200,
            ),
        )
    private val repo = mockk<QueueRepo>(relaxed = true)
    private val subscriptions = mockk<PodcastSubscriptionsRepo>(relaxed = true)

    init {
        every { repo.queue } returns state
        coEvery { repo.load() } returns ApiResult.Ok(state.value)
    }

    private fun vm() = QueueViewModel(repo, subscriptions)

    @Test
    fun `refreshSilently conserva el contenido y recarga`() =
        runTest {
            val viewModel = vm()
            advanceUntilIdle()
            assertTrue(viewModel.ui.value is QueueUiState.Content)

            val updated =
                state.value.copy(
                    items = listOf(first, second.copy(progress = testProgress(120L, 300L))),
                )
            state.value = updated
            viewModel.refreshSilently()
            advanceUntilIdle()

            assertTrue(viewModel.ui.value is QueueUiState.Content)
            val content = viewModel.ui.value as QueueUiState.Content
            assertEquals(120L, content.items.first { it.id == 2L }.progress?.positionSec)
            coVerify(exactly = 2) { repo.load() }
        }

    @Test
    fun `el render excluye el episodio en curso`() =
        runTest {
            val viewModel = vm()
            advanceUntilIdle()

            viewModel.setPlayingId(first.id)

            val content = viewModel.ui.value as QueueUiState.Content
            assertEquals(listOf(second.id), content.items.map { it.id })
        }
}
