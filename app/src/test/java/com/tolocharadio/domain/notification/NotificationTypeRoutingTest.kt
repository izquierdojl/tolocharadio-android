package com.tolocharadio.domain.notification

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NotificationTypeRoutingTest {
    
    private lateinit var router: NotificationTypeRouter
    
    @Before
    fun setUp() {
        router = NotificationTypeRouter()
    }
    
    @Test
    fun `playback notification should route to player screen`() {
        val action = router.routeNotification(NotificationType.PLAYBACK, "station_123")
        assertEquals(NotificationAction.OPEN_PLAYER, action)
    }
    
    @Test
    fun `content notification should route to content screen`() {
        val action = router.routeNotification(NotificationType.CONTENT, "content_456")
        assertEquals(NotificationAction.OPEN_CONTENT, action)
    }
    
    @Test
    fun `system notification should route to info screen`() {
        val action = router.routeNotification(NotificationType.SYSTEM, null)
        assertEquals(NotificationAction.OPEN_INFO, action)
    }
    
    @Test
    fun `system notification with contentId should route to info screen`() {
        val action = router.routeNotification(NotificationType.SYSTEM, "info_789")
        assertEquals(NotificationAction.OPEN_INFO, action)
    }
    
    @Test
    fun `notification with invalid type should route to main screen`() {
        val action = router.routeNotification(NotificationType.PLAYBACK, null)
        assertEquals(NotificationAction.OPEN_PLAYER, action)
    }
    
    @Test
    fun `notification without contentId should use default routing`() {
        val action = router.routeNotification(NotificationType.CONTENT, null)
        assertEquals(NotificationAction.OPEN_CONTENT, action)
    }
}
