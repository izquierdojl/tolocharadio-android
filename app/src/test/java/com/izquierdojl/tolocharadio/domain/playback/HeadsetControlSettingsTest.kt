package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Opciones, defaults y saneado de los controles de auricular (spec 0060). */
class HeadsetControlSettingsTest {
    @Test
    fun `defaults 15 30 y gestos activos`() {
        val settings = HeadsetControlSettings.DEFAULT
        assertEquals(15, settings.skipBackSeconds)
        assertEquals(30, settings.skipForwardSeconds)
        assertEquals(true, settings.gesturesEnabled)
        assertEquals(15_000L, settings.skipBackMs)
        assertEquals(30_000L, settings.skipForwardMs)
    }

    @Test
    fun `opciones canonicas de 5 a 60`() {
        assertEquals(listOf(5, 10, 15, 20, 30, 45, 60), HeadsetControlSettings.OPTIONS_SECONDS)
    }

    @Test
    fun `valores invalidos caen al default de cada campo`() {
        assertEquals(15, HeadsetControlSettings.sanitizeBackSeconds(7))
        assertEquals(15, HeadsetControlSettings.sanitizeBackSeconds(null))
        assertEquals(30, HeadsetControlSettings.sanitizeForwardSeconds(120))
        assertEquals(30, HeadsetControlSettings.sanitizeForwardSeconds(null))
        assertEquals(5, HeadsetControlSettings.sanitizeBackSeconds(5))
        assertEquals(60, HeadsetControlSettings.sanitizeForwardSeconds(60))
    }

    @Test
    fun `fromPersisted sanea y conserva el interruptor`() {
        val settings =
            HeadsetControlSettings.fromPersisted(
                backSeconds = 7,
                forwardSeconds = 60,
                gesturesEnabled = false,
            )
        assertEquals(15, settings.skipBackSeconds)
        assertEquals(60, settings.skipForwardSeconds)
        assertFalse(settings.gesturesEnabled)
    }
}
