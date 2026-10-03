package com.izquierdojl.tolocharadio.feature.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Formato de tiempos y etiquetas en español (espejo de `apps/web/src/lib/stats.ts`, R8). */
class StatsFormatTest {
    @Test
    fun `formatDurationMs cero`() {
        assertEquals("0 min", StatsFormat.formatDurationMs(0))
    }

    @Test
    fun `formatDurationMs minutos redondeados`() {
        assertEquals("1 min", StatsFormat.formatDurationMs(59_000))
        assertEquals("9 min", StatsFormat.formatDurationMs(540_000))
    }

    @Test
    fun `formatDurationMs horas`() {
        assertEquals("1 h", StatsFormat.formatDurationMs(3_600_000))
        assertEquals("1 h 30 min", StatsFormat.formatDurationMs(5_400_000))
        assertEquals("2 h", StatsFormat.formatDurationMs(7_200_000))
    }

    @Test
    fun `formatBucketLabel dia`() {
        val label = StatsFormat.formatBucketLabel("2026-09-02", "day")
        assertTrue(label.startsWith("2 "))
    }

    @Test
    fun `formatBucketLabel semana`() {
        val label = StatsFormat.formatBucketLabel("2026-09-07", "week")
        assertTrue(label.startsWith("sem. "))
    }

    @Test
    fun `formatBucketLabel mes`() {
        val label = StatsFormat.formatBucketLabel("2026-09-02", "month")
        assertTrue(label.contains("2026"))
    }

    @Test
    fun `formatFullDay`() {
        assertEquals("miércoles, 2 de septiembre", StatsFormat.formatFullDay("2026-09-02"))
    }
}
