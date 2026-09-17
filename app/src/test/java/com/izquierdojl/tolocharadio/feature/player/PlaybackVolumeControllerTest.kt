package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.cast.RemoteVolumeDevice
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests Red-Green del controlador de volumen (spec 0035, FR-003..FR-015).
 *
 * El receptor se sustituye por [FakeRemoteVolume] para poder simular eco, silencio y
 * receptores que ignoran los ajustes (research D7/D10).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackVolumeControllerTest {
    private val exoPlayer: ExoPlayer = mockk(relaxed = true)

    private fun newScope() = CoroutineScope(UnconfinedTestDispatcher())

    private fun controller(scope: CoroutineScope = newScope()) = PlaybackVolumeController(exoPlayer, scope)

    @Test
    fun `setCastVolume clampa y escribe en el dispositivo`() {
        val remote = FakeRemoteVolume()
        val controller = controller()
        controller.bind(remote)
        controller.setCastVolume(1.5f)
        assertEquals(1f, controller.castVolume.value, 0.001f)
        assertEquals(1.0, remote.writtenVolumes.last(), 0.001)
        controller.setCastVolume(-0.5f)
        assertEquals(0f, controller.castVolume.value, 0.001f)
    }

    @Test
    fun `setCastVolume ignora NaN`() {
        val remote = FakeRemoteVolume()
        val controller = controller()
        controller.bind(remote)
        val before = controller.castVolume.value
        controller.setCastVolume(Float.NaN)
        assertEquals(before, controller.castVolume.value, 0.001f)
        assertTrue(remote.writtenVolumes.isEmpty())
    }

    @Test
    fun `stepCastVolume avanza un paso y respeta extremos`() {
        val remote = FakeRemoteVolume(volume = 0.5)
        val controller = controller()
        controller.bind(remote)
        controller.stepCastVolume(1)
        assertEquals(0.51f, controller.castVolume.value, 0.001f)
        controller.stepCastVolume(-5)
        assertEquals(0.46f, controller.castVolume.value, 0.001f)
        controller.setCastVolume(0f)
        controller.stepCastVolume(-1)
        assertEquals(0f, controller.castVolume.value, 0.001f)
        controller.setCastVolume(1f)
        controller.stepCastVolume(1)
        assertEquals(1f, controller.castVolume.value, 0.001f)
    }

    @Test
    fun `ajustar volumen con silencio restablece el audio`() {
        val remote = FakeRemoteVolume()
        val controller = controller()
        controller.bind(remote)
        controller.toggleMute()
        assertTrue(remote.muted)
        controller.setCastVolume(0.5f)
        assertFalse(controller.muted.value)
        assertFalse(remote.muted)
        assertEquals(0.5, remote.writtenVolumes.last(), 0.001)
    }

    @Test
    fun `toggleMute con remoto aplica el silencio del dispositivo`() {
        val remote = FakeRemoteVolume()
        val controller = controller()
        controller.bind(remote)
        controller.toggleMute()
        assertTrue(controller.muted.value)
        assertTrue(remote.muted)
        controller.toggleMute()
        assertFalse(controller.muted.value)
        assertFalse(remote.muted)
    }

    @Test
    fun `toggleMute sin remoto aplica al reproductor local`() {
        val controller = controller()
        controller.toggleMute()
        verify { exoPlayer.volume = 0f }
        controller.toggleMute()
        verify { exoPlayer.volume = 1f }
    }

    @Test
    fun `bind aplica el silencio vigente al receptor y lee el volumen real`() {
        val remote = FakeRemoteVolume(volume = 0.5)
        val controller = controller()
        controller.toggleMute()
        controller.bind(remote)
        assertTrue(remote.muted)
        assertEquals(0.5f, controller.castVolume.value, 0.001f)
        assertTrue(controller.isRemoteActive)
    }

    @Test
    fun `bind nunca deja el volumen en el default 100 por ciento`() {
        val remote = FakeRemoteVolume(volume = 0.3)
        val controller = controller()
        // Default castVolume should be 1.0, but bind must override it immediately
        assertEquals(1f, controller.castVolume.value, 0.001f)
        controller.bind(remote)
        // After bind, castVolume MUST reflect the receiver's real volume, not 100%
        assertEquals(0.3f, controller.castVolume.value, 0.001f)
        assertEquals(30, controller.deviceVolumePercent())
    }

    @Test
    fun `unbind aplica el silencio al reproductor local y deja de observar`() {
        val remote = FakeRemoteVolume()
        val controller = controller()
        controller.bind(remote)
        controller.toggleMute()
        controller.unbind()
        assertFalse(remote.observing)
        assertFalse(controller.isRemoteActive)
        verify { exoPlayer.volume = 0f }
    }

    @Test
    fun `resetMute restablece el sonido`() {
        val remote = FakeRemoteVolume()
        val controller = controller()
        controller.bind(remote)
        controller.toggleMute()
        controller.resetMute()
        assertFalse(controller.muted.value)
        assertFalse(remote.muted)
    }

    @Test
    fun `eco equivalente confirma y no marca no soportado`() =
        runTest {
            val scheduler = testScheduler
            val remote = FakeRemoteVolume(volume = 0.3)
            val controller = PlaybackVolumeController(exoPlayer, CoroutineScope(UnconfinedTestDispatcher(scheduler)))
            val notices = mutableListOf<Unit>()
            val collectJob =
                launch(UnconfinedTestDispatcher(scheduler)) {
                    controller.notices.collect { notices += it }
                }
            controller.bind(remote)
            controller.setCastVolume(0.4f)
            remote.volume = 0.4
            remote.emit()
            advanceTimeBy(3_100)
            assertTrue(controller.castVolumeSupported.value)
            assertEquals(0, notices.size)
            assertEquals(0.4f, controller.castVolume.value, 0.001f)
            collectJob.cancel()
        }

    @Test
    fun `sin eco tras la ventana marca no soportado y avisa una sola vez`() =
        runTest {
            val scheduler = testScheduler
            val remote = FakeRemoteVolume(volume = 0.3, ignoresWrites = true)
            val controller = PlaybackVolumeController(exoPlayer, CoroutineScope(UnconfinedTestDispatcher(scheduler)))
            val notices = mutableListOf<Unit>()
            val collectJob =
                launch(UnconfinedTestDispatcher(scheduler)) {
                    controller.notices.collect { notices += it }
                }
            controller.bind(remote)
            controller.setCastVolume(0.4f)
            advanceTimeBy(PlaybackVolumeController.VOLUME_CONFIRMATION_TIMEOUT_MS + 100)
            assertFalse(controller.castVolumeSupported.value)
            assertEquals(1, notices.size)
            controller.setCastVolume(0.6f)
            advanceTimeBy(PlaybackVolumeController.VOLUME_CONFIRMATION_TIMEOUT_MS + 100)
            assertEquals(1, notices.size)
            collectJob.cancel()
        }

    @Test
    fun `pulsaciones rapidas reinician la ventana sin falso no soportado`() =
        runTest {
            val scheduler = testScheduler
            val remote = FakeRemoteVolume(volume = 0.3, ignoresWrites = true)
            val controller = PlaybackVolumeController(exoPlayer, CoroutineScope(UnconfinedTestDispatcher(scheduler)))
            controller.bind(remote)
            controller.setCastVolume(0.4f)
            advanceTimeBy(2_000)
            controller.setCastVolume(0.5f)
            advanceTimeBy(2_000)
            assertTrue(controller.castVolumeSupported.value)
            advanceTimeBy(PlaybackVolumeController.VOLUME_CONFIRMATION_TIMEOUT_MS)
            assertFalse(controller.castVolumeSupported.value)
        }

    @Test
    fun `bind resetea el soporte de una sesion anterior`() =
        runTest {
            val scheduler = testScheduler
            val controller = PlaybackVolumeController(exoPlayer, CoroutineScope(UnconfinedTestDispatcher(scheduler)))
            controller.bind(FakeRemoteVolume(volume = 0.3, ignoresWrites = true))
            controller.setCastVolume(0.4f)
            advanceTimeBy(PlaybackVolumeController.VOLUME_CONFIRMATION_TIMEOUT_MS + 100)
            assertFalse(controller.castVolumeSupported.value)
            controller.unbind()
            controller.bind(FakeRemoteVolume(volume = 0.2))
            assertTrue(controller.castVolumeSupported.value)
        }

    /** Receptor simulado con eco explícito ([emit]) y opción de ignorar los ajustes. */
    private class FakeRemoteVolume(
        var volume: Double = 1.0,
        var muted: Boolean = false,
        private val ignoresWrites: Boolean = false,
    ) : RemoteVolumeDevice {
        var observing = false
        val writtenVolumes = mutableListOf<Double>()
        private var listener: (() -> Unit)? = null

        override fun readVolume(): Double? = volume

        override fun readMuted(): Boolean = muted

        override fun writeVolume(volume: Double) {
            writtenVolumes += volume
            if (!ignoresWrites) this.volume = volume
        }

        override fun writeMuted(muted: Boolean) {
            this.muted = muted
        }

        override fun observe(onChanged: () -> Unit) {
            observing = true
            listener = onChanged
        }

        override fun stopObserving() {
            observing = false
            listener = null
        }

        fun emit() {
            listener?.invoke()
        }
    }
}
