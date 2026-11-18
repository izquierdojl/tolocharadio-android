package com.izquierdojl.tolocharadio.domain.podcasts

import org.junit.Assert.assertEquals
import org.junit.Test

class AbsoluteUrlTest {
    @Test
    fun `absoluta se conserva`() {
        assertEquals(
            "https://cdn.example/e.mp3",
            absoluteUrl("https://srv.example", "https://cdn.example/e.mp3"),
        )
    }

    @Test
    fun `relativa se resuelve contra el servidor`() {
        assertEquals(
            "https://srv.example/api/v1/e?sig=x",
            absoluteUrl("https://srv.example/", "/api/v1/e?sig=x"),
        )
    }
}
