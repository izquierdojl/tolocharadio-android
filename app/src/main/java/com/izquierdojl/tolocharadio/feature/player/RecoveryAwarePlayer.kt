package com.izquierdojl.tolocharadio.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi

/**
 * Player de la `MediaSession` que refleja el estado lógico de recuperación
 * (spec 0044, FR-018).
 *
 * Durante `Reconnecting`/`WaitingForNetwork` la notificación debe mostrar
 * buffering aunque el `ExoPlayer` esté en `IDLE` por el error de red. El player
 * subyacente sigue siendo el mismo: solo se adapta el estado publicado a la
 * sesión multimedia.
 */
@OptIn(UnstableApi::class)
class RecoveryAwarePlayer(
    player: Player,
) : ForwardingSimpleBasePlayer(player) {
    @Volatile
    private var recovering = false

    /** Activa o desactiva el estado de transporte de recuperación. */
    fun setRecovering(value: Boolean) {
        if (recovering == value) return
        recovering = value
        invalidateState()
    }

    /** Sustituye el player subyacente al cambiar el perfil de buffer. */
    fun updateDelegate(player: Player) {
        setPlayer(player)
    }

    override fun getState(): SimpleBasePlayer.State {
        val base = super.getState()
        if (!recovering || base.playlist.isEmpty()) return base
        return base
            .buildUpon()
            .setPlaybackState(Player.STATE_BUFFERING)
            .setPlayerError(null)
            .setIsLoading(true)
            .setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .build()
    }
}
