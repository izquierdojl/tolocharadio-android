package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlaybackEngineTest {
    private val first: ExoPlayer = mockk(relaxed = true)
    private val second: ExoPlayer = mockk(relaxed = true)
    private val factory: PlaybackPlayerFactory =
        mockk {
            every { create(BufferProfile.BALANCED) } returns first
            every { create(BufferProfile.MAX_STABILITY) } returns second
        }

    @Test
    fun `ensureProfile reutiliza el player si el perfil no cambia`() {
        val engine = PlaybackEngine(factory, BufferProfile.BALANCED)
        assertSame(first, engine.ensureProfile(BufferProfile.BALANCED))
        verify(exactly = 0) { first.release() }
    }

    @Test
    fun `ensureProfile recrea y libera el anterior si el perfil cambia`() {
        val engine = PlaybackEngine(factory, BufferProfile.BALANCED)
        assertSame(second, engine.ensureProfile(BufferProfile.MAX_STABILITY))
        assertEquals(BufferProfile.MAX_STABILITY, engine.currentProfile)
        verify { first.stop() }
        verify { first.release() }
    }

    @Test
    fun `swap devuelve el player anterior y el nuevo sin liberarlos`() {
        val engine = PlaybackEngine(factory, BufferProfile.BALANCED)
        val swap = engine.swap(BufferProfile.MAX_STABILITY)
        assertSame(first, swap.old)
        assertSame(second, swap.new)
        assertSame(second, engine.player)
        verify(exactly = 0) { first.release() }
        verify(exactly = 0) { second.release() }
    }
}
