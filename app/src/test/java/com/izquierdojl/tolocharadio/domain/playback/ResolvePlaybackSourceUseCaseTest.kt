package com.izquierdojl.tolocharadio.domain.playback

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolvePlaybackSourceUseCaseTest {
    private val resolve = ResolvePlaybackSourceUseCase()

    @Test
    fun `stream directo usa proxy sin hls`() {
        assertEquals(PlaybackSource("s1", hls = false), resolve(station("https://host/live.mp3")))
    }

    @Test
    fun `m3u8 usa proxy con hls`() {
        assertEquals(PlaybackSource("s1", hls = true), resolve(station("https://host/live.m3u8")))
    }

    @Test
    fun `m3u y pls usan proxy sin hls`() {
        assertEquals(PlaybackSource("s1", hls = false), resolve(station("https://host/list.m3u")))
        assertEquals(PlaybackSource("s1", hls = false), resolve(station("https://host/list.pls")))
    }

    @Test
    fun `emisora personalizada usa el mismo pipeline`() {
        assertEquals(
            PlaybackSource("s1", hls = false),
            resolve(station("https://host/list.m3u", custom = true)),
        )
    }

    private fun station(
        url: String,
        custom: Boolean = false,
    ) = StationDto(id = "s1", name = "Emisora", url = url, isCustom = custom)
}
