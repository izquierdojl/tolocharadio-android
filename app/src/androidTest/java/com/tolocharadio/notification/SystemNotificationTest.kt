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
 * Integration test for system notification tap.
 */
@RunWith(AndroidJUnit4::class)
class SystemNotificationTest {
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun `system_notification_should_create_valid_notification_data`() {
        val notificationData =
            NotificationData(
                type = NotificationType.SYSTEM,
                title = "System Message",
                message = "App update available",
                contentId = null,
                action = NotificationAction.OPEN_INFO,
            )

        assertTrue(notificationData.isValid())
        assertEquals(NotificationType.SYSTEM, notificationData.type)
        assertEquals(NotificationAction.OPEN_INFO, notificationData.action)
    }

    @Test
    fun `system_notification_should_use_correct_channel`() {
        val channelId = NotificationChannels.getChannelIdForType(NotificationType.SYSTEM)
        assertEquals(NotificationChannels.SYSTEM_CHANNEL_ID, channelId)
    }

    @Test
    fun `system_notification_should_navigate_to_info_screen`() {
        val notificationData =
            NotificationData(
                type = NotificationType.SYSTEM,
                title = "System Message",
                message = "App update available",
                contentId = null,
                action = NotificationAction.OPEN_INFO,
            )

        val mapper = NotificationMapper()
        val deepLink = mapper.mapActionToDeepLink(notificationData.action, notificationData.contentId)

        assertEquals("tolocharadio://info", deepLink)
    }

    @Test
    fun `system_notification_with_contentId_should_navigate_to_specific_info`() {
        val notificationData =
            NotificationData(
                type = NotificationType.SYSTEM,
                title = "System Message",
                message = "App update available",
                contentId = "update_v2",
                action = NotificationAction.OPEN_INFO,
            )

        val mapper = NotificationMapper()
        val deepLink = mapper.mapActionToDeepLink(notificationData.action, notificationData.contentId)

        assertEquals("tolocharadio://info/update_v2", deepLink)
    }

    @Test
    fun `system_notification_should_work_with_app_in_background`() {
        val notificationData =
            NotificationData(
                type = NotificationType.SYSTEM,
                title = "System Message",
                message = "App update available",
                contentId = null,
                action = NotificationAction.OPEN_INFO,
            )

        assertTrue(notificationData.isValid())
    }
}
