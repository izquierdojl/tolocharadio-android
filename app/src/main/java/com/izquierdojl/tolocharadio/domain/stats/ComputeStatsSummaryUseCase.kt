package com.izquierdojl.tolocharadio.domain.stats

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopListDto
import javax.inject.Inject

/** Resumen derivado del periodo (R4, paridad con `StatsSummary.tsx`). */
data class StatsSummary(
    val totalMs: Long,
    val topStation: StationDto?,
    val topStationMs: Long,
    val peakBucket: String?,
    val peakTotalMs: Long,
)

/**
 * Resumen del periodo a partir de la serie temporal y el top: tiempo
 * total, emisora destacada y día con más escucha. Puro (sin Android ni
 * red); `totalMs <= 0` equivale al estado vacío de la vista (data-model).
 */
class ComputeStatsSummaryUseCase
    @Inject
    constructor() {
        operator fun invoke(
            timeline: StatsTimelineDto,
            top: StatsTopListDto,
        ): StatsSummary {
            val best = top.items.firstOrNull()
            val peak = timeline.items.maxByOrNull { it.totalMs }
            return StatsSummary(
                totalMs = timeline.items.sumOf { it.totalMs },
                topStation = best?.station,
                topStationMs = best?.totalMs ?: 0L,
                peakBucket = peak?.bucket,
                peakTotalMs = peak?.totalMs ?: 0L,
            )
        }
    }
