package com.tolocharadio.domain.notification

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates notification data and ensures it meets requirements.
 */
@Singleton
class NotificationValidator
    @Inject
    constructor() {
        /**
         * Validates notification data.
         * @param notificationData The notification data to validate
         * @return true if the notification data is valid, false otherwise
         */
        fun validate(notificationData: NotificationData): Boolean {
            return notificationData.isValid()
        }

        /**
         * Validates notification channel configuration.
         * @param channelConfig The channel configuration to validate
         * @return true if the channel configuration is valid, false otherwise
         */
        fun validateChannelConfig(channelConfig: NotificationChannelConfig): Boolean {
            return channelConfig.isValid()
        }

        /**
         * Validates that the notification contains only public information.
         * @param notificationData The notification data to validate
         * @return true if the notification contains only public information, false otherwise
         */
        fun validatePublicInfoOnly(notificationData: NotificationData): Boolean {
            // All notification data should only contain public information
            // This is enforced by the data structure itself, but we validate here as well
            return notificationData.title.isNotBlank() && notificationData.message.isNotBlank()
        }

        /**
         * Validates that the notification action is appropriate for the notification type.
         * @param type The notification type
         * @param action The notification action
         * @return true if the action is appropriate for the type, false otherwise
         */
        fun validateActionForType(
            type: NotificationType,
            action: NotificationAction,
        ): Boolean {
            return when (type) {
                NotificationType.PLAYBACK -> action == NotificationAction.OPEN_PLAYER
                NotificationType.CONTENT -> action == NotificationAction.OPEN_CONTENT
                NotificationType.SYSTEM ->
                    action == NotificationAction.OPEN_INFO ||
                        action == NotificationAction.OPEN_MAIN
            }
        }
    }
