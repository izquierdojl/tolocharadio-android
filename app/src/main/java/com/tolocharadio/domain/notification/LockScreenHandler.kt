package com.tolocharadio.domain.notification

import android.app.KeyguardManager
import android.content.Context

/**
 * Handles lock screen detection and security requirements.
 */
class LockScreenHandler(
    private val context: Context
) {
    
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    
    /**
     * Checks if the device is locked.
     * @return true if the device is locked, false otherwise
     */
    fun isDeviceLocked(): Boolean {
        return keyguardManager.isKeyguardLocked
    }
    
    /**
     * Checks if the notification should require device unlock.
     * @param notificationData The notification data
     * @return true if the notification should require device unlock, false otherwise
     */
    fun shouldRequireUnlock(notificationData: NotificationData): Boolean {
        // All notifications require unlock on locked devices
        // except for playback notifications when audio is playing
        return isDeviceLocked()
    }
    
    /**
     * Checks if the notification can bypass lock screen.
     * @param notificationData The notification data
     * @return true if the notification can bypass lock screen, false otherwise
     */
    fun canBypassLockScreen(notificationData: NotificationData): Boolean {
        // Playback notifications can bypass lock screen when audio is playing
        // This is handled at a higher level by checking if playback is active
        return false
    }
}
