package com.tolocharadio.ui.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.tolocharadio.domain.notification.NotificationChannels

/**
 * Manages notification channels for the application.
 */
class NotificationChannelManager(
    private val context: Context,
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Creates all notification channels.
     */
    fun createAllChannels() {
        val channels = NotificationChannels.getAllChannels()
        channels.forEach { config ->
            createChannel(config.id, config.name, config.description, config.importance)
        }
    }

    /**
     * Creates a notification channel.
     * @param channelId The channel ID
     * @param channelName The channel name
     * @param channelDescription The channel description
     * @param importance The importance level
     */
    fun createChannel(
        channelId: String,
        channelName: String,
        channelDescription: String,
        importance: Int,
    ) {
        val channel =
            NotificationChannel(
                channelId,
                channelName,
                importance,
            ).apply {
                description = channelDescription
            }

        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Checks if a notification channel exists.
     * @param channelId The channel ID
     * @return true if the channel exists, false otherwise
     */
    fun channelExists(channelId: String): Boolean {
        return notificationManager.getNotificationChannel(channelId) != null
    }

    /**
     * Deletes a notification channel.
     * @param channelId The channel ID
     */
    fun deleteChannel(channelId: String) {
        notificationManager.deleteNotificationChannel(channelId)
    }

    /**
     * Gets all notification channel IDs.
     * @return List of channel IDs
     */
    fun getAllChannelIds(): List<String> {
        return NotificationChannels.getAllChannels().map { it.id }
    }
}
