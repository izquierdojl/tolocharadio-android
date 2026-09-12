package com.tolocharadio.domain.notification

import android.app.NotificationManager

/**
 * Configuration for a notification channel.
 */
data class NotificationChannelConfig(
    /** Unique channel identifier */
    val id: String,
    /** User-visible channel name */
    val name: String,
    /** User-visible channel description */
    val description: String,
    /** Notification importance level (e.g., NotificationManager.IMPORTANCE_LOW) */
    val importance: Int,
) {
    /**
     * Validates the channel configuration.
     * @return true if the configuration is valid, false otherwise
     */
    fun isValid(): Boolean {
        return id.isNotBlank() &&
            name.isNotBlank() &&
            description.isNotBlank() &&
            importance in NotificationManager.IMPORTANCE_MIN..NotificationManager.IMPORTANCE_MAX
    }
}
