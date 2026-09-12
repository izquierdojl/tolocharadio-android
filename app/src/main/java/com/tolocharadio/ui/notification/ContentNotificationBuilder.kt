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
 * Builds content notifications.
 */
class ContentNotificationBuilder(
    private val context: Context,
) {
    companion object {
        const val CONTENT_NOTIFICATION_ID = 1002
    }

    /**
     * Builds a content notification.
     * @param contentId The content ID
     * @param contentTitle The content title
     * @param contentMessage The content message
     * @return The notification
     */
    fun buildNotification(
        contentId: String,
        contentTitle: String,
        contentMessage: String,
    ): Notification {
        val intent = createNotificationTapIntent(contentId, contentTitle, contentMessage)
        val pendingIntent = createPendingIntent(intent)

        return NotificationCompat.Builder(context, NotificationChannels.CONTENT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(contentTitle)
            .setContentText(contentMessage)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    /**
     * Creates an intent for notification tap.
     */
    private fun createNotificationTapIntent(
        contentId: String,
        contentTitle: String,
        contentMessage: String,
    ): Intent {
        return Intent(context, com.izquierdojl.tolocharadio.MainActivity::class.java).apply {
            action = NotificationNavigation.NOTIFICATION_TAP_ACTION
            putExtra(NotificationNavigation.NOTIFICATION_TYPE_EXTRA, NotificationType.CONTENT.name)
            putExtra(NotificationNavigation.NOTIFICATION_ACTION_EXTRA, "OPEN_CONTENT")
            putExtra(NotificationNavigation.CONTENT_ID_EXTRA, contentId)
            putExtra(NotificationNavigation.NOTIFICATION_TITLE_EXTRA, contentTitle)
            putExtra(NotificationNavigation.NOTIFICATION_MESSAGE_EXTRA, contentMessage)
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
            CONTENT_NOTIFICATION_ID,
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
        notificationManager.notify(CONTENT_NOTIFICATION_ID, notification)
    }

    /**
     * Cancels the content notification.
     */
    fun cancelNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(CONTENT_NOTIFICATION_ID)
    }
}
