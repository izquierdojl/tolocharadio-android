package com.tolocharadio.domain.notification

/**
 * Routes notifications to appropriate actions based on their type.
 */
class NotificationTypeRouter {
    
    /**
     * Routes a notification to the appropriate action based on its type.
     * @param type The notification type
     * @param contentId Optional content ID
     * @return The appropriate action to perform
     */
    fun routeNotification(type: NotificationType, contentId: String?): NotificationAction {
        return when (type) {
            NotificationType.PLAYBACK -> NotificationAction.OPEN_PLAYER
            NotificationType.CONTENT -> NotificationAction.OPEN_CONTENT
            NotificationType.SYSTEM -> NotificationAction.OPEN_INFO
        }
    }
    
    /**
     * Gets the deep link pattern for a notification type.
     * @param type The notification type
     * @param contentId Optional content ID
     * @return The deep link pattern
     */
    fun getDeepLink(type: NotificationType, contentId: String?): String {
        return when (type) {
            NotificationType.PLAYBACK -> {
                if (contentId != null) {
                    "tolocharadio://player/$contentId"
                } else {
                    "tolocharadio://player"
                }
            }
            NotificationType.CONTENT -> {
                if (contentId != null) {
                    "tolocharadio://content/$contentId"
                } else {
                    "tolocharadio://content"
                }
            }
            NotificationType.SYSTEM -> {
                if (contentId != null) {
                    "tolocharadio://info/$contentId"
                } else {
                    "tolocharadio://info"
                }
            }
        }
    }
    
    /**
     * Validates that a notification action is appropriate for the notification type.
     * @param type The notification type
     * @param action The notification action
     * @return true if the action is appropriate, false otherwise
     */
    fun validateActionForType(type: NotificationType, action: NotificationAction): Boolean {
        val expectedAction = routeNotification(type, null)
        return action == expectedAction || action == NotificationAction.OPEN_MAIN
    }
}
