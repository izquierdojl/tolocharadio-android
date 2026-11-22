package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import com.izquierdojl.tolocharadio.domain.playback.ContentKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Resultado de sustituir el player para aplicar un perfil (spec 0042, R2). */
data class PlayerSwap(
    val old: ExoPlayer,
    val new: ExoPlayer,
)

/**
 * Dueño del `ExoPlayer` local, de su perfil de buffer y de su tipo de
 * contenido (spec 0042/0058).
 *
 * Media3 no permite cambiar el `LoadControl` en caliente, así que [ensure]
 * recrea el player cuando cambia el perfil o el tipo. El player anterior no se
 * libera aquí: queda en [takePendingRelease] para que el servicio lo suelte
 * después de actualizar `MediaSession.setPlayer`.
 *
 * [swap] mantiene el contrato de la spec 0042: entrega ambos players y el
 * llamador libera el anterior.
 */
class PlaybackEngine(
    private val factory: PlaybackPlayerFactory,
    initialProfile: BufferProfile,
) {
    private val _playerFlow = MutableStateFlow(factory.create(initialProfile, ContentKind.LIVE))
    val playerFlow: StateFlow<ExoPlayer> = _playerFlow.asStateFlow()

    val player: ExoPlayer
        get() = _playerFlow.value

    @Volatile
    var currentProfile: BufferProfile = initialProfile
        private set

    @Volatile
    var activeKind: ContentKind = ContentKind.LIVE
        private set

    private var pendingRelease: ExoPlayer? = null

    /**
     * Garantiza un player con [profile] y [kind]. Si ya coincide, reutiliza el
     * actual; si no, crea uno nuevo y deja el anterior pendiente de liberar.
     */
    fun ensure(
        profile: BufferProfile,
        kind: ContentKind,
    ): ExoPlayer {
        if (profile == currentProfile && kind == activeKind) return player
        val old = player
        val new = factory.create(profile, kind)
        currentProfile = profile
        activeKind = kind
        pendingRelease = old
        _playerFlow.value = new
        old.stop()
        old.clearMediaItems()
        return new
    }

    /** Sincroniza el perfil persistido sin cambiar de tipo de contenido. */
    fun ensureProfile(profile: BufferProfile): ExoPlayer = ensure(profile, activeKind)

    /** Cambia el tipo de contenido conservando el perfil (spec 0058, FR-001). */
    fun ensureKind(kind: ContentKind): ExoPlayer = ensure(currentProfile, kind)

    /** Player anterior que el servicio debe liberar tras cambiar la sesión. */
    fun takePendingRelease(): ExoPlayer? {
        val pending = pendingRelease
        pendingRelease = null
        return pending
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
