package com.tolocharadio.domain.notification

import android.app.NotificationManager

/**
 * Defines the notification channels for the application.
 */
object NotificationChannels {
    
    /** Playback channel ID */
    const val PLAYBACK_CHANNEL_ID = "playback_channel"
    
    /** Content channel ID */
    const val CONTENT_CHANNEL_ID = "content_channel"
    
    /** System channel ID */
    const val SYSTEM_CHANNEL_ID = "system_channel"
    
    /**
     * Returns all notification channel configurations.
     */
    fun getAllChannels(): List<NotificationChannelConfig> {
        return listOf(
            NotificationChannelConfig(
                id = PLAYBACK_CHANNEL_ID,
                name = "Playback Notifications",
                description = "Notifications about current playback",
                importance = NotificationManager.IMPORTANCE_LOW
            ),
            NotificationChannelConfig(
                id = CONTENT_CHANNEL_ID,
                name = "Content Notifications",
                description = "Notifications about new content",
                importance = NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannelConfig(
                id = SYSTEM_CHANNEL_ID,
                name = "System Notifications",
                description = "System messages and alerts",
                importance = NotificationManager.IMPORTANCE_HIGH
            )
        )
    }
    
    /**
     * Returns the channel ID for a given notification type.
     */
    fun getChannelIdForType(type: NotificationType): String {
        return when (type) {
            NotificationType.PLAYBACK -> PLAYBACK_CHANNEL_ID
            NotificationType.CONTENT -> CONTENT_CHANNEL_ID
            NotificationType.SYSTEM -> SYSTEM_CHANNEL_ID
        }
    }
}
