package com.tolocharadio.ui.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tolocharadio.domain.notification.NotificationChannels
import com.tolocharadio.domain.notification.NotificationType

/**
 * Builds playback notifications.
 */
class PlaybackNotificationBuilder(
    private val context: Context,
) {
    companion object {
        const val PLAYBACK_NOTIFICATION_ID = 1001
    }

    /**
     * Builds a playback notification.
     * @param stationId The station ID
     * @param stationName The station name
     * @param isPlaying Whether playback is active
     * @return The notification
     */
    fun buildNotification(
        stationId: String,
        stationName: String,
        isPlaying: Boolean,
    ): Notification {
        val intent = createNotificationTapIntent(stationId, stationName, isPlaying)
        val pendingIntent = createPendingIntent(intent)

        val title = if (isPlaying) "Now Playing" else "Paused"
        val message = stationName

        return NotificationCompat.Builder(context, NotificationChannels.PLAYBACK_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()
    }

    /**
     * Creates an intent for notification tap.
     */
    private fun createNotificationTapIntent(
        stationId: String,
        stationName: String,
        isPlaying: Boolean,
    ): Intent {
        return Intent(context, com.izquierdojl.tolocharadio.MainActivity::class.java).apply {
            action = NotificationNavigation.NOTIFICATION_TAP_ACTION
            putExtra(NotificationNavigation.NOTIFICATION_TYPE_EXTRA, NotificationType.PLAYBACK.name)
            putExtra(NotificationNavigation.NOTIFICATION_ACTION_EXTRA, "OPEN_PLAYER")
            putExtra(NotificationNavigation.CONTENT_ID_EXTRA, stationId)
            putExtra(NotificationNavigation.NOTIFICATION_TITLE_EXTRA, if (isPlaying) "Now Playing" else "Paused")
            putExtra(NotificationNavigation.NOTIFICATION_MESSAGE_EXTRA, stationName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    }

    /**
     * Creates a PendingIntent for the notification.
     */
    private fun createPendingIntent(intent: Intent): PendingIntent {
        return PendingIntent.getActivity(
            context,
            PLAYBACK_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /**
     * Shows the notification.
     * @param notification The notification to show
     */
    fun showNotification(notification: Notification) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(PLAYBACK_NOTIFICATION_ID, notification)
    }

    /**
     * Cancels the playback notification.
     */
    fun cancelNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(PLAYBACK_NOTIFICATION_ID)
    }
}
