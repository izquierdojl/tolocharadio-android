package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.MimeTypes
import com.izquierdojl.tolocharadio.domain.playback.PlaybackSource
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationMediaItemFactoryTest {
    private val factory = StationMediaItemFactory(mockk(relaxed = true), mockk(relaxed = true))
    private val base = "https://radio.test/"

    @Test
    fun `proxied usa la url del proxy sin mime`() {
        val source = PlaybackSource.Proxied("s1")
        assertEquals("https://radio.test/api/v1/playback/s1", factory.uriFor(source, base))
        assertNull(factory.mimeTypeFor(source))
    }

    @Test
    fun `direct hls usa la url directa y mime HLS`() {
        val source = PlaybackSource.DirectHls("https://host/live.m3u8")
        assertEquals("https://host/live.m3u8", factory.uriFor(source, base))
        assertEquals(MimeTypes.APPLICATION_M3U8, factory.mimeTypeFor(source))
    }

    @Test
    fun `direct progressive usa la url directa sin mime`() {
        val source = PlaybackSource.DirectProgressive("https://host/live.mp3")
        assertEquals("https://host/live.mp3", factory.uriFor(source, base))
        assertNull(factory.mimeTypeFor(source))
    }
}
