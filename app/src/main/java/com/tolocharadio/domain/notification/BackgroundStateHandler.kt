package com.tolocharadio.domain.notification

/**
 * Handles background state and determines appropriate actions for notifications.
 */
class BackgroundStateHandler {
    /**
     * Determines the appropriate action based on app state and notification data.
     * @param appState The current app state
     * @param notificationData The notification data
     * @return The appropriate action to perform
     */
    fun determineAction(
        appState: AppState,
        notificationData: NotificationData,
    ): NotificationAction {
        return when (appState) {
            AppState.FOREGROUND -> {
                // In foreground, use the notification's action
                notificationData.action
            }
            AppState.BACKGROUND -> {
                // In background, use the notification's action to bring app to foreground
                notificationData.action
            }
            AppState.NOT_RUNNING -> {
                // Not running, launch to main screen
                NotificationAction.OPEN_MAIN
            }
        }
    }

    /**
     * Checks if the app should be launched from scratch.
     * @param appState The current app state
     * @return true if the app should be launched from scratch, false otherwise
     */
    fun shouldLaunchFromScratch(appState: AppState): Boolean {
        return appState == AppState.NOT_RUNNING
    }

    /**
     * Checks if the app should be brought to foreground.
     * @param appState The current app state
     * @return true if the app should be brought to foreground, false otherwise
     */
    fun shouldBringToForeground(appState: AppState): Boolean {
        return appState == AppState.BACKGROUND || appState == AppState.NOT_RUNNING
    }
}
