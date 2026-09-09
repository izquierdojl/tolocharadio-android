package com.tolocharadio.ui.notification

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import com.tolocharadio.domain.notification.NotificationAction

/**
 * Handles navigation from notifications to the appropriate screens.
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
     * Creates an intent for handling notification taps.
     * @param context The context
     * @param notificationType The notification type
     * @param notificationAction The notification action
     * @param contentId Optional content ID
     * @param title The notification title
     * @param message The notification message
     * @return The intent for handling the notification tap
     */
    fun createNotificationTapIntent(
        context: Context,
        notificationType: String,
        notificationAction: String,
        contentId: String? = null,
        title: String,
        message: String
    ): Intent {
        return Intent(context, com.izquierdojl.tolocharadio.MainActivity::class.java).apply {
            action = NOTIFICATION_TAP_ACTION
            putExtra(NOTIFICATION_TYPE_EXTRA, notificationType)
            putExtra(NOTIFICATION_ACTION_EXTRA, notificationAction)
            contentId?.let { putExtra(CONTENT_ID_EXTRA, it) }
            putExtra(NOTIFICATION_TITLE_EXTRA, title)
            putExtra(NOTIFICATION_MESSAGE_EXTRA, message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    }
    
    /**
     * Navigates to the appropriate screen based on the notification action.
     * @param navController The navigation controller
     * @param action The notification action
     * @param contentId Optional content ID
     */
    fun navigateToScreen(
        navController: NavController,
        action: NotificationAction,
        contentId: String? = null
    ) {
        when (action) {
            NotificationAction.OPEN_PLAYER -> {
                if (contentId != null) {
                    navController.navigate("player/$contentId")
                } else {
                    navController.navigate("player")
                }
            }
            NotificationAction.OPEN_CONTENT -> {
                if (contentId != null) {
                    navController.navigate("content/$contentId")
                } else {
                    navController.navigate("content")
                }
            }
            NotificationAction.OPEN_INFO -> {
                if (contentId != null) {
                    navController.navigate("info/$contentId")
                } else {
                    navController.navigate("info")
                }
            }
            NotificationAction.OPEN_MAIN -> {
                navController.navigate("main") {
                    popUpTo("main") { inclusive = true }
                }
            }
        }
    }
    
    /**
     * Navigates to player screen with focus on current playback.
     * @param navController The navigation controller
     * @param stationId The station ID to focus on
     */
    fun navigateToPlayerWithFocus(
        navController: NavController,
        stationId: String
    ) {
        navController.navigate("player/$stationId") {
            popUpTo("player") { inclusive = true }
        }
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
                action = notificationAction
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
