package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.domain.SleepTimerDuration
import com.izquierdojl.tolocharadio.domain.SleepTimerState
import com.izquierdojl.tolocharadio.domain.SleepTimerUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var useCase: SleepTimerUseCase
    private lateinit var viewModel: SleepTimerViewModel

    @Before
    fun setup() {
        useCase = SleepTimerUseCase()
        viewModel = SleepTimerViewModel(useCase)
    }

    @Test
    fun `initial state is Inactive`() =
        testScope.runTest {
            assertEquals(SleepTimerState.Inactive, viewModel.state.value)
            assertTrue(viewModel.uiState.value is SleepTimerUiState.Inactive)
        }

    @Test
    fun `start activates timer`() =
        testScope.runTest {
            viewModel.start(SleepTimerDuration.MINUTES_30)

            val state = viewModel.state.value
            assertTrue(state is SleepTimerState.Active)
            assertEquals(30, (state as SleepTimerState.Active).durationMinutes)
        }

    @Test
    fun `start updates uiState to Active with formatted time`() =
        testScope.runTest {
            viewModel.start(SleepTimerDuration.MINUTES_15)

            val uiState = viewModel.uiState.value
            assertTrue(uiState is SleepTimerUiState.Active)
            assertEquals("15:00", (uiState as SleepTimerUiState.Active).remainingFormatted)
        }

    @Test
    fun `cancel returns to Inactive`() =
        testScope.runTest {
            viewModel.start(SleepTimerDuration.MINUTES_60)
            viewModel.cancel()

            assertEquals(SleepTimerState.Inactive, viewModel.state.value)
            assertTrue(viewModel.uiState.value is SleepTimerUiState.Inactive)
        }

    @Test
    fun `expiration calls stopPlayer callback`() =
        testScope.runTest {
            var stopCalled = false
            viewModel.setStopPlayerCallback { stopCalled = true }

            viewModel.start(SleepTimerDuration.MINUTES_15)
            advanceTimeBy(15 * 60 * 1000L + 1000L)

            assertTrue(stopCalled)
            assertEquals(SleepTimerState.Inactive, viewModel.state.value)
        }

    @Test
    fun `expiration does not call stopPlayer when no callback set`() =
        testScope.runTest {
            viewModel.start(SleepTimerDuration.MINUTES_15)
            advanceTimeBy(15 * 60 * 1000L + 1000L)

            assertEquals(SleepTimerState.Inactive, viewModel.state.value)
        }

    @Test
    fun `formatting handles single digit minutes`() =
        testScope.runTest {
            viewModel.start(SleepTimerDuration.MINUTES_15)

            val uiState = viewModel.uiState.value as SleepTimerUiState.Active
            assertEquals("15:00", uiState.remainingFormatted)
        }

    @Test
    fun `formatting handles seconds correctly`() =
        testScope.runTest {
            viewModel.start(SleepTimerDuration.MINUTES_15)
            advanceTimeBy(3000L)

            val uiState = viewModel.uiState.value as SleepTimerUiState.Active
            assertEquals("14:57", uiState.remainingFormatted)
        }
}
