package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.support.testProgress
import org.junit.Assert.assertEquals
import org.junit.Test

/** Regla de reanudación de episodios (FR-001–FR-003, Red→Green). */
class EpisodeResumeTest {
    @Test
    fun `progreso parcial devuelve la posicion guardada`() {
        assertEquals(120L, resumePositionSec(testProgress(120L, 1_800L), 1_800L))
    }

    @Test
    fun `episodio completado empieza desde cero`() {
        assertEquals(0L, resumePositionSec(testProgress(1_800L, 1_800L, completed = true), 1_800L))
    }

    @Test
    fun `sin progreso empieza desde cero`() {
        assertEquals(0L, resumePositionSec(null, 1_800L))
    }

    @Test
    fun `posicion negativa o cero se considera invalida`() {
        assertEquals(0L, resumePositionSec(testProgress(-5L, 1_800L), 1_800L))
        assertEquals(0L, resumePositionSec(testProgress(0L, 1_800L), 1_800L))
    }

    @Test
    fun `posicion igual o mayor que la duracion se considera invalida`() {
        assertEquals(0L, resumePositionSec(testProgress(1_800L, 1_800L), 1_800L))
        assertEquals(0L, resumePositionSec(testProgress(2_000L, 1_800L), 1_800L))
    }

    @Test
    fun `sin duracion conocida se reanuda en la posicion`() {
        assertEquals(90L, resumePositionSec(testProgress(90L, null), null))
    }

    @Test
    fun `resumePoint expone milisegundos y origen`() {
        val point = resumePoint(11L, testProgress(120L, 1_800L), 1_800L, ResumeSource.REMOTE)
        assertEquals(11L, point.episodeId)
        assertEquals(120_000L, point.positionMs)
        assertEquals(ResumeSource.REMOTE, point.source)
    }

    @Test
    fun `resumePoint sin posicion valida marca NONE`() {
        val point = resumePoint(11L, testProgress(1_800L, 1_800L, completed = true), 1_800L, ResumeSource.EMBEDDED)
        assertEquals(0L, point.positionMs)
        assertEquals(ResumeSource.NONE, point.source)
    }
}
