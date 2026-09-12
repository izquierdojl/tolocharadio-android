package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistParserTest {
    private val base = "https://host/dir/list.m3u"

    @Test
    fun `m3u ignora comentarios y toma la url`() {
        val text = "#EXTM3U\n#EXTINF:-1,Radio\nhttps://stream.example.com/live.mp3\n"
        val result = PlaylistParser.parse(text, base, PlaylistFormat.M3U)
        assertEquals(
            listOf("https://stream.example.com/live.mp3"),
            (result as PlaylistParseResult.Candidates).urls,
        )
    }

    @Test
    fun `m3u resuelve relativas contra la url final`() {
        val result = PlaylistParser.parse("stream.mp3\n", base, PlaylistFormat.M3U)
        assertEquals(listOf("https://host/dir/stream.mp3"), (result as PlaylistParseResult.Candidates).urls)
    }

    @Test
    fun `m3u descarta http y marca insecure only`() {
        val result = PlaylistParser.parse("http://host/live.mp3\n", base, PlaylistFormat.M3U)
        assertTrue(result is PlaylistParseResult.InsecureOnly)
    }

    @Test
    fun `m3u deduplica preservando orden`() {
        val text = "https://a/1.mp3\nhttps://b/2.mp3\nhttps://a/1.mp3\n"
        val result = PlaylistParser.parse(text, base, PlaylistFormat.M3U)
        assertEquals(listOf("https://a/1.mp3", "https://b/2.mp3"), (result as PlaylistParseResult.Candidates).urls)
    }

    @Test
    fun `m3u vacio o solo comentarios es no entries`() {
        assertTrue(PlaylistParser.parse("#EXTM3U\n", base, PlaylistFormat.M3U) is PlaylistParseResult.NoEntries)
        assertTrue(PlaylistParser.parse("", base, PlaylistFormat.M3U) is PlaylistParseResult.NoEntries)
    }

    @Test
    fun `m3u con texto no reconocible es malformed`() {
        assertTrue(PlaylistParser.parse("basura\n", base, PlaylistFormat.M3U) is PlaylistParseResult.Malformed)
    }

    @Test
    fun `pls ordena por indice e ignora titulos`() {
        val text = "[playlist]\nFile2=https://b/2.mp3\nTitle1=x\nFile1=https://a/1.mp3\n"
        val result = PlaylistParser.parse(text, base, PlaylistFormat.PLS)
        assertEquals(listOf("https://a/1.mp3", "https://b/2.mp3"), (result as PlaylistParseResult.Candidates).urls)
    }

    @Test
    fun `pls sin entradas ni marcador es malformed`() {
        assertTrue(PlaylistParser.parse("contenido raro\n", base, PlaylistFormat.PLS) is PlaylistParseResult.Malformed)
    }
}
