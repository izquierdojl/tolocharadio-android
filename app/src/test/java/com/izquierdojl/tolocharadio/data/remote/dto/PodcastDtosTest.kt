package com.izquierdojl.tolocharadio.data.remote.dto

import com.izquierdojl.tolocharadio.core.network.TolochaJson
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Serialización de DTOs podcast contra ejemplos del contrato web (T009, Red). */
class PodcastDtosTest {
    private val json: Json = TolochaJson

    @Test
    fun `PodcastSummary ignora campos nuevos del backend`() {
        val raw =
            """
            {"id":7,"feedUrl":"https://a.example/f.xml","title":"Tolocha",
             "author":"JL","isSubscribed":true,"episodeCount":3,"categories":["rock"],
             "campoFuturo":"x"}
            """.trimIndent()
        val s = json.decodeFromString<PodcastSummary>(raw)
        assertEquals(7L, s.id)
        assertEquals("https://a.example/f.xml", s.feedUrl)
        assertTrue(s.isSubscribed)
        assertEquals(listOf("rock"), s.categories)
    }

    @Test
    fun `PodcastEpisode con progreso redondea el mapeo`() {
        val raw =
            """
            {"id":11,"podcastId":7,"title":"E1","publishedAt":1700000000000,
             "durationSec":3600,"explicit":false,
             "progress":{"positionSec":120,"durationSec":3600,"completed":false,
                         "completedAt":null,"updatedAt":1700000100000},
             "isFavorite":true}
            """.trimIndent()
        val e = json.decodeFromString<PodcastEpisode>(raw)
        assertEquals(11L, e.id)
        assertEquals(120L, e.progress?.positionSec)
        assertFalse(e.progress?.completed ?: true)
        assertTrue(e.isFavorite)
    }

    @Test
    fun `ReportEpisodeProgressBody envia listenedMs y completed false`() {
        val body =
            ReportEpisodeProgressBody(
                sessionId = "s1",
                sequence = 1,
                positionSec = 0,
                listenedMs = 0,
                durationSec = 100,
                completed = false,
                source = "android",
            )
        val encoded = json.encodeToString(ReportEpisodeProgressBody.serializer(), body)
        assertTrue(encoded.contains("\"listenedMs\":0"))
        assertTrue(encoded.contains("\"completed\":false"))
        assertTrue(encoded.contains("\"source\":\"android\""))
    }

    @Test
    fun `PodcastQueue conserva revision y limite`() {
        val raw = """{"revision":9,"updatedAt":null,"limit":200,"items":[]}"""
        val q = json.decodeFromString<PodcastQueue>(raw)
        assertEquals(9L, q.revision)
        assertEquals(200, q.limit)
    }

    @Test
    fun `IssuedToken lee url y vencimiento`() {
        val raw = """{"url":"https://cdn.example/e.mp3?sig=abc","expiresAt":1799999999}"""
        val t = json.decodeFromString<IssuedToken>(raw)
        assertEquals("https://cdn.example/e.mp3?sig=abc", t.url)
        assertEquals(1799999999L, t.expiresAt)
    }

    @Test
    fun `BulkSubscriptionResult informa importados y errores`() {
        val raw =
            """
            {"imported":2,"skipped":1,"failed":1,"items":[],
             "errors":[{"feedUrl":"https://x.example/f","message":"feed inválido"}]}
            """.trimIndent()
        val r = json.decodeFromString<BulkSubscriptionResult>(raw)
        assertEquals(2, r.imported)
        assertEquals(1, r.failed)
        assertEquals("https://x.example/f", r.errors.single().feedUrl)
    }
}
