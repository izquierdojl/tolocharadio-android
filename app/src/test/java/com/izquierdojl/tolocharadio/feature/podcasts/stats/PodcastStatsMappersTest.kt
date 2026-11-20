package com.izquierdojl.tolocharadio.feature.podcasts.stats

import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.support.testEpisode
import com.izquierdojl.tolocharadio.support.testPodcastCategories
import com.izquierdojl.tolocharadio.support.testPodcastHabits
import com.izquierdojl.tolocharadio.support.testPodcastRecents
import com.izquierdojl.tolocharadio.support.testPodcastStatsSummary
import com.izquierdojl.tolocharadio.support.testPodcastTimeline
import com.izquierdojl.tolocharadio.support.testPodcastTopEpisodes
import com.izquierdojl.tolocharadio.support.testPodcastTopPodcasts
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastStatsMappersTest {
    private val podcast =
        PodcastSummary(
            id = 7L,
            feedUrl = "https://feed.example/7",
            title = "Programa de prueba",
        )

    @Test
    fun `resumen se convierte en tarjetas`() {
        val items =
            testPodcastStatsSummary(
                totalMs = 3_600_000L,
                podcastCount = 2,
                startedEpisodes = 3,
                completedEpisodes = 2,
                completionRate = 0.66,
                avgMsPerEpisode = 1_200_000L,
            ).toSummaryItems()

        assertEquals(3, items.size)
        assertEquals("Tiempo total", items[0].label)
        assertEquals("1 h", items[0].value)
        assertEquals("Episodios", items[1].label)
        assertEquals("3 empezados · 2 completados", items[1].value)
        assertEquals("Tasa de finalización: 66 %", items[1].hint)
        assertEquals("Programas", items[2].label)
        assertEquals("2", items[2].value)
    }

    @Test
    fun `timeline y habitos se adaptan a las primitivas de radio`() {
        val timeline = testPodcastTimeline("day", "2026-11-19" to 1_000L).toStatsTimeline()
        assertEquals("day", timeline.granularity)
        assertEquals("2026-11-19", timeline.items.single().bucket)
        assertEquals(1_000L, timeline.items.single().totalMs)

        val habits = testPodcastHabits(Triple(1, 8, 2_000L)).toStatsHabits()
        assertEquals(1, habits.items.single().weekday)
        assertEquals(8, habits.items.single().hour)
        assertEquals(2_000L, habits.items.single().totalMs)
    }

    @Test
    fun `rankings categorias y recientes conservan etiquetas y tiempos`() {
        val topPodcasts = testPodcastTopPodcasts(podcast to 5_000L).toRankingItems()
        assertEquals("Programa de prueba", topPodcasts.single().label)
        assertEquals(5_000L, topPodcasts.single().totalMs)

        val episode = testEpisode(id = 1L, title = "Episodio 1")
        val topEpisodes = testPodcastTopEpisodes(episode to 4_000L).toRankingItems()
        assertEquals("Episodio 1", topEpisodes.single().label)

        val categories = testPodcastCategories("Ciencia" to 3_000L).toShareItems()
        assertEquals("Ciencia", categories.single().label)

        val recent =
            testPodcastRecents(
                episode to 2_000L,
                null to 1_000L,
            ).toRecentItems()
        assertEquals(listOf("Episodio 1", "Episodio"), recent.map { it.label })
    }
}
