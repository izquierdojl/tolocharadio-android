package com.tolocharadio.domain.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles notification tap events with security and edge case handling.
 */
@Singleton
class NotificationHandler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val lockScreenHandler = LockScreenHandler(context)
        private val forceStopHandler = ForceStopHandler(context)
        private val unavailableContentHandler = UnavailableContentHandler()
        private val notificationValidator = NotificationValidator()

        /**
         * Handles a notification tap event.
         * @param notificationData The notification data
         * @param appState The current app state
         * @return The action to perform based on the notification and app state
         */
        suspend fun handleNotificationTap(
            notificationData: NotificationData,
            appState: AppState,
        ): NotificationAction {
            // Validate the notification data
            if (!notificationValidator.validate(notificationData)) {
                return NotificationAction.OPEN_MAIN
            }

            // Check if the notification can be shown
            if (!canShowNotification(notificationData)) {
                return NotificationAction.OPEN_MAIN
            }

            // Check if device is locked
            if (lockScreenHandler.shouldRequireUnlock(notificationData)) {
                // Device is locked, require unlock
                // The actual unlock handling will be done at the UI level
                // For now, we return the appropriate action
            }

            // Check if content is available
            if (!unavailableContentHandler.isContentAvailable(notificationData.contentId)) {
                return unavailableContentHandler.handleUnavailableContent(
                    notificationData.contentId,
                    notificationData.type,
                )
            }

            // Handle based on app state
            return when (appState) {
                AppState.FOREGROUND -> {
                    // In foreground, use the notification's action
                    notificationData.action
                }
                AppState.BACKGROUND -> {
                    // In background, use the notification's action to bring app to foreground
                    notificationData.action
                }
                AppState.NOT_RUNNING -> {
                    // Not running, launch to main screen
                    NotificationAction.OPEN_MAIN
                }
            }
        }

        /**
         * Validates if a notification can be shown.
         * @param notificationData The notification data to validate
         * @return true if the notification can be shown, false otherwise
         */
        fun canShowNotification(notificationData: NotificationData): Boolean {
            return notificationValidator.validate(notificationData) &&
                notificationValidator.validatePublicInfoOnly(notificationData)
        }

        /**
         * Gets the lock screen handler.
         * @return The lock screen handler
         */
        fun getLockScreenHandler(): LockScreenHandler {
            return lockScreenHandler
        }

        /**
         * Gets the force stop handler.
         * @return The force stop handler
         */
        fun getForceStopHandler(): ForceStopHandler {
            return forceStopHandler
        }

        /**
         * Gets the unavailable content handler.
         * @return The unavailable content handler
         */
        fun getUnavailableContentHandler(): UnavailableContentHandler {
            return unavailableContentHandler
        }
    }
