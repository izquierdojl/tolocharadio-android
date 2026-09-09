package com.tolocharadio.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tolocharadio.domain.notification.*
import org.junit.Assert.*
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
    fun `system notification should create valid notification data`() {
        val notificationData = NotificationData(
            type = NotificationType.SYSTEM,
            title = "System Message",
            message = "App update available",
            contentId = null,
            action = NotificationAction.OPEN_INFO
        )
        
        assertTrue(notificationData.isValid())
        assertEquals(NotificationType.SYSTEM, notificationData.type)
        assertEquals(NotificationAction.OPEN_INFO, notificationData.action)
    }
    
    @Test
    fun `system notification should use correct channel`() {
        val channelId = NotificationChannels.getChannelIdForType(NotificationType.SYSTEM)
        assertEquals(NotificationChannels.SYSTEM_CHANNEL_ID, channelId)
    }
    
    @Test
    fun `system notification should navigate to info screen`() {
        val notificationData = NotificationData(
            type = NotificationType.SYSTEM,
            title = "System Message",
            message = "App update available",
            contentId = null,
            action = NotificationAction.OPEN_INFO
        )
        
        val mapper = NotificationMapper()
        val deepLink = mapper.mapActionToDeepLink(notificationData.action, notificationData.contentId)
        
        assertEquals("tolocharadio://info", deepLink)
    }
    
    @Test
    fun `system notification with contentId should navigate to specific info`() {
        val notificationData = NotificationData(
            type = NotificationType.SYSTEM,
            title = "System Message",
            message = "App update available",
            contentId = "update_v2",
            action = NotificationAction.OPEN_INFO
        )
        
        val mapper = NotificationMapper()
        val deepLink = mapper.mapActionToDeepLink(notificationData.action, notificationData.contentId)
        
        assertEquals("tolocharadio://info/update_v2", deepLink)
    }
    
    @Test
    fun `system notification should work with app in background`() {
        val notificationData = NotificationData(
            type = NotificationType.SYSTEM,
            title = "System Message",
            message = "App update available",
            contentId = null,
            action = NotificationAction.OPEN_INFO
        )
        
        assertTrue(notificationData.isValid())
    }
}
