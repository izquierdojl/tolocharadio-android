package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Invariantes del buffer profundo de episodios (bug/spec 0058, FR-002). */
class EpisodeBufferSpecTest {
    @Test
    fun `el perfil por defecto mantiene dos minutos de fondo y cuatro de techo`() {
        val spec = EpisodeBufferSpec.DEFAULT

        assertEquals(120_000L, spec.minBufferMs)
        assertEquals(240_000L, spec.maxBufferMs)
        assertEquals(2_000L, spec.bufferForPlaybackMs)
        assertEquals(2_500L, spec.bufferForPlaybackAfterRebufferMs)
    }

    @Test
    fun `el minimo no baja de dos minutos`() {
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeBufferSpec(minBufferMs = 60_000L)
        }
    }

    @Test
    fun `el techo no puede ser menor que el minimo`() {
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeBufferSpec(minBufferMs = 180_000L, maxBufferMs = 120_000L)
        }
    }

    @Test
    fun `el minimo debe cubrir los umbrales de arranque y rebuffer`() {
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeBufferSpec(minBufferMs = 120_000L, bufferForPlaybackMs = 130_000L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeBufferSpec(minBufferMs = 120_000L, bufferForPlaybackAfterRebufferMs = 130_000L)
        }
    }

    @Test
    fun `los umbrales deben ser positivos y no retrasar el arranque`() {
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeBufferSpec(bufferForPlaybackMs = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeBufferSpec(bufferForPlaybackAfterRebufferMs = 0L)
        }
        assertEquals(true, EpisodeBufferSpec.DEFAULT.bufferForPlaybackMs <= 3_000L)
    }
}
