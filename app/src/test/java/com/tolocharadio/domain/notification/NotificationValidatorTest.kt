package com.tolocharadio.domain.notification

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
