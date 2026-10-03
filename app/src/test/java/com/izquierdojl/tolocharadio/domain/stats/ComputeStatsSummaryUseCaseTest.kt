package com.izquierdojl.tolocharadio.domain.stats

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopListDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Resumen derivado del periodo: paridad con `StatsSummary.tsx` (R4, data-model.md). */
class ComputeStatsSummaryUseCaseTest {
    private val compute = ComputeStatsSummaryUseCase()

    @Test
    fun `total suma el timeline destacada es el top1 y pico el bucket maximo`() {
        val timeline =
            StatsTimelineDto(
                items =
                    listOf(
                        StatsTimelineEntryDto("2026-09-01", 1000),
                        StatsTimelineEntryDto("2026-09-02", 5000),
                        StatsTimelineEntryDto("2026-09-03", 3000),
                    ),
            )
        val top =
            StatsTopListDto(
                items =
                    listOf(
                        StatsTopEntryDto(StationDto("s1", "Rock FM"), 7000),
                        StatsTopEntryDto(StationDto("s2", "Jazz FM"), 2000),
                    ),
            )

        val summary = compute(timeline, top)

        assertEquals(9000L, summary.totalMs)
        assertEquals("Rock FM", summary.topStation?.name)
        assertEquals(7000L, summary.topStationMs)
        assertEquals("2026-09-02", summary.peakBucket)
        assertEquals(5000L, summary.peakTotalMs)
    }

    @Test
    fun `sin escuchas el resumen esta vacio (invariante del data-model)`() {
        val summary = compute(StatsTimelineDto(), StatsTopListDto())
        assertEquals(0L, summary.totalMs)
        assertNull(summary.topStation)
        assertNull(summary.peakBucket)
        assertEquals(0L, summary.peakTotalMs)
    }
}
