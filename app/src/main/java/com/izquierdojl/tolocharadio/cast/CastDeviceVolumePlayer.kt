package com.izquierdojl.tolocharadio.cast

import androidx.annotation.OptIn
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.izquierdojl.tolocharadio.feature.player.PlaybackVolumeController
import java.util.concurrent.CopyOnWriteArrayList

/**
 * `Player` de la sesión que expone el volumen del dispositivo Cast (spec 0035, research D2).
 *
 * Delega todo en [delegate] (`CastPlayer`) y añade los comandos de volumen de dispositivo y
 * un `DeviceInfo` remoto con `maxVolume = 100`. Con eso, el `MediaSession` de media3 crea
 * automáticamente su `VolumeProviderCompat` remoto: las teclas físicas ajustan el receptor y
 * aparece la barra de volumen del sistema.
 *
 * Si [PlaybackVolumeController] detecta que el receptor no admite volumen, `DeviceInfo` pasa a
 * `PLAYBACK_TYPE_LOCAL` y `CastPlayerManager` reinstala el player para devolver las teclas al
 * volumen del móvil (FR-009).
 *
 * [emitDeviceVolumeChanged] propaga el estado al `MediaSession` para mantener sincronizada la
 * barra del sistema con el eco del receptor (FR-005).
 */
@OptIn(UnstableApi::class)
@Suppress("DEPRECATION", "TooManyFunctions")
class CastDeviceVolumePlayer(
    private val delegate: Player,
    private val controller: PlaybackVolumeController,
) : Player by delegate {
    private val listeners = CopyOnWriteArrayList<Player.Listener>()

    /** Notifica a los listeners de la sesión el volumen (`0..100`) y silencio vigentes. */
    fun emitDeviceVolumeChanged(
        volume: Int,
        muted: Boolean,
    ) {
        listeners.forEach { it.onDeviceVolumeChanged(volume, muted) }
    }

    override fun getDeviceInfo(): DeviceInfo =
        if (controller.castVolumeSupported.value) {
            DeviceInfo
                .Builder(DeviceInfo.PLAYBACK_TYPE_REMOTE)
                .setMaxVolume(PlaybackVolumeController.MAX_VOLUME)
                .build()
        } else {
            DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_LOCAL).build()
        }

    override fun getAvailableCommands(): Player.Commands =
        delegate.availableCommands
            .buildUpon()
            .addAll(
                Player.COMMAND_GET_DEVICE_VOLUME,
                Player.COMMAND_SET_DEVICE_VOLUME,
                Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
                Player.COMMAND_ADJUST_DEVICE_VOLUME,
                Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS,
            ).build()

    override fun getDeviceVolume(): Int {
        return controller.deviceVolumePercent()
    }

    override fun setDeviceVolume(volume: Int) {
        controller.setDeviceVolumePercent(volume)
    }

    override fun setDeviceVolume(
        volume: Int,
        flags: Int,
    ) {
        controller.setDeviceVolumePercent(volume)
    }

    override fun increaseDeviceVolume() {
        controller.stepCastVolume(1)
    }

    override fun increaseDeviceVolume(flags: Int) {
        controller.stepCastVolume(1)
    }

    override fun decreaseDeviceVolume() {
        controller.stepCastVolume(-1)
    }

    override fun decreaseDeviceVolume(flags: Int) {
        controller.stepCastVolume(-1)
    }

    override fun isDeviceMuted(): Boolean = controller.isDeviceMuted()

    override fun setDeviceMuted(muted: Boolean) = controller.setMuted(muted)

    override fun setDeviceMuted(
        muted: Boolean,
        flags: Int,
    ) = controller.setMuted(muted)

    override fun addListener(listener: Player.Listener) {
        listeners += listener
        delegate.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        listeners -= listener
        delegate.removeListener(listener)
    }
}
