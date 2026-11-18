package com.izquierdojl.tolocharadio.domain.podcasts

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeStateTest {
    @Test
    fun `posicion cero sin completar es sin empezar`() {
        assertEquals(EpisodeProgressState.NOT_STARTED, progressState(0, false))
    }

    @Test
    fun `posicion positiva sin completar es en progreso`() {
        assertEquals(EpisodeProgressState.IN_PROGRESS, progressState(42, false))
    }

    @Test
    fun `completado manda sobre la posicion`() {
        assertEquals(EpisodeProgressState.COMPLETED, progressState(0, true))
        assertEquals(EpisodeProgressState.COMPLETED, progressState(99, true))
    }
}
