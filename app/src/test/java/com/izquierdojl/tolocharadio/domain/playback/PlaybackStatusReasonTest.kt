package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaybackStatusReasonTest {
    @Test
    fun `cada codigo de playlist tiene su mensaje`() {
        assertEquals(
            "La lista de la emisora no contiene ninguna emisión reproducible.",
            PlaybackStatusReason.reasonToMessage(PlaybackStatusReason.PLAYLIST_EMPTY),
        )
        assertEquals(
            "La emisora solo ofrece conexiones no seguras (HTTP), bloqueadas por la app.",
            PlaybackStatusReason.reasonToMessage(PlaybackStatusReason.PLAYLIST_INSECURE_ONLY),
        )
        assertEquals(
            "La lista de la emisora no tiene un formato válido.",
            PlaybackStatusReason.reasonToMessage(PlaybackStatusReason.PLAYLIST_MALFORMED),
        )
        assertEquals(
            "No se pudo descargar la lista de la emisora. Comprueba tu conexión.",
            PlaybackStatusReason.reasonToMessage(PlaybackStatusReason.PLAYLIST_UNREACHABLE),
        )
        assertEquals(
            "La emisora no está disponible en este momento.",
            PlaybackStatusReason.reasonToMessage(PlaybackStatusReason.STREAM_UNAVAILABLE),
        )
    }

    @Test
    fun `motivo desconocido o nulo usa mensaje generico`() {
        assertEquals("Emisora no disponible.", PlaybackStatusReason.reasonToMessage(null))
        assertEquals("Emisora no disponible.", PlaybackStatusReason.reasonToMessage("OTRO_CODIGO"))
        assertNotEquals("OTRO_CODIGO", PlaybackStatusReason.reasonToMessage("OTRO_CODIGO"))
    }
}
