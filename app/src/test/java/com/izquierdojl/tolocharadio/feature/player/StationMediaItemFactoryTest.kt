package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.MimeTypes
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.playback.PlaybackSource
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class StationMediaItemFactoryTest {
    private val factory = StationMediaItemFactory(mockk(relaxed = true))
    private val base = "https://radio.test/"

    private fun station(url: String) = StationDto(id = "s1", name = "Radio", url = url)

    @Test
    fun `progresivo usa la url del proxy y mime de audio`() {
        val source = PlaybackSource("s1", hls = false)
        assertEquals("https://radio.test/api/v1/playback/s1", factory.uriFor(source, base))
        assertEquals(
            MimeTypes.AUDIO_MPEG,
            factory.mimeTypeFor(station("https://host/live.mp3"), source),
        )
    }

    @Test
    fun `progresivo sin extension cae a audio mpeg`() {
        val source = PlaybackSource("s1", hls = false)
        assertEquals(
            MimeTypes.AUDIO_MPEG,
            factory.mimeTypeFor(station("https://host/live"), source),
        )
    }

    @Test
    fun `progresivo aac usa audio aac`() {
        val source = PlaybackSource("s1", hls = false)
        assertEquals(
            MimeTypes.AUDIO_AAC,
            factory.mimeTypeFor(station("https://host/live.aac"), source),
        )
    }

    @Test
    fun `hls usa la url del proxy y mime HLS`() {
        val source = PlaybackSource("s2", hls = true)
        assertEquals("https://radio.test/api/v1/playback/s2", factory.uriFor(source, base))
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            factory.mimeTypeFor(station("https://host/live.m3u8"), source),
        )
    }

    @Test
    fun `cast usa la url publica de la emisora`() {
        val url = "https://stream.example.com/live.mp3"
        assertEquals(url, factory.castUriFor(station(url), base))
    }

    @Test
    fun `cast cae al proxy si la emisora no tiene url`() {
        assertEquals("https://radio.test/api/v1/playback/s1", factory.castUriFor(station(""), base))
    }
}
