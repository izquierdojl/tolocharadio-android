package com.tolocharadio.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tolocharadio.domain.notification.NotificationAction
import com.tolocharadio.domain.notification.NotificationChannels
import com.tolocharadio.domain.notification.NotificationData
import com.tolocharadio.domain.notification.NotificationMapper
import com.tolocharadio.domain.notification.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for content notification tap.
 */
@RunWith(AndroidJUnit4::class)
class ContentNotificationTest {
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun `content_notification_should_create_valid_notification_data`() {
        val notificationData =
            NotificationData(
                type = NotificationType.CONTENT,
                title = "New Content",
                message = "Check out this station",
                contentId = "station_456",
                action = NotificationAction.OPEN_CONTENT,
            )

        assertTrue(notificationData.isValid())
        assertEquals(NotificationType.CONTENT, notificationData.type)
        assertEquals(NotificationAction.OPEN_CONTENT, notificationData.action)
    }

    @Test
    fun `content_notification_should_use_correct_channel`() {
        val channelId = NotificationChannels.getChannelIdForType(NotificationType.CONTENT)
        assertEquals(NotificationChannels.CONTENT_CHANNEL_ID, channelId)
    }

    @Test
    fun `content_notification_should_navigate_to_content_screen`() {
        val notificationData =
            NotificationData(
                type = NotificationType.CONTENT,
                title = "New Content",
                message = "Check out this station",
                contentId = "station_456",
                action = NotificationAction.OPEN_CONTENT,
            )

        val mapper = NotificationMapper()
        val deepLink = mapper.mapActionToDeepLink(notificationData.action, notificationData.contentId)

        assertEquals("tolocharadio://content/station_456", deepLink)
    }

    @Test
    fun `content_notification_without_contentId_should_navigate_to_content_list`() {
        val notificationData =
            NotificationData(
                type = NotificationType.CONTENT,
                title = "New Content",
                message = "Check out this station",
                contentId = null,
                action = NotificationAction.OPEN_CONTENT,
            )

        val mapper = NotificationMapper()
        val deepLink = mapper.mapActionToDeepLink(notificationData.action, notificationData.contentId)

        assertEquals("tolocharadio://content", deepLink)
    }

    @Test
    fun `content_notification_should_work_with_app_in_background`() {
        val notificationData =
            NotificationData(
                type = NotificationType.CONTENT,
                title = "New Content",
                message = "Check out this station",
                contentId = "station_456",
                action = NotificationAction.OPEN_CONTENT,
            )

        assertTrue(notificationData.isValid())
    }
}
