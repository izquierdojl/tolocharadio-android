package com.izquierdojl.tolocharadio.domain.playback

/** Acción que la UI debe ofrecer ante un error de reproducción (spec 0042). */
enum class PlayerErrorAction {
    RETRY,
    EDIT_SERVER,
}

/** Estado interno del manager de recuperación (spec 0042, data-model §6). */
sealed interface RecoveryState {
    data object Idle : RecoveryState

    data class WaitingForNetwork(
        val stationId: String,
        val cause: InterruptionCause,
    ) : RecoveryState

    data class Reconnecting(
        val stationId: String,
        val attempt: Int,
        val maxAttempts: Int,
        val cause: InterruptionCause,
    ) : RecoveryState

    data class Failed(
        val stationId: String,
        val cause: InterruptionCause,
        val message: String,
        val action: PlayerErrorAction,
    ) : RecoveryState

    data class PausedAfterLoss(
        val stationId: String,
    ) : RecoveryState
}
