package com.tolocharadio.domain.notification

/**
 * Represents the current state of the application.
 */
enum class AppState {
    /** App is in foreground and visible */
    FOREGROUND,

    /** App is in background (may be playing audio) */
    BACKGROUND,

    /** App is not running (process killed or force-stopped) */
    NOT_RUNNING,
}
