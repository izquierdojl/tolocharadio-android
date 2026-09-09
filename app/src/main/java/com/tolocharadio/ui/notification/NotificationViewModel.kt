package com.tolocharadio.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolocharadio.domain.notification.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for handling notification tap events.
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val handleNotificationTapUseCase: HandleNotificationTapUseCase,
    private val notificationMapper: NotificationMapper,
    private val appStateTracker: AppStateTracker
) : ViewModel() {
    
    private val _notificationState = MutableStateFlow<NotificationState>(NotificationState.Idle)
    val notificationState: StateFlow<NotificationState> = _notificationState.asStateFlow()
    
    /**
     * Handles a notification tap event.
     * @param notificationData The notification data
     */
    fun handleNotificationTap(notificationData: NotificationData) {
        viewModelScope.launch {
            _notificationState.value = NotificationState.Processing
            
            try {
                val action = handleNotificationTapUseCase(notificationData)
                val deepLink = notificationMapper.mapActionToDeepLink(action, notificationData.contentId)
                
                _notificationState.value = NotificationState.Navigate(deepLink, action)
            } catch (e: Exception) {
                _notificationState.value = NotificationState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Resets the notification state to idle.
     */
    fun resetState() {
        _notificationState.value = NotificationState.Idle
    }
    
    /**
     * Updates the app state.
     * @param state The new app state
     */
    fun updateAppState(state: AppState) {
        appStateTracker.updateState(state)
    }
}

/**
 * Represents the state of the notification handling.
 */
sealed class NotificationState {
    /** Idle state - no notification being processed */
    object Idle : NotificationState()
    
    /** Processing state - notification is being handled */
    object Processing : NotificationState()
    
    /** Navigate state - ready to navigate to the target screen */
    data class Navigate(val deepLink: String, val action: NotificationAction) : NotificationState()
    
    /** Error state - an error occurred while handling the notification */
    data class Error(val message: String) : NotificationState()
}
