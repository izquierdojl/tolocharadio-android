package com.izquierdojl.tolocharadio.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Presets de periodo y granularidad automática (FR-003/FR-004, R5):
 * ≤ 62 días → día, ≤ 370 → semana, si no → mes; sin fechas → 30 días
 * (paridad con `daysBetween`/`autoGranularity` de la web).
 */
class StatsRangeTest {
    private fun rangeOf(days: Int): StatsRange {
        val to = LocalDate.parse("2026-10-02")
        val from = to.minusDays((days - 1).toLong())
        return StatsRange(from.toString(), to.toString())
    }

    @Test
    fun `el preset de 30 dias genera un rango inclusivo de 30 dias`() {
        val range = StatsPeriod.THIRTY.range(LocalDate.parse("2026-10-02"))
        assertEquals("2026-09-03", range.from)
        assertEquals("2026-10-02", range.to)
    }

    @Test
    fun `el preset todo el historico no envia fechas`() {
        val range = StatsPeriod.ALL.range(LocalDate.parse("2026-10-02"))
        assertNull(range.from)
        assertNull(range.to)
    }

    @Test
    fun `limite de granularidad dia hasta 62 dias`() {
        assertEquals(TimelineGranularity.DAY, rangeOf(62).autoGranularity())
    }

    @Test
    fun `limite de granularidad semana de 63 a 370 dias`() {
        assertEquals(TimelineGranularity.WEEK, rangeOf(63).autoGranularity())
        assertEquals(TimelineGranularity.WEEK, rangeOf(370).autoGranularity())
    }

    @Test
    fun `limite de granularidad mes desde 371 dias`() {
        assertEquals(TimelineGranularity.MONTH, rangeOf(371).autoGranularity())
    }

    @Test
    fun `rango sin fechas usa 30 dias por defecto (paridad con la web)`() {
        assertEquals(TimelineGranularity.DAY, StatsRange(null, null).autoGranularity())
    }
}
