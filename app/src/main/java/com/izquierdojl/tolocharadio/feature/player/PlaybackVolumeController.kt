package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.cast.RemoteVolumeDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fuente única de silencio de la reproducción (spec 0037).
 *
 * El volumen de la salida activa lo gestiona el reproductor de la sesión (nativo de
 * media3 1.11.0: teclas y barra del sistema); la app solo conserva el **silencio** como
 * estado, aplicado a la salida activa y sincronizado con el eco del receptor (FR-005,
 * contracts §2). El silencio sobrevive al cambio de salida cuando su origen es el
 * usuario (FR-006); el adoptado del receptor (eco externo) se descarta al desconectar.
 *
 * Hilos: todas las operaciones se invocan desde el hilo principal.
 */
class PlaybackVolumeController(
    private val exoPlayer: ExoPlayer,
) {
    private val _muted = MutableStateFlow(false)

    /** Silencio único de la app, aplicado a la salida activa (FR-006). */
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    /** Origen del silencio vigente: acción del usuario o eco externo del receptor. */
    private var muteOrigin: MuteOrigin = MuteOrigin.USER

    private var device: RemoteVolumeDevice? = null

    /** Hay sesión Cast enlazada y el silencio se aplica al receptor. */
    val isRemoteActive: Boolean
        get() = device != null

    /**
     * Enlaza el receptor [device]: aplica el silencio vigente solo si la app estaba
     * silenciada (carry-over, FR-006); si no, lee y representa el silencio real del
     * receptor sin escribir nada (FR-005) y empieza a observar el eco.
     */
    fun bind(device: RemoteVolumeDevice) {
        unbind()
        this.device = device
        if (_muted.value) {
            runCatching { device.writeMuted(true) }
        } else {
            _muted.value = device.readMuted()
            muteOrigin = if (_muted.value) MuteOrigin.ECHO else MuteOrigin.USER
        }
        device.observe { onDeviceChanged() }
    }

    /** Desenlaza el receptor y devuelve el control de silencio al reproductor local (FR-010, FR-006). */
    fun unbind() {
        device?.stopObserving()
        device = null
        if (muteOrigin == MuteOrigin.ECHO) {
            // Silencio adoptado del receptor: no se arrastra al teléfono (FR-006).
            _muted.value = false
        }
        applyMuteToActiveOutput()
    }

    /** Alterna el silencio de la salida activa (FR-006). */
    fun toggleMute() {
        setMuted(!_muted.value)
    }

    /** Fija el silencio de la salida activa (ADJUST_MUTE/UNMUTE del sistema y botón de la app). */
    fun setMuted(muted: Boolean) {
        if (_muted.value == muted) return
        _muted.value = muted
        muteOrigin = MuteOrigin.USER
        applyMuteToActiveOutput()
    }

    /** Quita el silencio de la salida activa (regla de la spec 004 al reproducir/detener). */
    fun resetMute() {
        setMuted(false)
    }

    private fun applyMuteToActiveOutput() {
        val remote = device
        if (remote != null) {
            runCatching { remote.writeMuted(_muted.value) }
        } else {
            exoPlayer.volume = if (_muted.value) 0f else 1f
        }
    }

    /** Eco del receptor: sincroniza el silencio con origen eco, sin avisos. */
    private fun onDeviceChanged() {
        val remote = device ?: return
        val muted = remote.readMuted()
        if (muted != _muted.value) {
            _muted.value = muted
            muteOrigin = MuteOrigin.ECHO
        }
    }

    /** Procedencia del silencio vigente (contracts §2). */
    private enum class MuteOrigin {
        /** Botón de silencio o comando de la notificación. */
        USER,

        /** Adoptado del receptor (eco externo). */
        ECHO,
    }
}
