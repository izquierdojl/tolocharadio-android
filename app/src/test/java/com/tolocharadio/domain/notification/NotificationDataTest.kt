package com.tolocharadio.domain.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDataTest {
    @Test
    fun `valid notification data should be valid`() {
        val notificationData =
            NotificationData(
                type = NotificationType.PLAYBACK,
                title = "Now Playing",
                message = "Radio Tolocha",
                contentId = "station_123",
                action = NotificationAction.OPEN_PLAYER,
                timestamp = System.currentTimeMillis(),
            )

        assertTrue(notificationData.isValid())
    }

    @Test
    fun `notification data with empty title should be invalid`() {
        val notificationData =
            NotificationData(
                type = NotificationType.PLAYBACK,
                title = "",
                message = "Radio Tolocha",
                contentId = "station_123",
                action = NotificationAction.OPEN_PLAYER,
                timestamp = System.currentTimeMillis(),
            )

        assertFalse(notificationData.isValid())
    }

    @Test
    fun `notification data with empty message should be invalid`() {
        val notificationData =
            NotificationData(
                type = NotificationType.PLAYBACK,
                title = "Now Playing",
                message = "",
                contentId = "station_123",
                action = NotificationAction.OPEN_PLAYER,
                timestamp = System.currentTimeMillis(),
            )

        assertFalse(notificationData.isValid())
    }

    @Test
    fun `notification data with null contentId should be valid`() {
        val notificationData =
            NotificationData(
                type = NotificationType.PLAYBACK,
                title = "Now Playing",
                message = "Radio Tolocha",
                contentId = null,
                action = NotificationAction.OPEN_PLAYER,
                timestamp = System.currentTimeMillis(),
            )

        assertTrue(notificationData.isValid())
    }

    @Test
    fun `notification data with empty contentId should be invalid`() {
        val notificationData =
            NotificationData(
                type = NotificationType.PLAYBACK,
                title = "Now Playing",
                message = "Radio Tolocha",
                contentId = "",
                action = NotificationAction.OPEN_PLAYER,
                timestamp = System.currentTimeMillis(),
            )

        assertFalse(notificationData.isValid())
    }

    @Test
    fun `notification data with negative timestamp should be invalid`() {
        val notificationData =
            NotificationData(
                type = NotificationType.PLAYBACK,
                title = "Now Playing",
                message = "Radio Tolocha",
                contentId = "station_123",
                action = NotificationAction.OPEN_PLAYER,
                timestamp = -1,
            )

        assertFalse(notificationData.isValid())
    }

    @Test
    fun `notification data with zero timestamp should be invalid`() {
        val notificationData =
            NotificationData(
                type = NotificationType.PLAYBACK,
                title = "Now Playing",
                message = "Radio Tolocha",
                contentId = "station_123",
                action = NotificationAction.OPEN_PLAYER,
                timestamp = 0,
            )

        assertFalse(notificationData.isValid())
    }
}
