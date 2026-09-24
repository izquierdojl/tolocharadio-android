package com.tolocharadio.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolocharadio.domain.notification.AppState
import com.tolocharadio.domain.notification.AppStateTracker
import com.tolocharadio.domain.notification.HandleNotificationTapUseCase
import com.tolocharadio.domain.notification.NotificationAction
import com.tolocharadio.domain.notification.NotificationData
import com.tolocharadio.domain.notification.NotificationLogger
import com.tolocharadio.domain.notification.NotificationMapper
import com.tolocharadio.domain.notification.NotificationPerformanceMonitor
import com.tolocharadio.domain.notification.NotificationType
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
class NotificationViewModel
    @Inject
    constructor(
        private val handleNotificationTapUseCase: HandleNotificationTapUseCase,
        private val notificationMapper: NotificationMapper,
        private val appStateTracker: AppStateTracker,
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
                    NotificationLogger.logNotificationTapped(
                        notificationData.type,
                        notificationData.action,
                        notificationData.contentId,
                    )
                    val action =
                        NotificationPerformanceMonitor.measurePerformanceSuspend("handleNotificationTap") {
                            handleNotificationTapUseCase(notificationData)
                        }
                    val deepLink = notificationMapper.mapActionToDeepLink(action, notificationData.contentId)
                    NotificationLogger.logNavigation(action, deepLink)

                    _notificationState.value =
                        NotificationState.Navigate(deepLink, action, notificationData.contentId)
                } catch (e: Exception) {
                    NotificationLogger.logError("Error al procesar la notificación", e)
                    _notificationState.value = NotificationState.Error(e.message ?: "Unknown error")
                }
            }
        }

        /**
         * Tap de la notificación multimedia de Media3 (llega con la acción pero
         * sin extras): enfoca el reproductor actual (spec 0016, US2/AC1).
         */
        fun handlePlaybackNotificationTap() {
            val deepLink = notificationMapper.mapActionToDeepLink(NotificationAction.OPEN_PLAYER)
            NotificationLogger.logNotificationTapped(NotificationType.PLAYBACK, NotificationAction.OPEN_PLAYER, null)
            NotificationLogger.logNavigation(NotificationAction.OPEN_PLAYER, deepLink)
            _notificationState.value = NotificationState.Navigate(deepLink, NotificationAction.OPEN_PLAYER, null)
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
            val previous = appStateTracker.currentState.value
            if (previous == state) return
            appStateTracker.updateState(state)
            NotificationLogger.logAppStateChange(previous, state)
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
    data class Navigate(
        val deepLink: String,
        val action: NotificationAction,
        val contentId: String? = null,
    ) : NotificationState()

    /** Error state - an error occurred while handling the notification */
    data class Error(val message: String) : NotificationState()
}
