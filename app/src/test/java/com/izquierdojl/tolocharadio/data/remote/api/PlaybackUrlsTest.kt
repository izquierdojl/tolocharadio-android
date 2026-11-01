package com.izquierdojl.tolocharadio.data.remote.api

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackUrlsTest {
    private val base = "https://radio.test/"

    @Test
    fun `la ruta continua anade live y el marcador`() {
        assertEquals(
            "https://radio.test/api/v1/playback/s1/live",
            liveStreamUrl(base, "s1"),
        )
        assertEquals(
            "https://radio.test/api/v1/playback/s1/live?r=3",
            liveStreamUrl(base, "s1", 3),
        )
    }

    @Test
    fun `la ruta legacy conserva el path y anade el marcador`() {
        assertEquals(
            "https://radio.test/api/v1/playback/s1",
            streamUrl(base, "s1"),
        )
        assertEquals(
            "https://radio.test/api/v1/playback/s1?r=0",
            streamUrl(base, "s1", 0),
        )
    }
}
