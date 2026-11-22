package com.izquierdojl.tolocharadio.data.remote.dto

import com.izquierdojl.tolocharadio.core.network.TolochaJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Capacidades `/config`: presentes y ausentes (T005, Red). */
class PodcastCapabilitiesDtoTest {
    @Test
    fun `config sin features conserva radio y desactiva podcasts`() {
        val c = TolochaJson.decodeFromString<AppConfigDto>("""{"appName":"Tolocha"}""")
        assertEquals("Tolocha", c.appName)
        assertFalse(c.podcastsEnabled())
        assertFalse(c.podcastDownloadsEnabled())
        assertFalse(c.podcastTopChartsEnabled())
        assertEquals(200, c.podcastQueueMax())
    }

    @Test
    fun `config con features activa lo publicado`() {
        val raw =
            """
            {"appName":"Tolocha","features":{"podcasts":true,"podcastDownloads":true,
             "podcastQueue":true,"podcastFavorites":true,"podcastStats":true,"podcastOpml":true,
             "podcastDiscovery":"itunes"},
             "limits":{"podcastQueueMax":50}}
            """.trimIndent()
        val c = TolochaJson.decodeFromString<AppConfigDto>(raw)
        assertTrue(c.podcastsEnabled())
        assertTrue(c.podcastDownloadsEnabled())
        assertTrue(c.podcastTopChartsEnabled())
        assertEquals(50, c.podcastQueueMax())
    }

    @Test
    fun `el ranking solo esta disponible con podcasts e itunes`() {
        fun config(
            provider: String?,
            podcasts: Boolean = true,
        ): AppConfigDto =
            AppConfigDto(
                appName = "Tolocha",
                features = PodcastFeaturesDto(podcasts = podcasts, podcastDiscovery = provider),
            )

        assertTrue(config("itunes").podcastTopChartsEnabled())
        assertFalse(config("podcastindex").podcastTopChartsEnabled())
        assertFalse(config("none").podcastTopChartsEnabled())
        assertFalse(config(null).podcastTopChartsEnabled())
        assertFalse(config("itunes", podcasts = false).podcastTopChartsEnabled())
    }
}
