package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.MimeTypes
import com.izquierdojl.tolocharadio.domain.playback.PlaybackSource
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationMediaItemFactoryTest {
    private val factory = StationMediaItemFactory(mockk(relaxed = true))
    private val base = "https://radio.test/"

    @Test
    fun `progresivo usa la url del proxy sin mime`() {
        val source = PlaybackSource("s1", hls = false)
        assertEquals("https://radio.test/api/v1/playback/s1", factory.uriFor(source, base))
        assertNull(factory.mimeTypeFor(source))
    }

    @Test
    fun `hls usa la url del proxy y mime HLS`() {
        val source = PlaybackSource("s2", hls = true)
        assertEquals("https://radio.test/api/v1/playback/s2", factory.uriFor(source, base))
        assertEquals(MimeTypes.APPLICATION_M3U8, factory.mimeTypeFor(source))
    }
}
