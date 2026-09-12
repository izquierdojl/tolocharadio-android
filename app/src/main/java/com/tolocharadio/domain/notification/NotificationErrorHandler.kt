package com.tolocharadio.domain.notification

/**
 * Handles notification errors.
 */
class NotificationErrorHandler {
    /**
     * Handles invalid notification data error.
     * @param notificationData The invalid notification data
     * @return The action to perform
     */
    fun handleInvalidData(notificationData: NotificationData?): NotificationAction {
        NotificationLogger.logError("Invalid notification data")
        return NotificationAction.OPEN_MAIN
    }

    /**
     * Handles navigation error.
     * @param deepLink The deep link that failed
     * @param cause The cause of the error
     * @return The action to perform
     */
    fun handleNavigationError(
        deepLink: String,
        cause: Throwable? = null,
    ): NotificationAction {
        NotificationLogger.logError("Navigation failed for deepLink: $deepLink", cause)
        return NotificationAction.OPEN_MAIN
    }

    /**
     * Handles content unavailable error.
     * @param contentId The content ID
     * @param notificationType The notification type
     * @return The action to perform
     */
    fun handleContentUnavailable(
        contentId: String?,
        notificationType: NotificationType,
    ): NotificationAction {
        NotificationLogger.logError("Content unavailable: contentId=$contentId, type=$notificationType")
        return when (notificationType) {
            NotificationType.PLAYBACK -> NotificationAction.OPEN_PLAYER
            NotificationType.CONTENT -> NotificationAction.OPEN_MAIN
            NotificationType.SYSTEM -> NotificationAction.OPEN_MAIN
        }
    }

    /**
     * Handles lock screen error.
     * @param notificationData The notification data
     * @return The action to perform
     */
    fun handleLockScreenError(notificationData: NotificationData): NotificationAction {
        NotificationLogger.logSecurityEvent("Lock screen error for notification: ${notificationData.type}")
        return NotificationAction.OPEN_MAIN
    }

    /**
     * Handles force stop error.
     * @return The action to perform
     */
    fun handleForceStopError(): NotificationAction {
        NotificationLogger.logSecurityEvent("Force stop error")
        return NotificationAction.OPEN_MAIN
    }

    /**
     * Handles unknown error.
     * @param cause The cause of the error
     * @return The action to perform
     */
    fun handleUnknownError(cause: Throwable? = null): NotificationAction {
        NotificationLogger.logError("Unknown error", cause)
        return NotificationAction.OPEN_MAIN
    }
}
