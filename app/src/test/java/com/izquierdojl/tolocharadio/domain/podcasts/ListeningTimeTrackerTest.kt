package com.izquierdojl.tolocharadio.domain.podcasts

import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningTimeTrackerTest {
    @Test
    fun `suma solo el avance continuo mientras se reproduce`() {
        val tracker = ListeningTimeTracker()

        tracker.onProgress(0L, playing = true)
        tracker.onProgress(1_000L, playing = true)
        tracker.onProgress(2_000L, playing = true)
        tracker.onProgress(3_000L, playing = true)

        assertEquals(3_000L, tracker.totalMs)
    }

    @Test
    fun `los saltos hacia delante no inflan el tiempo escuchado`() {
        val tracker = ListeningTimeTracker()
        tracker.onProgress(0L, playing = true)
        tracker.onProgress(1_000L, playing = true)

        // Salto de la barra de progreso hasta el minuto 20.
        tracker.onProgress(1_200_000L, playing = true)
        tracker.onProgress(1_201_000L, playing = true)

        assertEquals(2_000L, tracker.totalMs)
    }

    @Test
    fun `el retroceso no resta y la reescucha vuelve a contar`() {
        val tracker = ListeningTimeTracker()
        tracker.onProgress(10_000L, playing = true)
        tracker.onProgress(11_000L, playing = true)

        // Volver al minuto uno no descuenta.
        tracker.onProgress(1_000L, playing = true)
        tracker.onProgress(2_000L, playing = true)

        assertEquals(2_000L, tracker.totalMs)
    }

    @Test
    fun `las muestras en pausa no suman tiempo`() {
        val tracker = ListeningTimeTracker()
        tracker.onProgress(0L, playing = true)
        tracker.onProgress(5_000L, playing = false)
        tracker.onProgress(5_000L, playing = false)
        tracker.onProgress(6_000L, playing = true)

        assertEquals(1_000L, tracker.totalMs)
    }

    @Test
    fun `reset empieza una sesion nueva`() {
        val tracker = ListeningTimeTracker()
        tracker.onProgress(0L, playing = true)
        tracker.onProgress(2_000L, playing = true)

        tracker.reset()
        tracker.onProgress(500_000L, playing = true)
        tracker.onProgress(501_000L, playing = true)

        assertEquals(1_000L, tracker.totalMs)
    }

    @Test
    fun `synchronize recupera el ultimo tramo sin contar saltos`() {
        val tracker = ListeningTimeTracker()
        tracker.onProgress(0L, playing = true)
        tracker.onProgress(1_000L, playing = true)

        tracker.synchronize(3_000L)
        assertEquals(3_000L, tracker.totalMs)

        tracker.synchronize(600_000L)
        assertEquals(3_000L, tracker.totalMs)
    }
}
