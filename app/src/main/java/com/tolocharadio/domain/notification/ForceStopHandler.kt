package com.tolocharadio.domain.notification

import android.app.ActivityManager
import android.content.Context

/**
 * Handles force-stopped app state detection.
 */
class ForceStopHandler(
    private val context: Context
) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    /**
     * Checks if the app is running.
     * @return true if the app is running, false otherwise
     */
    fun isAppRunning(): Boolean {
        val packageName = context.packageName
        val runningProcesses = activityManager.runningAppProcesses
        
        return runningProcesses?.any { process ->
            process.processName == packageName
        } ?: false
    }
    
    /**
     * Checks if the app was force-stopped.
     * @return true if the app was force-stopped, false otherwise
     */
    fun wasForceStopped(): Boolean {
        return !isAppRunning()
    }
    
    /**
     * Gets the appropriate action for force-stopped state.
     * @return The action to perform when app was force-stopped
     */
    fun getActionForForceStopped(): NotificationAction {
        // When app was force-stopped, launch to main screen
        return NotificationAction.OPEN_MAIN
    }
    
    /**
     * Gets the app state based on running status.
     * @return The appropriate app state
     */
    fun getAppState(): AppState {
        return if (isAppRunning()) {
            AppState.FOREGROUND
        } else {
            AppState.NOT_RUNNING
        }
    }
}
