package com.izquierdojl.tolocharadio.domain.playback

/** Acción que la UI debe ofrecer ante un error de reproducción (spec 0042). */
enum class PlayerErrorAction {
    RETRY,
    EDIT_SERVER,
}

/** Estado interno del supervisor de recuperación (spec 0042/0044). */
sealed interface RecoveryState {
    data object Idle : RecoveryState

    data class WaitingForNetwork(
        val stationId: String,
        val cause: InterruptionCause,
        val initial: Boolean = false,
    ) : RecoveryState

    data class Reconnecting(
        val stationId: String,
        val attempt: Int,
        val maxAttempts: Int,
        val cause: InterruptionCause,
        val initial: Boolean = false,
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

/** `true` mientras la UI debe mostrar transporte en buffering por recuperación. */
fun RecoveryState.isTransportBuffering(): Boolean {
    return this is RecoveryState.Reconnecting || this is RecoveryState.WaitingForNetwork
}
