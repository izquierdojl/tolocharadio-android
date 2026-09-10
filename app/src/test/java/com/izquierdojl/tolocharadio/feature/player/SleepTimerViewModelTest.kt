package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.domain.SleepTimerDuration
import com.izquierdojl.tolocharadio.domain.SleepTimerState
import com.izquierdojl.tolocharadio.domain.SleepTimerUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testScope = TestScope(UnconfinedTestDispatcher())
    private lateinit var useCase: SleepTimerUseCase
    private lateinit var viewModel: SleepTimerViewModel

    @Before
    fun setup() {
        useCase = SleepTimerUseCase(testScope)
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
    fun `start updates uiState to Active with remaining minutes`() =
        testScope.runTest {
            val states = mutableListOf<SleepTimerUiState>()
            val subscription = launch { viewModel.uiState.collect { states.add(it) } }

            viewModel.start(SleepTimerDuration.MINUTES_15)

            val uiState = states.last()
            assertTrue(uiState is SleepTimerUiState.Active)
            assertEquals(15, (uiState as SleepTimerUiState.Active).remainingMinutes)
            subscription.cancel()
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
    fun `uiState keeps remaining minutes until a full minute elapses`() =
        testScope.runTest {
            val states = mutableListOf<SleepTimerUiState>()
            val subscription = launch { viewModel.uiState.collect { states.add(it) } }

            viewModel.start(SleepTimerDuration.MINUTES_15)
            advanceTimeBy(59_000L)

            val uiState = states.last()
            assertTrue(uiState is SleepTimerUiState.Active)
            assertEquals(15, (uiState as SleepTimerUiState.Active).remainingMinutes)
            subscription.cancel()
        }

    @Test
    fun `uiState decrements remaining minutes after a full minute`() =
        testScope.runTest {
            val states = mutableListOf<SleepTimerUiState>()
            val subscription = launch { viewModel.uiState.collect { states.add(it) } }

            viewModel.start(SleepTimerDuration.MINUTES_15)
            advanceTimeBy(60_500L)

            val uiState = states.last() as SleepTimerUiState.Active
            assertEquals(14, uiState.remainingMinutes)
            subscription.cancel()
        }
}
