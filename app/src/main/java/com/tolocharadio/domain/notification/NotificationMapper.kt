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
    }
