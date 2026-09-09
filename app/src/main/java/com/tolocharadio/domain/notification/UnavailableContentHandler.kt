package com.tolocharadio.domain.notification

/**
 * Handles unavailable content scenarios.
 */
class UnavailableContentHandler {
    
    /**
     * Checks if content is available.
     * @param contentId The content ID to check
     * @return true if content is available, false otherwise
     */
    fun isContentAvailable(contentId: String?): Boolean {
        // In a real implementation, this would check if the content exists
        // For now, we assume content is available if contentId is not null
        return contentId != null && contentId.isNotBlank()
    }
    
    /**
     * Gets the appropriate action when content is unavailable.
     * @param notificationType The notification type
     * @return The action to perform when content is unavailable
     */
    fun getActionForUnavailableContent(notificationType: NotificationType): NotificationAction {
        return when (notificationType) {
            NotificationType.PLAYBACK -> NotificationAction.OPEN_PLAYER
            NotificationType.CONTENT -> NotificationAction.OPEN_MAIN
            NotificationType.SYSTEM -> NotificationAction.OPEN_MAIN
        }
    }
    
    /**
     * Gets the appropriate message when content is unavailable.
     * @param notificationType The notification type
     * @return The message to display
     */
    fun getUnavailableMessage(notificationType: NotificationType): String {
        return when (notificationType) {
            NotificationType.PLAYBACK -> "Radio no disponible"
            NotificationType.CONTENT -> "Contenido no disponible"
            NotificationType.SYSTEM -> "Información no disponible"
        }
    }
    
    /**
     * Handles unavailable content scenario.
     * @param contentId The content ID
     * @param notificationType The notification type
     * @return The action to perform
     */
    fun handleUnavailableContent(contentId: String?, notificationType: NotificationType): NotificationAction {
        if (isContentAvailable(contentId)) {
            // Content is available, proceed with normal action
            return when (notificationType) {
                NotificationType.PLAYBACK -> NotificationAction.OPEN_PLAYER
                NotificationType.CONTENT -> NotificationAction.OPEN_CONTENT
                NotificationType.SYSTEM -> NotificationAction.OPEN_INFO
            }
        } else {
            // Content is unavailable, return appropriate fallback action
            return getActionForUnavailableContent(notificationType)
        }
    }
}
