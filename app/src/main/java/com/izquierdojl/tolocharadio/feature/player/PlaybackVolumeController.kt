package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.cast.RemoteVolumeDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Fuente única de volumen y silencio de la reproducción (spec 0035).
 *
 * Con sesión Cast activa ([bind]) el volumen y el silencio se aplican al dispositivo remoto
 * (`CastSession`, volumen de dispositivo) y el estado refleja el eco real del receptor
 * (FR-005). Sin sesión, el silencio se aplica al `ExoPlayer` local y el estado sobrevive al
 * cambio de salida (FR-015).
 *
 * Detección de receptor sin volumen (FR-009): tras el último ajuste, si no llega eco en
 * [VOLUME_CONFIRMATION_TIMEOUT_MS] y el valor leído difiere del solicitado, se marca
 * [castVolumeSupported] como `false` y se emite un único aviso en [notices]. Los fallos
 * transitorios ya confirmados se corrigen en silencio (FR-014).
 *
 * Hilos: todas las operaciones se invocan desde el hilo principal; [scope] debe usar
 * `Dispatchers.Main.immediate`.
 */
@Suppress("TooManyFunctions")
class PlaybackVolumeController(
    private val exoPlayer: ExoPlayer,
    private val scope: CoroutineScope,
) {
    private val _muted = MutableStateFlow(false)

    /** Silencio único de la app, aplicado a la salida activa (FR-007, FR-015). */
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _castVolume = MutableStateFlow(1f)

    /** Volumen real conocido del receptor en `[0f, 1f]` (FR-003, FR-005). */
    val castVolume: StateFlow<Float> = _castVolume.asStateFlow()

    private val _castVolumeSupported = MutableStateFlow(true)

    /** `false` cuando el receptor no admite control de volumen (FR-009). */
    val castVolumeSupported: StateFlow<Boolean> = _castVolumeSupported.asStateFlow()

    private val _notices = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Aviso único de receptor sin soporte de volumen, para la UI (FR-009). */
    val notices: SharedFlow<Unit> = _notices.asSharedFlow()

    private var device: RemoteVolumeDevice? = null

    /** Último valor solicitado al receptor pendiente de eco (FR-014). */
    private var pendingVolume: Double? = null
    private var confirmationJob: Job? = null

    /** Hay sesión Cast enlazada y el volumen se aplica al receptor. */
    val isRemoteActive: Boolean
        get() = device != null

    /**
     * Enlaza el receptor [device]: aplica el silencio vigente (FR-015), lee el volumen real y
     * empieza a observar el eco.
     */
    fun bind(device: RemoteVolumeDevice) {
        unbind()
        this.device = device
        _castVolumeSupported.value = true
        runCatching { device.writeMuted(_muted.value) }
        device.readVolume()?.let { _castVolume.value = it.toFloat().coerceIn(0f, 1f) }
        device.observe { onDeviceChanged() }
    }

    /** Desenlaza el receptor y devuelve el control de silencio al reproductor local (FR-010, FR-015). */
    fun unbind() {
        cancelConfirmation()
        device?.stopObserving()
        device = null
        applyMuteToActiveOutput()
    }

    /** Ajusta el volumen del receptor de forma continua (FR-004). Ignora `NaN`. */
    fun setCastVolume(volume: Float) {
        if (volume.isNaN()) return
        val remote = device ?: return
        if (_muted.value) {
            // FR-008: ajustar estando silenciado restablece el audio al nuevo nivel.
            _muted.value = false
            runCatching { remote.writeMuted(false) }
        }
        val clamped = volume.coerceIn(0f, 1f)
        _castVolume.value = clamped
        val pending = clamped.toDouble()
        pendingVolume = pending
        runCatching { remote.writeVolume(pending) }
        scheduleConfirmationCheck()
    }

    /** Sube/baja [delta] pasos (1 % cada uno) con clamp en los extremos (FR-004). */
    fun stepCastVolume(delta: Int) {
        val steps = (castVolume.value * MAX_VOLUME).roundToInt() + delta
        setCastVolume(steps.coerceIn(0, MAX_VOLUME) / MAX_VOLUME.toFloat())
    }

    /** Alterna el silencio de la salida activa (FR-007). */
    fun toggleMute() {
        setMuted(!_muted.value)
    }

    /** Fija el silencio de la salida activa (ADJUST_MUTE/UNMUTE del sistema y botón de la app). */
    fun setMuted(muted: Boolean) {
        if (_muted.value == muted) return
        _muted.value = muted
        applyMuteToActiveOutput()
    }

    /** Quita el silencio de la salida activa (regla de la spec 004 al reproducir/detener). */
    fun resetMute() {
        setMuted(false)
    }

    /** Volumen del receptor en pasos `0..100` para el `Player` de la sesión. */
    fun deviceVolumePercent(): Int = (castVolume.value * MAX_VOLUME).roundToInt().coerceIn(0, MAX_VOLUME)

    /** Aplica el volumen en pasos `0..100` recibido del `Player` de la sesión. */
    fun setDeviceVolumePercent(percent: Int) {
        setCastVolume(percent.coerceIn(0, MAX_VOLUME) / MAX_VOLUME.toFloat())
    }

    /** Silencio del receptor para el `Player` de la sesión. */
    fun isDeviceMuted(): Boolean = _muted.value

    private fun applyMuteToActiveOutput() {
        val remote = device
        if (remote != null) {
            runCatching { remote.writeMuted(_muted.value) }
        } else {
            exoPlayer.volume = if (_muted.value) 0f else 1f
        }
    }

    /** Eco del receptor: confirma o corrige el valor mostrado y sincroniza el silencio. */
    private fun onDeviceChanged() {
        val remote = device ?: return
        val volume = remote.readVolume() ?: return
        val pending = pendingVolume
        if (pending != null && abs(volume - pending) <= VOLUME_EPSILON) {
            cancelConfirmation()
        }
        _castVolume.value = volume.toFloat().coerceIn(0f, 1f)
        val muted = remote.readMuted()
        if (muted != _muted.value) _muted.value = muted
    }

    private fun scheduleConfirmationCheck() {
        confirmationJob?.cancel()
        confirmationJob =
            scope.launch {
                delay(VOLUME_CONFIRMATION_TIMEOUT_MS)
                checkConfirmation()
            }
    }

    private fun checkConfirmation() {
        val pending = pendingVolume ?: return
        val remote = device ?: return
        val read = remote.readVolume()
        if (read != null && abs(read - pending) <= VOLUME_EPSILON) {
            cancelConfirmation()
            _castVolume.value = read.toFloat().coerceIn(0f, 1f)
            return
        }
        cancelConfirmation()
        if (_castVolumeSupported.value) {
            _castVolumeSupported.value = false
            _notices.tryEmit(Unit)
        }
    }

    private fun cancelConfirmation() {
        confirmationJob?.cancel()
        confirmationJob = null
        pendingVolume = null
    }

    companion object {
        /** Pasos del control remoto (1 % por paso). */
        const val MAX_VOLUME = 100

        /** Ventana sin eco antes de considerar el receptor sin soporte de volumen. */
        const val VOLUME_CONFIRMATION_TIMEOUT_MS = 3_000L

        /** Tolerancia de eco equivalente a un paso. */
        private const val VOLUME_EPSILON = 0.01
    }
}
