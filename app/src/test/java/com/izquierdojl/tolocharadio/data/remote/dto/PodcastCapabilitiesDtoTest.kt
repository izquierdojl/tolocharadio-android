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
        assertEquals(200, c.podcastQueueMax())
    }

    @Test
    fun `config con features activa lo publicado`() {
        val raw =
            """
            {"appName":"Tolocha","features":{"podcasts":true,"podcastDownloads":true,
             "podcastQueue":true,"podcastFavorites":true,"podcastStats":true,"podcastOpml":true},
             "limits":{"podcastQueueMax":50}}
            """.trimIndent()
        val c = TolochaJson.decodeFromString<AppConfigDto>(raw)
        assertTrue(c.podcastsEnabled())
        assertTrue(c.podcastDownloadsEnabled())
        assertEquals(50, c.podcastQueueMax())
    }
}
