package com.tolocharadio.domain.notification

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BackgroundStateTest {
    
    private lateinit var backgroundStateHandler: BackgroundStateHandler
    
    @Before
    fun setUp() {
        backgroundStateHandler = BackgroundStateHandler()
    }
    
    @Test
    fun `should handle foreground state correctly`() {
        val appState = AppState.FOREGROUND
        val notificationData = NotificationData(
            type = NotificationType.PLAYBACK,
            title = "Now Playing",
            message = "Radio Tolocha",
            action = NotificationAction.OPEN_PLAYER
        )
        
        val action = backgroundStateHandler.determineAction(appState, notificationData)
        assertEquals(NotificationAction.OPEN_PLAYER, action)
    }
    
    @Test
    fun `should handle background state correctly`() {
        val appState = AppState.BACKGROUND
        val notificationData = NotificationData(
            type = NotificationType.PLAYBACK,
            title = "Now Playing",
            message = "Radio Tolocha",
            action = NotificationAction.OPEN_PLAYER
        )
        
        val action = backgroundStateHandler.determineAction(appState, notificationData)
        assertEquals(NotificationAction.OPEN_PLAYER, action)
    }
    
    @Test
    fun `should handle not running state correctly`() {
        val appState = AppState.NOT_RUNNING
        val notificationData = NotificationData(
            type = NotificationType.PLAYBACK,
            title = "Now Playing",
            message = "Radio Tolocha",
            action = NotificationAction.OPEN_PLAYER
        )
        
        val action = backgroundStateHandler.determineAction(appState, notificationData)
        assertEquals(NotificationAction.OPEN_MAIN, action)
    }
    
    @Test
    fun `should prioritize notification action over default`() {
        val appState = AppState.FOREGROUND
        val notificationData = NotificationData(
            type = NotificationType.CONTENT,
            title = "New Content",
            message = "Check out this station",
            contentId = "station_456",
            action = NotificationAction.OPEN_CONTENT
        )
        
        val action = backgroundStateHandler.determineAction(appState, notificationData)
        assertEquals(NotificationAction.OPEN_CONTENT, action)
    }
}
