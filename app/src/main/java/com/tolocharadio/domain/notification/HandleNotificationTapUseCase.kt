package com.tolocharadio.domain.notification

import javax.inject.Inject

/**
 * Use case for handling notification tap events.
 */
class HandleNotificationTapUseCase
    @Inject
    constructor(
        private val notificationHandler: NotificationHandler,
        private val appStateTracker: AppStateTracker,
        private val notificationValidator: NotificationValidator,
    ) {
        /**
         * Handles a notification tap event.
         * @param notificationData The notification data
         * @return The action to perform based on the notification and app state
         */
        suspend operator fun invoke(notificationData: NotificationData): NotificationAction {
            // Validate the notification data
            if (!notificationValidator.validate(notificationData)) {
                return NotificationAction.OPEN_MAIN
            }

            // Check if the notification can be shown
            if (!notificationHandler.canShowNotification(notificationData)) {
                return NotificationAction.OPEN_MAIN
            }

            // Get the current app state
            val appState = appStateTracker.currentState.value

            // Handle the notification tap
            return notificationHandler.handleNotificationTap(notificationData, appState)
        }
    }
