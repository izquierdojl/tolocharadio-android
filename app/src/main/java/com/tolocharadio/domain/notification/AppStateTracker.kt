package com.tolocharadio.domain.notification

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for tracking the application state.
 */
interface AppStateTracker {
    /**
     * Current application state.
     */
    val currentState: StateFlow<AppState>

    /**
     * Updates the application state.
     * @param state The new application state
     */
    fun updateState(state: AppState)

    /**
     * Checks if the app is in the foreground.
     * @return true if the app is in the foreground, false otherwise
     */
    fun isForeground(): Boolean

    /**
     * Checks if the app is in the background.
     * @return true if the app is in the background, false otherwise
     */
    fun isBackground(): Boolean

    /**
     * Checks if the app is not running.
     * @return true if the app is not running, false otherwise
     */
    fun isNotRunning(): Boolean
}
