package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HlsStationTest {
    @Test
    fun `m3u8 en minusculas es hls`() {
        assertTrue(HlsStation.isHls("https://host/live.m3u8"))
    }

    @Test
    fun `m3u8 en mayusculas es hls`() {
        assertTrue(HlsStation.isHls("https://host/live.M3U8"))
    }

    @Test
    fun `m3u8 con query sigue siendo hls`() {
        assertTrue(HlsStation.isHls("https://host/live.m3u8?token=1"))
    }

    @Test
    fun `formatos que no son m3u8 no son hls`() {
        assertFalse(HlsStation.isHls("https://host/list.m3u"))
        assertFalse(HlsStation.isHls("https://host/list.pls"))
        assertFalse(HlsStation.isHls("https://host/live.mp3"))
        assertFalse(HlsStation.isHls("https://host/live"))
        assertFalse(HlsStation.isHls(""))
        assertFalse(HlsStation.isHls("no es una url"))
    }
}
