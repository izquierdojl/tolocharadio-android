package com.izquierdojl.tolocharadio.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.domain.SleepTimerDuration
import com.izquierdojl.tolocharadio.domain.SleepTimerState
import com.izquierdojl.tolocharadio.domain.SleepTimerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * UI state del temporizador de apagado.
 */
sealed interface SleepTimerUiState {
    data object Inactive : SleepTimerUiState

    data class Active(
        val remainingMinutes: Int,
    ) : SleepTimerUiState
}

/**
 * ViewModel del temporizador de apagado.
 *
 * Gestiona el estado UI del timer y delega la lógica de countdown a [SleepTimerUseCase].
 * Al expirar, invoca [PlayerViewModel.stop()] para detener la reproducción silenciosamente.
 *
 * Scope: Activity-scoped (mismo patrón que PlayerViewModel) para persistir al navegar.
 */
@HiltViewModel
class SleepTimerViewModel
    @Inject
    constructor(
        private val sleepTimerUseCase: SleepTimerUseCase,
    ) : ViewModel() {
        val state: StateFlow<SleepTimerState> = sleepTimerUseCase.state

        val uiState: StateFlow<SleepTimerUiState> =
            sleepTimerUseCase.state
                .map { timerState ->
                    when (timerState) {
                        is SleepTimerState.Inactive -> SleepTimerUiState.Inactive
                        is SleepTimerState.Active ->
                            SleepTimerUiState.Active(
                                remainingMinutes = timerState.remainingMinutes,
                            )
                    }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SleepTimerUiState.Inactive)

        private var onStopPlayer: (() -> Unit)? = null

        /**
         * Registra el callback para detener el player al expirar el timer.
         * Debe llamarse desde TolochaNavGraph donde se tiene acceso al PlayerViewModel.
         */
        fun setStopPlayerCallback(callback: () -> Unit) {
            onStopPlayer = callback
        }

        /**
         * Inicia un temporizador con la duración especificada.
         */
        fun start(duration: SleepTimerDuration) {
            sleepTimerUseCase.start(duration) {
                onStopPlayer?.invoke()
            }
        }

        /**
         * Cancela el temporizador activo.
         */
        fun cancel() {
            sleepTimerUseCase.cancel()
        }
    }
