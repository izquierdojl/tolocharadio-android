package com.tolocharadio.domain.notification

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppStateTrackerTest {
    private lateinit var appStateTracker: AppStateTracker

    @Before
    fun setUp() {
        appStateTracker = InMemoryAppStateTracker()
    }

    @Test
    fun `initial state should be NOT_RUNNING`() =
        runTest {
            assertEquals(AppState.NOT_RUNNING, appStateTracker.currentState.value)
        }

    @Test
    fun `updating state to FOREGROUND should change current state`() =
        runTest {
            appStateTracker.updateState(AppState.FOREGROUND)
            assertEquals(AppState.FOREGROUND, appStateTracker.currentState.value)
        }

    @Test
    fun `updating state to BACKGROUND should change current state`() =
        runTest {
            appStateTracker.updateState(AppState.BACKGROUND)
            assertEquals(AppState.BACKGROUND, appStateTracker.currentState.value)
        }

    @Test
    fun `isForeground should return true when state is FOREGROUND`() =
        runTest {
            appStateTracker.updateState(AppState.FOREGROUND)
            assertTrue(appStateTracker.isForeground())
        }

    @Test
    fun `isForeground should return false when state is BACKGROUND`() =
        runTest {
            appStateTracker.updateState(AppState.BACKGROUND)
            assertFalse(appStateTracker.isForeground())
        }

    @Test
    fun `isBackground should return true when state is BACKGROUND`() =
        runTest {
            appStateTracker.updateState(AppState.BACKGROUND)
            assertTrue(appStateTracker.isBackground())
        }

    @Test
    fun `isBackground should return false when state is FOREGROUND`() =
        runTest {
            appStateTracker.updateState(AppState.FOREGROUND)
            assertFalse(appStateTracker.isBackground())
        }

    @Test
    fun `isNotRunning should return true when state is NOT_RUNNING`() =
        runTest {
            assertTrue(appStateTracker.isNotRunning())
        }

    @Test
    fun `isNotRunning should return false when state is FOREGROUND`() =
        runTest {
            appStateTracker.updateState(AppState.FOREGROUND)
            assertFalse(appStateTracker.isNotRunning())
        }

    @Test
    fun `state should be updated correctly through multiple changes`() =
        runTest {
            appStateTracker.updateState(AppState.FOREGROUND)
            assertEquals(AppState.FOREGROUND, appStateTracker.currentState.value)

            appStateTracker.updateState(AppState.BACKGROUND)
            assertEquals(AppState.BACKGROUND, appStateTracker.currentState.value)

            appStateTracker.updateState(AppState.FOREGROUND)
            assertEquals(AppState.FOREGROUND, appStateTracker.currentState.value)
        }
}

/**
 * In-memory implementation of AppStateTracker for testing.
 */
class InMemoryAppStateTracker : AppStateTracker {
    private val _currentState = kotlinx.coroutines.flow.MutableStateFlow(AppState.NOT_RUNNING)
    override val currentState: kotlinx.coroutines.flow.StateFlow<AppState> = _currentState

    override fun updateState(state: AppState) {
        _currentState.value = state
    }

    override fun isForeground(): Boolean {
        return _currentState.value == AppState.FOREGROUND
    }

    override fun isBackground(): Boolean {
        return _currentState.value == AppState.BACKGROUND
    }

    override fun isNotRunning(): Boolean {
        return _currentState.value == AppState.NOT_RUNNING
    }
}
