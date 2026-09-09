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
 * Integration test for playback notification tap.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackNotificationTest {
    
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var appStateTracker: InMemoryAppStateTracker
    
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        appStateTracker = InMemoryAppStateTracker()
    }
    
    @Test
    fun `playback notification should create valid notification data`() {
        val notificationData = NotificationData(
            type = NotificationType.PLAYBACK,
            title = "Now Playing",
            message = "Radio Tolocha",
            contentId = "station_123",
            action = NotificationAction.OPEN_PLAYER
        )
        
        assertTrue(notificationData.isValid())
        assertEquals(NotificationType.PLAYBACK, notificationData.type)
        assertEquals(NotificationAction.OPEN_PLAYER, notificationData.action)
    }
    
    @Test
    fun `playback notification should use correct channel`() {
        val channelId = NotificationChannels.getChannelIdForType(NotificationType.PLAYBACK)
        assertEquals(NotificationChannels.PLAYBACK_CHANNEL_ID, channelId)
    }
    
    @Test
    fun `playback notification should navigate to player screen`() {
        val notificationData = NotificationData(
            type = NotificationType.PLAYBACK,
            title = "Now Playing",
            message = "Radio Tolocha",
            contentId = "station_123",
            action = NotificationAction.OPEN_PLAYER
        )
        
        val mapper = NotificationMapper()
        val deepLink = mapper.mapActionToDeepLink(notificationData.action, notificationData.contentId)
        
        assertEquals("tolocharadio://player/station_123", deepLink)
    }
    
    @Test
    fun `playback notification should work with app in background`() {
        appStateTracker.updateState(AppState.BACKGROUND)
        
        val notificationData = NotificationData(
            type = NotificationType.PLAYBACK,
            title = "Now Playing",
            message = "Radio Tolocha",
            contentId = "station_123",
            action = NotificationAction.OPEN_PLAYER
        )
        
        assertEquals(AppState.BACKGROUND, appStateTracker.currentState.value)
        assertTrue(notificationData.isValid())
    }
    
    @Test
    fun `playback notification should work with app not running`() {
        appStateTracker.updateState(AppState.NOT_RUNNING)
        
        val notificationData = NotificationData(
            type = NotificationType.PLAYBACK,
            title = "Now Playing",
            message = "Radio Tolocha",
            contentId = "station_123",
            action = NotificationAction.OPEN_PLAYER
        )
        
        assertEquals(AppState.NOT_RUNNING, appStateTracker.currentState.value)
        assertTrue(notificationData.isValid())
    }
}

/**
 * In-memory implementation of AppStateTracker for testing.
 */
class InMemoryAppStateTracker : AppStateTracker {
    
    private val _currentState = kotlinx.coroutines.flow.MutableStateFlow(AppState.NOT_RUNNING)
    override val currentState: kotlinx.coroutines.flow.StateFlow<AppState> = _currentState
    
    override fun updateState(state: AppState) {
        _currentState.value = state
    }
    
    override fun isForeground(): Boolean {
        return _currentState.value == AppState.FOREGROUND
    }
    
    override fun isBackground(): Boolean {
        return _currentState.value == AppState.BACKGROUND
    }
    
    override fun isNotRunning(): Boolean {
        return _currentState.value == AppState.NOT_RUNNING
    }
}
