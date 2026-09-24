package com.tolocharadio.ui.notification

import android.content.Intent

/**
 * Handles notification tap data extraction for the app's notification entry point.
 */
class NotificationNavigation {
    companion object {
        const val NOTIFICATION_TAP_ACTION = "com.tolocharadio.NOTIFICATION_TAP"
        const val NOTIFICATION_TYPE_EXTRA = "notification_type"
        const val NOTIFICATION_ACTION_EXTRA = "notification_action"
        const val CONTENT_ID_EXTRA = "content_id"
        const val NOTIFICATION_TITLE_EXTRA = "notification_title"
        const val NOTIFICATION_MESSAGE_EXTRA = "notification_message"
    }

    /**
     * Extracts notification data from an intent.
     * @param intent The intent containing notification data
     * @return The extracted notification data, or null if invalid
     */
    fun extractNotificationData(intent: Intent): com.tolocharadio.domain.notification.NotificationData? {
        val type = intent.getStringExtra(NOTIFICATION_TYPE_EXTRA) ?: return null
        val action = intent.getStringExtra(NOTIFICATION_ACTION_EXTRA) ?: return null
        val title = intent.getStringExtra(NOTIFICATION_TITLE_EXTRA) ?: return null
        val message = intent.getStringExtra(NOTIFICATION_MESSAGE_EXTRA) ?: return null
        val contentId = intent.getStringExtra(CONTENT_ID_EXTRA)

        return try {
            val notificationType = com.tolocharadio.domain.notification.NotificationType.valueOf(type)
            val notificationAction = com.tolocharadio.domain.notification.NotificationAction.valueOf(action)

            com.tolocharadio.domain.notification.NotificationData(
                type = notificationType,
                title = title,
                message = message,
                contentId = contentId,
                action = notificationAction,
            )
        } catch (e: IllegalArgumentException) {
            android.util.Log.w("NotificationNavigation", "Extras de notificación inválidos", e)
            null
        }
    }
}
