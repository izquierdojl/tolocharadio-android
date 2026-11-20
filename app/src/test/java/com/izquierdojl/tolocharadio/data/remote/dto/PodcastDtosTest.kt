package com.izquierdojl.tolocharadio.data.remote.dto

import com.izquierdojl.tolocharadio.core.network.TolochaJson
import com.izquierdojl.tolocharadio.data.remote.api.PodcastCategories
import com.izquierdojl.tolocharadio.data.remote.api.PodcastHabits
import com.izquierdojl.tolocharadio.data.remote.api.PodcastRecents
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopEpisodes
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopPodcasts
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

    @Test
    fun `PodcastStatsSummary lee los agregados reales del backend`() {
        val raw =
            """
            {"totalMs":170000,"episodesStarted":2,"episodesCompleted":1,
             "completionRate":0.5,"podcastsCount":2,"averageEpisodeMs":85000}
            """.trimIndent()
        val s = json.decodeFromString<PodcastStatsSummaryDto>(raw)
        assertEquals(170_000L, s.totalMs)
        assertEquals(2, s.episodesStarted)
        assertEquals(1, s.episodesCompleted)
        assertEquals(0.5, s.completionRate, 0.001)
        assertEquals(2, s.podcastsCount)
        assertEquals(85_000L, s.averageEpisodeMs)
    }

    @Test
    fun `PodcastTopPodcasts lee el ranking plano del backend`() {
        val raw =
            """
            {"items":[{"podcastId":7,"title":"Tech Talks","author":"Ana",
             "imageUrl":null,"totalMs":150000,"episodesStarted":2,"episodesCompleted":1}]}
            """.trimIndent()
        val item = json.decodeFromString<PodcastTopPodcasts>(raw).items.single()
        assertEquals(7L, item.podcastId)
        assertEquals("Tech Talks", item.title)
        assertEquals("Ana", item.author)
        assertEquals(150_000L, item.totalMs)
        assertEquals(2, item.episodesStarted)
        assertEquals(1, item.episodesCompleted)
    }

    @Test
    fun `PodcastTopEpisodes lee el ranking plano del backend`() {
        val raw =
            """
            {"items":[{"episodeId":11,"podcastId":7,"title":"Episodio B",
             "podcastTitle":"Tech Talks","imageUrl":null,"totalMs":90000,"completed":true}]}
            """.trimIndent()
        val item = json.decodeFromString<PodcastTopEpisodes>(raw).items.single()
        assertEquals(11L, item.episodeId)
        assertEquals(7L, item.podcastId)
        assertEquals("Episodio B", item.title)
        assertEquals("Tech Talks", item.podcastTitle)
        assertEquals(90_000L, item.totalMs)
        assertTrue(item.completed)
    }

    @Test
    fun `PodcastRecents lee episodeTitle y listenedMs`() {
        val raw =
            """
            {"items":[{"episodeId":12,"podcastId":8,"episodeTitle":"Episodio C",
             "podcastTitle":"Sin Categoria","imageUrl":null,"startedAt":1700000000000,
             "endedAt":1700000060000,"listenedMs":20000,"positionSec":20,
             "durationSec":600,"completed":false}]}
            """.trimIndent()
        val item = json.decodeFromString<PodcastRecents>(raw).items.single()
        assertEquals(12L, item.episodeId)
        assertEquals("Episodio C", item.episodeTitle)
        assertEquals(20_000L, item.listenedMs)
        assertFalse(item.completed)
    }

    @Test
    fun `timeline habits y categories del backend se deserializan`() {
        val timeline =
            json.decodeFromString<PodcastTimelineDto>(
                """{"granularity":"day","items":[{"bucket":"2026-11-19","totalMs":60000}]}""",
            )
        assertEquals("day", timeline.granularity)
        assertEquals(60_000L, timeline.items.single().totalMs)

        val habits =
            json.decodeFromString<PodcastHabits>(
                """{"items":[{"weekday":1,"hour":12,"totalMs":20000}]}""",
            )
        assertEquals(12, habits.items.single().hour)

        val categories =
            json.decodeFromString<PodcastCategories>(
                """{"items":[{"category":"Tecnología","totalMs":150000}]}""",
            )
        assertEquals("Tecnología", categories.items.single().category)
    }
}
