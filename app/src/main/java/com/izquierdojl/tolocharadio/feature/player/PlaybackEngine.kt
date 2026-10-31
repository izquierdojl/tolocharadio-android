package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Resultado de sustituir el player para aplicar un perfil (spec 0042, R2). */
data class PlayerSwap(
    val old: ExoPlayer,
    val new: ExoPlayer,
)

/**
 * Dueño del `ExoPlayer` local y de su reemplazo al cambiar de perfil.
 *
 * El servicio es quien libera el player anterior tras `MediaSession.setPlayer`;
 * [ensureProfile] sí libera el anterior porque se usa antes de crear la sesión.
 */
class PlaybackEngine(
    private val factory: PlaybackPlayerFactory,
    initialProfile: BufferProfile,
) {
    private val _playerFlow = MutableStateFlow(factory.create(initialProfile))
    val playerFlow: StateFlow<ExoPlayer> = _playerFlow.asStateFlow()

    val player: ExoPlayer
        get() = _playerFlow.value

    @Volatile
    var currentProfile: BufferProfile = initialProfile
        private set

    /** Sincroniza el perfil persistido al arrancar el servicio. */
    fun ensureProfile(profile: BufferProfile): ExoPlayer {
        if (profile == currentProfile) return player
        val old = player
        val new = factory.create(profile)
        currentProfile = profile
        _playerFlow.value = new
        old.stop()
        old.release()
        return new
    }

    /** Crea el player del nuevo perfil; el llamador libera el anterior. */
    fun swap(profile: BufferProfile): PlayerSwap {
        val old = player
        val new = factory.create(profile)
        currentProfile = profile
        _playerFlow.value = new
        return PlayerSwap(old, new)
    }
}
