package com.izquierdojl.tolocharadio.domain.podcasts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeTimeTest {
    @Test
    fun `remainingMs descuenta la posicion y nunca es negativo`() {
        assertEquals(180_000L, remainingMs(120_000L, 300_000L))
        assertEquals(0L, remainingMs(400_000L, 300_000L))
        assertEquals(300_000L, remainingMs(-1_000L, 300_000L))
        assertNull(remainingMs(120_000L, 0L))
    }

    @Test
    fun `progressFraction se acota entre cero y uno`() {
        assertEquals(0.4f, progressFraction(120_000L, 300_000L) ?: -1f, 0.0001f)
        assertEquals(1f, progressFraction(400_000L, 300_000L) ?: -1f, 0.0001f)
        assertEquals(0f, progressFraction(-10L, 300_000L) ?: -1f, 0.0001f)
        assertNull(progressFraction(120_000L, 0L))
    }

    @Test
    fun `remainingWholeMinutes redondea hacia arriba`() {
        assertEquals(3L, remainingWholeMinutes(120L, 300L))
        assertEquals(1L, remainingWholeMinutes(241L, 300L))
        assertEquals(2L, remainingWholeMinutes(239L, 300L))
        assertEquals(1L, remainingWholeMinutes(299L, 300L))
    }

    @Test
    fun `remainingWholeMinutes es nulo sin progreso o sin duracion`() {
        assertNull(remainingWholeMinutes(0L, 300L))
        assertNull(remainingWholeMinutes(300L, 300L))
        assertNull(remainingWholeMinutes(400L, 300L))
        assertNull(remainingWholeMinutes(120L, null))
        assertNull(remainingWholeMinutes(120L, 0L))
    }

    @Test
    fun `remainingLabel distingue menos de un minuto`() {
        assertEquals("Quedan 3 min", remainingLabel(120L, 300L))
        assertEquals("Queda menos de 1 min", remainingLabel(1_701L, 1_760L))
        assertEquals("Quedan 1 min", remainingLabel(1_700L, 1_760L))
        assertNull(remainingLabel(0L, 1_760L))
        assertNull(remainingLabel(1_760L, 1_760L))
    }
}
