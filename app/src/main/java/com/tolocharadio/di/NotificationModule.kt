package com.tolocharadio.di

import com.tolocharadio.domain.notification.AppState
import com.tolocharadio.domain.notification.AppStateTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Singleton

/**
 * Hilt module for notification dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    
    @Provides
    @Singleton
    fun provideAppStateTracker(): AppStateTracker {
        return InMemoryAppStateTracker()
    }
}

/**
 * In-memory implementation of AppStateTracker.
 */
class InMemoryAppStateTracker : AppStateTracker {
    
    private val _currentState = MutableStateFlow(AppState.NOT_RUNNING)
    override val currentState: StateFlow<AppState> = _currentState
    
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
