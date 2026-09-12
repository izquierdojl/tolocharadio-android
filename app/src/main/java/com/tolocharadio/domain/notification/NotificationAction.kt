package com.tolocharadio.domain.notification

/**
 * Enum representing the action to perform when a notification is tapped.
 */
enum class NotificationAction {
    /** Navigate to the player screen */
    OPEN_PLAYER,

    /** Navigate to specific content screen */
    OPEN_CONTENT,

    /** Navigate to information screen */
    OPEN_INFO,

    /** Navigate to main screen */
    OPEN_MAIN,
}
