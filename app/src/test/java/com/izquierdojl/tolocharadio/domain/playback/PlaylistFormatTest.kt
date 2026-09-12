package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistFormatTest {
    @Test
    fun `detecta m3u8 como HLS`() {
        assertEquals(PlaylistFormat.HLS, PlaylistFormat.detect("https://host/live.m3u8"))
    }

    @Test
    fun `detecta m3u y pls`() {
        assertEquals(PlaylistFormat.M3U, PlaylistFormat.detect("https://host/lista.m3u"))
        assertEquals(PlaylistFormat.PLS, PlaylistFormat.detect("https://host/lista.pls"))
    }

    @Test
    fun `ignora query y mayusculas`() {
        assertEquals(PlaylistFormat.HLS, PlaylistFormat.detect("https://host/live.M3U8?token=1"))
    }

    @Test
    fun `stream directo o invalido devuelve null`() {
        assertNull(PlaylistFormat.detect("https://host/live.mp3"))
        assertNull(PlaylistFormat.detect(""))
        assertNull(PlaylistFormat.detect("no es una url"))
        assertNull(PlaylistFormat.detect("https://host/"))
    }
}
