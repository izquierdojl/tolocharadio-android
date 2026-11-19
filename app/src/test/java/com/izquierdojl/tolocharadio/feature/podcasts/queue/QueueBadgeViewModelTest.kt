package com.izquierdojl.tolocharadio.feature.podcasts.queue

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.repo.QueueRepo
import com.izquierdojl.tolocharadio.data.repo.QueueState
import com.izquierdojl.tolocharadio.support.testEpisode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** ViewModel de la insignia de cola (US5, FR-031–FR-035). */
class QueueBadgeViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val queue = mockk<QueueRepo>(relaxed = true)
    private val state =
        MutableStateFlow(
            QueueState(
                revision = 1,
                items = listOf(testEpisode(1), testEpisode(2), testEpisode(3)),
                limit = 200,
            ),
        )

    private fun viewModel(): QueueBadgeViewModel {
        every { queue.queue } returns state
        coEvery { queue.load() } returns ApiResult.Ok(state.value)
        return QueueBadgeViewModel(queue)
    }

    @Test
    fun `cuenta pendientes y excluye el episodio en curso`() =
        runTest {
            val vm = viewModel()
            assertEquals(3, vm.badge.first { it.pendingCount == 3 }.pendingCount)

            vm.setPlayingId(1L)
            advanceUntilIdle()
            assertEquals(2, vm.badge.first { it.pendingCount == 2 }.pendingCount)
        }

    @Test
    fun `se actualiza al cambiar la cola`() =
        runTest {
            val vm = viewModel()
            assertEquals(3, vm.badge.first { it.pendingCount == 3 }.pendingCount)

            state.value = state.value.copy(items = listOf(testEpisode(1)))
            advanceUntilIdle()
            assertEquals(1, vm.badge.first { it.pendingCount == 1 }.pendingCount)
        }

    @Test
    fun `refresh recarga la cola`() =
        runTest {
            val vm = viewModel()
            vm.refresh()
            advanceUntilIdle()
            coVerify(exactly = 1) { queue.load() }
        }
}
