package com.izquierdojl.tolocharadio.domain

import com.izquierdojl.tolocharadio.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opciones de duración predefinidas para el temporizador de apagado.
 */
enum class SleepTimerDuration(val minutes: Int) {
    MINUTES_15(15),
    MINUTES_30(30),
    MINUTES_45(45),
    MINUTES_60(60),
    MINUTES_90(90),
}

/**
 * Estado del temporizador de apagado. Transitorio (no persiste entre sesiones).
 */
sealed interface SleepTimerState {
    data object Inactive : SleepTimerState

    data class Active(
        val durationMinutes: Int,
        val remainingSeconds: Long,
        val expiresAtEpochMs: Long,
    ) : SleepTimerState
}

/**
 * Caso de uso del temporizador de apagado.
 *
 * Gestiona la lógica de countdown con corrutinas. Cuando el temporizador expira,
 * notifica al caller mediante [onExpired].
 *
 * Thread-safety: todas las operaciones se ejecutan en el [CoroutineScope] inyectado.
 */
@Singleton
class SleepTimerUseCase
    @Inject
    constructor(
        @ApplicationScope private val scope: CoroutineScope,
    ) {

        private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Inactive)
        val state: StateFlow<SleepTimerState> = _state.asStateFlow()

        private var countdownJob: Job? = null

        /**
         * Inicia un temporizador con la duración especificada.
         * Si ya hay uno activo, lo reemplaza.
         *
         * @param duration Duración del temporizador
         * @param onExpired Callback invocado cuando el temporizador expira
         */
        fun start(
            duration: SleepTimerDuration,
            onExpired: () -> Unit,
        ) {
            countdownJob?.cancel()
            val now = System.currentTimeMillis()
            val totalSeconds = duration.minutes * 60L
            val expiresAt = now + totalSeconds * 1000L

            _state.value =
                SleepTimerState.Active(
                    durationMinutes = duration.minutes,
                    remainingSeconds = totalSeconds,
                    expiresAtEpochMs = expiresAt,
                )

            countdownJob =
                scope.launch {
                    var remaining = totalSeconds
                    while (remaining > 0) {
                        delay(1000L)
                        remaining--
                        val currentState = _state.value
                        if (currentState is SleepTimerState.Active) {
                            _state.value = currentState.copy(remainingSeconds = remaining)
                        }
                    }
                    _state.value = SleepTimerState.Inactive
                    onExpired()
                }
        }

        /**
         * Cancela el temporizador activo.
         */
        fun cancel() {
            countdownJob?.cancel()
            countdownJob = null
            _state.value = SleepTimerState.Inactive
        }

        /**
         * Cancela el temporizador activo y libera recursos.
         */
        fun release() {
            cancel()
        }
    }
