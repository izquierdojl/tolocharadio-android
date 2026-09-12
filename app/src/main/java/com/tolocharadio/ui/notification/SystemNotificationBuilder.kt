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
 * Builds system notifications.
 */
class SystemNotificationBuilder(
    private val context: Context,
) {
    companion object {
        const val SYSTEM_NOTIFICATION_ID = 1003
    }

    /**
     * Builds a system notification.
     * @param title The notification title
     * @param message The notification message
     * @param contentId Optional content ID
     * @return The notification
     */
    fun buildNotification(
        title: String,
        message: String,
        contentId: String? = null,
    ): Notification {
        val intent = createNotificationTapIntent(title, message, contentId)
        val pendingIntent = createPendingIntent(intent)

        return NotificationCompat.Builder(context, NotificationChannels.SYSTEM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .build()
    }

    /**
     * Creates an intent for notification tap.
     */
    private fun createNotificationTapIntent(
        title: String,
        message: String,
        contentId: String?,
    ): Intent {
        return Intent(context, com.izquierdojl.tolocharadio.MainActivity::class.java).apply {
            action = NotificationNavigation.NOTIFICATION_TAP_ACTION
            putExtra(NotificationNavigation.NOTIFICATION_TYPE_EXTRA, NotificationType.SYSTEM.name)
            putExtra(NotificationNavigation.NOTIFICATION_ACTION_EXTRA, "OPEN_INFO")
            contentId?.let { putExtra(NotificationNavigation.CONTENT_ID_EXTRA, it) }
            putExtra(NotificationNavigation.NOTIFICATION_TITLE_EXTRA, title)
            putExtra(NotificationNavigation.NOTIFICATION_MESSAGE_EXTRA, message)
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
            SYSTEM_NOTIFICATION_ID,
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
        notificationManager.notify(SYSTEM_NOTIFICATION_ID, notification)
    }

    /**
     * Cancels the system notification.
     */
    fun cancelNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(SYSTEM_NOTIFICATION_ID)
    }
}
