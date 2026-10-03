package com.izquierdojl.tolocharadio.domain.stats

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.repo.StatsRepo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Carga el conjunto de bloques del periodo en paralelo (R3): los 6
 * endpoints (`top`, `timeline`, `genres`, `countries`, `habits` y
 * `recent`). Si cualquiera falla, la vista entera pasa a error con
 * reintento (FR-007).
 */
class LoadStatsUseCase
    @Inject
    constructor(
        private val repo: StatsRepo,
    ) {
        suspend operator fun invoke(range: StatsRange): ApiResult<StatsBundle> =
            coroutineScope {
                val top = async { repo.top(range.from, range.to, null) }
                val timeline =
                    async {
                        repo.timeline(range.from, range.to, range.autoGranularity().wire)
                    }
                val genres = async { repo.genres(range.from, range.to, null) }
                val countries = async { repo.countries(range.from, range.to) }
                val habits = async { repo.habits(range.from, range.to) }
                val recent = async { repo.recent(null) }
                val topResult = top.await()
                val timelineResult = timeline.await()
                val genresResult = genres.await()
                val countriesResult = countries.await()
                val habitsResult = habits.await()
                val recentResult = recent.await()
                listOf(
                    topResult,
                    timelineResult,
                    genresResult,
                    countriesResult,
                    habitsResult,
                    recentResult,
                ).filterIsInstance<ApiResult.Err>().firstOrNull()
                    ?.let { return@coroutineScope it }
                ApiResult.Ok(
                    StatsBundle(
                        top = (topResult as ApiResult.Ok).value,
                        timeline = (timelineResult as ApiResult.Ok).value,
                        genres = (genresResult as ApiResult.Ok).value,
                        countries = (countriesResult as ApiResult.Ok).value,
                        habits = (habitsResult as ApiResult.Ok).value,
                        recent = (recentResult as ApiResult.Ok).value,
                    ),
                )
            }
    }
