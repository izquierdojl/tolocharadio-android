package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.cast.RemoteVolumeDevice
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests Red-Green del controlador de silencio (spec 0037, contracts §2).
 *
 * El volumen deja de ser estado de la app (lo representa la barra del sistema vía
 * player nativo); solo el silencio conserva estado, con **origen**: usuario
 * (botón/notificación) o eco externo. El origen gobierna `unbind()`.
 */
class PlaybackVolumeControllerTest {
    private val exoPlayer: ExoPlayer = mockk(relaxed = true)

    private fun controller() = PlaybackVolumeController(exoPlayer)

    @Test
    fun `bind con app silenciada escribe el silencio al receptor y conserva el origen`() {
        val remote = FakeRemoteVolume()
        val controller = controller()
        controller.toggleMute()
        controller.bind(remote)
        assertTrue(controller.muted.value)
        assertTrue(controller.isRemoteActive)
        assertEquals(listOf(true), remote.writtenMutes)
        controller.unbind()
        // Origen usuario: el silencio se arrastra al teléfono (FR-006).
        verify { exoPlayer.volume = 0f }
        assertTrue(controller.muted.value)
    }

    @Test
    fun `bind con app sin silencio no escribe nada y representa el silencio externo`() {
        val remote = FakeRemoteVolume(muted = true)
        val controller = controller()
        controller.bind(remote)
        assertTrue(remote.writtenMutes.isEmpty())
        assertTrue(controller.muted.value)
    }

    @Test
    fun `bind con app sin silencio y receptor sin silencio no escribe nada`() {
        val remote = FakeRemoteVolume(muted = false)
        val controller = controller()
        controller.bind(remote)
        assertFalse(controller.muted.value)
        assertTrue(remote.writtenMutes.isEmpty())
    }

    @Test
    fun `eco de silencio del receptor sincroniza el estado con origen eco`() {
        val remote = FakeRemoteVolume()
        val controller = controller()
        controller.bind(remote)
        remote.muted = true
        remote.emit()
        assertTrue(controller.muted.value)
        // Origen eco: al desconectar no se arrastra al teléfono (FR-006, edge case B2).
        controller.unbind()
        verify(exactly = 0) { exoPlayer.volume = 0f }
        verify { exoPlayer.volume = 1f }
        assertFalse(controller.muted.value)
        assertFalse(remote.observing)
    }

    @Test
    fun `toggleMute con remoto aplica el silencio del dispositivo`() {
        val remote = FakeRemoteVolume()
        val controller = controller()
        controller.bind(remote)
        controller.toggleMute()
        assertTrue(controller.muted.value)
        assertEquals(listOf(true), remote.writtenMutes)
        controller.toggleMute()
        assertFalse(controller.muted.value)
        assertEquals(listOf(true, false), remote.writtenMutes)
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
    fun `setMuted marca origen usuario aunque el estado venga del eco`() {
        val remote = FakeRemoteVolume()
        val controller = controller()
        controller.bind(remote)
        remote.muted = true
        remote.emit()
        assertTrue(controller.muted.value)
        // El usuario pulsa el botón para des-silenciar: pasa a origen usuario.
        controller.setMuted(false)
        assertEquals(listOf(false), remote.writtenMutes)
        controller.unbind()
        verify { exoPlayer.volume = 1f }
    }

    @Test
    fun `resetMute restablece el sonido`() {
        val remote = FakeRemoteVolume()
        val controller = controller()
        controller.bind(remote)
        controller.toggleMute()
        controller.resetMute()
        assertFalse(controller.muted.value)
        assertEquals(listOf(true, false), remote.writtenMutes)
    }

    @Test
    fun `rebind tras unbind reinicia la observacion`() {
        val first = FakeRemoteVolume(muted = true)
        val controller = controller()
        controller.bind(first)
        assertTrue(controller.muted.value)
        controller.unbind()
        assertFalse(controller.muted.value)
        val second = FakeRemoteVolume()
        controller.bind(second)
        assertTrue(second.observing)
        assertFalse(controller.muted.value)
    }

    /** Receptor simulado con eco explícito ([emit]); solo silencio (contracts §2). */
    private class FakeRemoteVolume(
        var muted: Boolean = false,
    ) : RemoteVolumeDevice {
        var observing = false
        val writtenMutes = mutableListOf<Boolean>()
        private var listener: (() -> Unit)? = null

        override fun readMuted(): Boolean = muted

        override fun writeMuted(muted: Boolean) {
            writtenMutes += muted
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
