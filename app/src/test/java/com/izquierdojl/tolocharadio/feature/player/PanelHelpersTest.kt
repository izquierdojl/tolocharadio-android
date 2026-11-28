package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Enlace copiable (spec 004, FR-006). */
class PanelHelpersTest {
    private fun station(url: String = "") =
        StationDto(
            id = "u1",
            name = "Tolocha",
            url = url,
        )

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
