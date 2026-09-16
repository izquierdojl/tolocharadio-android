package com.izquierdojl.tolocharadio.cast

import androidx.media3.common.DeviceInfo
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.feature.player.PlaybackVolumeController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests Red-Green del wrapper `Player` de la sesión (spec 0035, FR-001/FR-009/FR-013).
 *
 * Verifica el contrato de `contracts/cast-volume.md` §1: comandos de volumen de dispositivo,
 * `DeviceInfo` remoto/local, mapeo a pasos 0..100, delegación de reproducción y emisión de
 * `onDeviceVolumeChanged`. Los `Commands` reales no se construyen (usan APIs de framework no
 * mockeadas en JVM); se verifica la petición al builder.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DEPRECATION")
class CastDeviceVolumePlayerTest {
    private val exoPlayer: ExoPlayer = mockk(relaxed = true)
    private val commands: Player.Commands = mockk(relaxed = true)
    private val commandsBuilder: Player.Commands.Builder = mockk(relaxed = true)
    private val delegate: Player = mockk(relaxed = true)

    @Before
    fun setup() {
        every { delegate.availableCommands } returns commands
        every { commands.buildUpon() } returns commandsBuilder
    }

    private fun newScope() = CoroutineScope(UnconfinedTestDispatcher())

    private fun controller(scope: CoroutineScope = newScope()) = PlaybackVolumeController(exoPlayer, scope)

    @Test
    fun `pide los cinco comandos de volumen de dispositivo`() {
        CastDeviceVolumePlayer(delegate, controller()).availableCommands
        verify(exactly = 1) {
            commandsBuilder.addAll(
                Player.COMMAND_GET_DEVICE_VOLUME,
                Player.COMMAND_SET_DEVICE_VOLUME,
                Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
                Player.COMMAND_ADJUST_DEVICE_VOLUME,
                Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS,
            )
        }
    }

    @Test
    fun `mantiene los comandos de volumen sin reproduccion en curso`() {
        val player = CastDeviceVolumePlayer(delegate, controller())
        every { delegate.playbackState } returns Player.STATE_IDLE
        assertNotNull(player.availableCommands)
        every { delegate.playbackState } returns Player.STATE_READY
        assertNotNull(player.availableCommands)
        verify(exactly = 2) {
            commandsBuilder.addAll(
                Player.COMMAND_GET_DEVICE_VOLUME,
                Player.COMMAND_SET_DEVICE_VOLUME,
                Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
                Player.COMMAND_ADJUST_DEVICE_VOLUME,
                Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS,
            )
        }
    }

    @Test
    fun `device info remoto con maxVolume 100`() {
        val player = CastDeviceVolumePlayer(delegate, controller())
        val info = player.deviceInfo
        assertEquals(DeviceInfo.PLAYBACK_TYPE_REMOTE, info.playbackType)
        assertEquals(PlaybackVolumeController.MAX_VOLUME, info.maxVolume)
    }

    @Test
    fun `device info pasa a local cuando el receptor no soporta volumen`() =
        runTest {
            val scheduler = testScheduler
            val controller = PlaybackVolumeController(exoPlayer, CoroutineScope(UnconfinedTestDispatcher(scheduler)))
            val player = CastDeviceVolumePlayer(delegate, controller)
            controller.bind(TestRemoteVolume(volume = 0.3, ignoresWrites = true))
            controller.setCastVolume(0.5f)
            advanceTimeBy(PlaybackVolumeController.VOLUME_CONFIRMATION_TIMEOUT_MS + 100)
            assertEquals(DeviceInfo.PLAYBACK_TYPE_LOCAL, player.deviceInfo.playbackType)
        }

    @Test
    fun `setDeviceVolume e increase-decrease mapean a pasos 0-100`() {
        val remote = TestRemoteVolume(volume = 0.5)
        val controller = controller()
        controller.bind(remote)
        val player = CastDeviceVolumePlayer(delegate, controller)
        assertEquals(50, player.deviceVolume)
        player.setDeviceVolume(42, 0)
        assertEquals(42, player.deviceVolume)
        player.setDeviceVolume(42)
        assertEquals(42, player.deviceVolume)
        player.increaseDeviceVolume(0)
        assertEquals(43, player.deviceVolume)
        player.decreaseDeviceVolume(0)
        assertEquals(42, player.deviceVolume)
        assertEquals(0.42, remote.writtenVolumes.last(), 0.001)
    }

    @Test
    fun `setDeviceMuted e isDeviceMuted reflejan el controlador`() {
        val remote = TestRemoteVolume()
        val controller = controller()
        controller.bind(remote)
        val player = CastDeviceVolumePlayer(delegate, controller)
        player.setDeviceMuted(true, 0)
        assertTrue(player.isDeviceMuted)
        assertTrue(remote.muted)
        player.setDeviceMuted(false)
        assertFalse(player.isDeviceMuted)
    }

    @Test
    fun `emitDeviceVolumeChanged notifica a los listeners registrados`() {
        val player = CastDeviceVolumePlayer(delegate, controller())
        val listener = mockk<Player.Listener>(relaxed = true)
        player.addListener(listener)
        player.emitDeviceVolumeChanged(37, true)
        verify { listener.onDeviceVolumeChanged(37, true) }
        verify { delegate.addListener(listener) }
        player.removeListener(listener)
        verify { delegate.removeListener(listener) }
    }

    @Test
    fun `delega la reproduccion en el Player subyacente`() {
        val player = CastDeviceVolumePlayer(delegate, controller())
        player.play()
        verify { delegate.play() }
        every { delegate.isPlaying } returns true
        assertTrue(player.isPlaying)
    }

    /** Receptor simulado para el controlador (eco explícito no usado aquí). */
    private class TestRemoteVolume(
        var volume: Double = 1.0,
        var muted: Boolean = false,
        private val ignoresWrites: Boolean = false,
    ) : RemoteVolumeDevice {
        val writtenVolumes = mutableListOf<Double>()

        override fun readVolume(): Double? = volume

        override fun readMuted(): Boolean = muted

        override fun writeVolume(volume: Double) {
            writtenVolumes += volume
            if (!ignoresWrites) this.volume = volume
        }

        override fun writeMuted(muted: Boolean) {
            this.muted = muted
        }

        override fun observe(onChanged: () -> Unit) = Unit

        override fun stopObserving() = Unit
    }
}
