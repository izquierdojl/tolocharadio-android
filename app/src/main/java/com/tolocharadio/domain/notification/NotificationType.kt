package com.tolocharadio.domain.notification

/**
 * Enum representing the type of notification.
 */
enum class NotificationType {
    /** Playback status notifications (currently playing, playback controls) */
    PLAYBACK,

    /** New content notifications (station updates, recommendations) */
    CONTENT,

    /** System messages (maintenance, updates, alerts) */
    SYSTEM,
}
