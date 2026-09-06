package com.example.tolocharadio.feature.player

import com.example.tolocharadio.data.remote.dto.StationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Segunda línea del panel + enlace copiable (spec 004, FR-002/FR-006). */
class PanelHelpersTest {
    private fun station(
        country: String? = null,
        language: String? = null,
        codec: String? = null,
        bitrate: Int? = null,
        url: String = "",
    ) = StationDto(
        id = "u1",
        name = "Tolocha",
        url = url,
        country = country,
        language = language,
        codec = codec,
        bitrate = bitrate,
    )

    @Test
    fun `subtitulo con todos los datos`() {
        assertEquals(
            "España · Español · MP3 128 kbps",
            panelSubtitle(station(country = "España", language = "Español", codec = "MP3", bitrate = 128)),
        )
    }

    @Test
    fun `subtitulo omite lo que falta`() {
        assertEquals("España", panelSubtitle(station(country = "España")))
        assertEquals("MP3", panelSubtitle(station(codec = "MP3")))
        assertEquals("128 kbps", panelSubtitle(station(bitrate = 128)))
    }

    @Test
    fun `subtitulo sin datos muestra fallback`() {
        assertEquals("Emisora de radio", panelSubtitle(station()))
        assertEquals("Emisora de radio", panelSubtitle(station(bitrate = 0)))
    }

    @Test
    fun `copiar devuelve la url original`() {
        assertEquals(
            "https://stream.test/radio.mp3",
            resolveCopyLink(station(url = "https://stream.test/radio.mp3")),
        )
    }

    @Test
    fun `copiar con url vacia devuelve null`() {
        assertNull(resolveCopyLink(station(url = "")))
        assertNull(resolveCopyLink(station(url = "   ")))
    }
}
