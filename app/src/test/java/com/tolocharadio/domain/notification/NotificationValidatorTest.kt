package com.tolocharadio.domain.notification

import android.app.NotificationManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationValidatorTest {
    private lateinit var validator: NotificationValidator

    @Before
    fun setUp() {
        validator = NotificationValidator()
    }

    @Test
    fun `valid notification data should pass validation`() {
        val notificationData =
            NotificationData(
                type = NotificationType.PLAYBACK,
                title = "Now Playing",
                message = "Radio Tolocha",
                contentId = "station_123",
                action = NotificationAction.OPEN_PLAYER,
                timestamp = System.currentTimeMillis(),
            )

        assertTrue(validator.validate(notificationData))
    }

    @Test
    fun `invalid notification data should fail validation`() {
        val notificationData =
            NotificationData(
                type = NotificationType.PLAYBACK,
                title = "",
                message = "Radio Tolocha",
                contentId = "station_123",
                action = NotificationAction.OPEN_PLAYER,
                timestamp = System.currentTimeMillis(),
            )

        assertFalse(validator.validate(notificationData))
    }

    @Test
    fun `valid channel config should pass validation`() {
        val channelConfig =
            NotificationChannelConfig(
                id = "test_channel",
                name = "Test Channel",
                description = "Test channel description",
                importance = NotificationManager.IMPORTANCE_DEFAULT,
            )

        assertTrue(validator.validateChannelConfig(channelConfig))
    }

    @Test
    fun `channel config with empty id should fail validation`() {
        val channelConfig =
            NotificationChannelConfig(
                id = "",
                name = "Test Channel",
                description = "Test channel description",
                importance = NotificationManager.IMPORTANCE_DEFAULT,
            )

        assertFalse(validator.validateChannelConfig(channelConfig))
    }

    @Test
    fun `channel config with invalid importance should fail validation`() {
        val channelConfig =
            NotificationChannelConfig(
                id = "test_channel",
                name = "Test Channel",
                description = "Test channel description",
                importance = -1,
            )

        assertFalse(validator.validateChannelConfig(channelConfig))
    }

    @Test
    fun `notification with public info should pass validation`() {
        val notificationData =
            NotificationData(
                type = NotificationType.PLAYBACK,
                title = "Now Playing",
                message = "Radio Tolocha",
                contentId = "station_123",
                action = NotificationAction.OPEN_PLAYER,
                timestamp = System.currentTimeMillis(),
            )

        assertTrue(validator.validatePublicInfoOnly(notificationData))
    }

    @Test
    fun `playback notification should have open player action`() {
        assertTrue(validator.validateActionForType(NotificationType.PLAYBACK, NotificationAction.OPEN_PLAYER))
    }

    @Test
    fun `playback notification should not have open content action`() {
        assertFalse(validator.validateActionForType(NotificationType.PLAYBACK, NotificationAction.OPEN_CONTENT))
    }

    @Test
    fun `content notification should have open content action`() {
        assertTrue(validator.validateActionForType(NotificationType.CONTENT, NotificationAction.OPEN_CONTENT))
    }

    @Test
    fun `system notification should have open info action`() {
        assertTrue(validator.validateActionForType(NotificationType.SYSTEM, NotificationAction.OPEN_INFO))
    }

    @Test
    fun `system notification should have open main action`() {
        assertTrue(validator.validateActionForType(NotificationType.SYSTEM, NotificationAction.OPEN_MAIN))
    }
}
