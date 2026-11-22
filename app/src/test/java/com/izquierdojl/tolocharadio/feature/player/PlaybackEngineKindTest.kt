package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import com.izquierdojl.tolocharadio.domain.playback.ContentKind
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Buffer por tipo de contenido: radio intacta y episodios con 2–4 min (spec 0058, FR-001/FR-003). */
class PlaybackEngineKindTest {
    private val live: ExoPlayer = mockk(relaxed = true)
    private val vod: ExoPlayer = mockk(relaxed = true)
    private val factory: PlaybackPlayerFactory =
        mockk {
            every { create(BufferProfile.BALANCED, ContentKind.LIVE) } returns live
            every { create(BufferProfile.BALANCED, ContentKind.VOD) } returns vod
        }

    @Test
    fun `ensureKind VOD usa el player de episodios y conserva el perfil`() {
        val engine = PlaybackEngine(factory, BufferProfile.BALANCED)

        assertSame(live, engine.player)
        assertSame(vod, engine.ensureKind(ContentKind.VOD))

        assertSame(ContentKind.VOD, engine.activeKind)
        assertEquals(BufferProfile.BALANCED, engine.currentProfile)
        assertSame(live, engine.takePendingRelease())
        verify { live.stop() }
        verify(exactly = 0) { live.release() }
    }

    @Test
    fun `ensureKind es idempotente`() {
        val engine = PlaybackEngine(factory, BufferProfile.BALANCED)
        engine.ensureKind(ContentKind.VOD)

        assertSame(vod, engine.ensureKind(ContentKind.VOD))

        verify(exactly = 1) { factory.create(BufferProfile.BALANCED, ContentKind.VOD) }
    }

    @Test
    fun `volver a LIVE crea un player de radio nuevo`() {
        val secondLive: ExoPlayer = mockk(relaxed = true)
        every { factory.create(BufferProfile.BALANCED, ContentKind.LIVE) } returnsMany
            listOf(live, secondLive)
        val engine = PlaybackEngine(factory, BufferProfile.BALANCED)
        engine.ensureKind(ContentKind.VOD)

        assertSame(secondLive, engine.ensureKind(ContentKind.LIVE))
        assertSame(ContentKind.LIVE, engine.activeKind)
        assertSame(vod, engine.takePendingRelease())
    }
}
