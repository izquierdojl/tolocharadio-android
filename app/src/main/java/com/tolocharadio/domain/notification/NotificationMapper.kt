package com.tolocharadio.domain.notification

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps between notification data and different representations.
 */
@Singleton
class NotificationMapper
    @Inject
    constructor() {
        /**
         * Maps a notification type to its corresponding channel ID.
         */
        fun mapTypeToChannelId(type: NotificationType): String {
            return NotificationChannels.getChannelIdForType(type)
        }

        /**
         * Maps a notification action to its corresponding deep link pattern.
         */
        fun mapActionToDeepLink(
            action: NotificationAction,
            contentId: String? = null,
        ): String {
            return when (action) {
                NotificationAction.OPEN_PLAYER -> {
                    if (contentId != null) {
                        "tolocharadio://player/$contentId"
                    } else {
                        "tolocharadio://player"
                    }
                }
                NotificationAction.OPEN_CONTENT -> {
                    if (contentId != null) {
                        "tolocharadio://content/$contentId"
                    } else {
                        "tolocharadio://content"
                    }
                }
                NotificationAction.OPEN_INFO -> {
                    if (contentId != null) {
                        "tolocharadio://info/$contentId"
                    } else {
                        "tolocharadio://info"
                    }
                }
                NotificationAction.OPEN_MAIN -> "tolocharadio://main"
            }
        }

        /**
         * Maps a notification type to its default action.
         */
        fun mapTypeToDefaultAction(type: NotificationType): NotificationAction {
            return when (type) {
                NotificationType.PLAYBACK -> NotificationAction.OPEN_PLAYER
                NotificationType.CONTENT -> NotificationAction.OPEN_CONTENT
                NotificationType.SYSTEM -> NotificationAction.OPEN_INFO
            }
        }

        /**
         * Validates that a notification data object is complete and valid.
         */
        fun validateNotificationData(notificationData: NotificationData): Boolean {
            return notificationData.isValid() &&
                notificationData.action == mapTypeToDefaultAction(notificationData.type) ||
                notificationData.action == NotificationAction.OPEN_MAIN
        }
    }
