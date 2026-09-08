package com.izquierdojl.tolocharadio.domain

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
class SleepTimerUseCaseTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var useCase: SleepTimerUseCase

    @Before
    fun setup() {
        useCase = SleepTimerUseCase()
    }

    @Test
    fun `initial state is Inactive`() =
        testScope.runTest {
            assertEquals(SleepTimerState.Inactive, useCase.state.value)
        }

    @Test
    fun `start activates timer with correct duration`() =
        testScope.runTest {
            var expired = false
            useCase.start(SleepTimerDuration.MINUTES_15) { expired = true }

            val state = useCase.state.value
            assertTrue(state is SleepTimerState.Active)
            assertEquals(15, (state as SleepTimerState.Active).durationMinutes)
            assertEquals(15 * 60L, state.remainingSeconds)
        }

    @Test
    fun `start replaces previous timer`() =
        testScope.runTest {
            var expiredCount = 0
            useCase.start(SleepTimerDuration.MINUTES_30) { expiredCount++ }
            useCase.start(SleepTimerDuration.MINUTES_15) { expiredCount++ }

            val state = useCase.state.value
            assertTrue(state is SleepTimerState.Active)
            assertEquals(15, (state as SleepTimerState.Active).durationMinutes)
        }

    @Test
    fun `cancel returns to Inactive`() =
        testScope.runTest {
            useCase.start(SleepTimerDuration.MINUTES_60) { }
            useCase.cancel()

            assertEquals(SleepTimerState.Inactive, useCase.state.value)
        }

    @Test
    fun `countdown decrements remaining seconds`() =
        testScope.runTest {
            useCase.start(SleepTimerDuration.MINUTES_15) { }

            advanceTimeBy(3000L)

            val state = useCase.state.value
            assertTrue(state is SleepTimerState.Active)
            assertEquals(15 * 60L - 3, (state as SleepTimerState.Active).remainingSeconds)
        }

    @Test
    fun `expiration emits Inactive and calls onExpired`() =
        testScope.runTest {
            var expired = false
            useCase.start(SleepTimerDuration.MINUTES_15) { expired = true }

            advanceTimeBy(15 * 60 * 1000L + 1000L)

            assertEquals(SleepTimerState.Inactive, useCase.state.value)
            assertTrue(expired)
        }

    @Test
    fun `cancel stops countdown`() =
        testScope.runTest {
            var expired = false
            useCase.start(SleepTimerDuration.MINUTES_15) { expired = true }

            advanceTimeBy(5000L)
            useCase.cancel()
            advanceTimeBy(20000L)

            assertEquals(SleepTimerState.Inactive, useCase.state.value)
            assertTrue(!expired)
        }
}
