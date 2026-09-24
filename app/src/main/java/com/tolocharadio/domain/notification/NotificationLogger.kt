package com.tolocharadio.domain.notification

import android.util.Log

/**
 * Structured logging for notification events.
 */
object NotificationLogger {
    private const val TAG = "NotificationHandler"

    /**
     * Logs notification tap.
     * @param type The notification type
     * @param action The notification action
     * @param contentId Optional content ID
     */
    fun logNotificationTapped(
        type: NotificationType,
        action: NotificationAction,
        contentId: String?,
    ) {
        Log.d(TAG, "Notification tapped: type=$type, action=$action, contentId=$contentId")
    }

    /**
     * Logs navigation event.
     * @param action The notification action
     * @param deepLink The deep link
     */
    fun logNavigation(
        action: NotificationAction,
        deepLink: String,
    ) {
        Log.d(TAG, "Navigation: action=$action, deepLink=$deepLink")
    }

    /**
     * Logs error event.
     * @param message The error message
     * @param cause The cause of the error
     */
    fun logError(
        message: String,
        cause: Throwable? = null,
    ) {
        if (cause != null) {
            Log.e(TAG, message, cause)
        } else {
            Log.e(TAG, message)
        }
    }

    /**
     * Logs security event.
     * @param event The security event
     */
    fun logSecurityEvent(event: String) {
        Log.w(TAG, "Security event: $event")
    }

    /**
     * Logs performance event.
     * @param operation The operation name
     * @param durationMs The duration in milliseconds
     */
    fun logPerformance(
        operation: String,
        durationMs: Long,
    ) {
        Log.d(TAG, "Performance: $operation took ${durationMs}ms")
    }

    /**
     * Logs app state change.
     * @param oldState The previous app state
     * @param newState The new app state
     */
    fun logAppStateChange(
        oldState: AppState,
        newState: AppState,
    ) {
        Log.d(TAG, "App state changed: $oldState -> $newState")
    }
}
