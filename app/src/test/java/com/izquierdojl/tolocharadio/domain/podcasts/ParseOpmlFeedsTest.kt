package com.izquierdojl.tolocharadio.domain.podcasts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseOpmlFeedsTest {
    private val opml =
        """
        <opml version="2.0"><body>
        <outline text="A" xmlUrl="https://a.example/feed.xml"/>
        <outline text="B" xmlUrl='http://b.example/rss' />
        <outline text="SinUrl" />
        <outline text="A dup" xmlUrl="  https://a.example/feed.xml  "/>
        </body></opml>
        """.trimIndent()

    @Test
    fun `parse extrae xmlUrl unicas recortadas en orden`() {
        assertEquals(
            listOf("https://a.example/feed.xml", "http://b.example/rss"),
            parseOpmlFeedUrls(opml),
        )
    }

    @Test
    fun `parse sin outlines devuelve vacio`() {
        assertEquals(emptyList<String>(), parseOpmlFeedUrls("<opml></opml>"))
    }

    @Test
    fun `feed valida solo http y https no vacias`() {
        assertTrue(isValidFeedUrl("https://a.example/f.xml"))
        assertTrue(isValidFeedUrl("http://a.example/f.xml"))
        assertFalse(isValidFeedUrl(""))
        assertFalse(isValidFeedUrl("   "))
        assertFalse(isValidFeedUrl("ftp://a.example/f.xml"))
        assertFalse(isValidFeedUrl("a.example/f.xml"))
    }
}
