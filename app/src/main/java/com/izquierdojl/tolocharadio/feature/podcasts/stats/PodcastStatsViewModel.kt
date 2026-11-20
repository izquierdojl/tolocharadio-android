package com.izquierdojl.tolocharadio.feature.podcasts.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.api.PodcastCategories
import com.izquierdojl.tolocharadio.data.remote.api.PodcastHabits
import com.izquierdojl.tolocharadio.data.remote.api.PodcastRecents
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopEpisodes
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopPodcasts
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastStatsSummaryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTimelineDto
import com.izquierdojl.tolocharadio.data.repo.PodcastStatsRepo
import com.izquierdojl.tolocharadio.domain.stats.buildHabitsMatrix
import com.izquierdojl.tolocharadio.feature.stats.StatsErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de estadísticas privadas de podcast (US6, FR-026). */
sealed interface PodcastStatsUi {
    data object Loading : PodcastStatsUi

    data object Empty : PodcastStatsUi

    data class Content(
        val summary: PodcastStatsSummaryDto,
        val topPodcasts: PodcastTopPodcasts = PodcastTopPodcasts(),
        val topEpisodes: PodcastTopEpisodes = PodcastTopEpisodes(),
        val timeline: PodcastTimelineDto = PodcastTimelineDto(),
        val habits: PodcastHabits = PodcastHabits(),
        val categories: PodcastCategories = PodcastCategories(),
        val recent: PodcastRecents = PodcastRecents(),
        val habitsMatrix: List<List<Long>> = emptyList(),
    ) : PodcastStatsUi

    data class Error(
        val message: String,
        val isAuthError: Boolean = false,
    ) : PodcastStatsUi
}

/** Estadísticas de podcast, visibles solo para la cuenta activa (US6). */
@HiltViewModel
class PodcastStatsViewModel
    @Inject
    constructor(
        private val repo: PodcastStatsRepo,
        private val errorLogger: StatsErrorLogger,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<PodcastStatsUi>(PodcastStatsUi.Loading)
        val ui: StateFlow<PodcastStatsUi> = _ui.asStateFlow()

        private var loading = false

        init {
            refresh()
        }

        /** Recarga al volver a primer plano si no hay una carga en curso. */
        fun onForeground() {
            if (loading) return
            refresh()
        }

        /** Reintento manual. */
        fun retry() = refresh()

        @Suppress("LongMethod")
        fun refresh() {
            viewModelScope.launch {
                loading = true
                try {
                    if (_ui.value !is PodcastStatsUi.Content) {
                        _ui.value = PodcastStatsUi.Loading
                    }
                    val summaryDeferred = async { repo.summary() }
                    val topPodcastsDeferred = async { repo.topPodcasts() }
                    val topEpisodesDeferred = async { repo.topEpisodes() }
                    val timelineDeferred = async { repo.timeline() }
                    val habitsDeferred = async { repo.habits() }
                    val categoriesDeferred = async { repo.categories() }
                    val recentDeferred = async { repo.recent() }

                    val summary = summaryDeferred.await()
                    val topPodcasts = topPodcastsDeferred.await()
                    val topEpisodes = topEpisodesDeferred.await()
                    val timeline = timelineDeferred.await()
                    val habits = habitsDeferred.await()
                    val categories = categoriesDeferred.await()
                    val recent = recentDeferred.await()

                    val results =
                        listOf<ApiResult<*>>(
                            summary,
                            topPodcasts,
                            topEpisodes,
                            timeline,
                            habits,
                            categories,
                            recent,
                        )
                    val error = results.firstOrNull { it is ApiResult.Err } as? ApiResult.Err
                    if (error != null) {
                        errorLogger.log(error.error)
                        _ui.value =
                            PodcastStatsUi.Error(
                                message = error.error.userMessage(),
                                isAuthError = error.error is DomainError.Unauthorized,
                            )
                        return@launch
                    }

                    val summaryValue = (summary as ApiResult.Ok<PodcastStatsSummaryDto>).value
                    val topPodcastsValue = (topPodcasts as ApiResult.Ok<PodcastTopPodcasts>).value
                    val topEpisodesValue = (topEpisodes as ApiResult.Ok<PodcastTopEpisodes>).value
                    val timelineValue = (timeline as ApiResult.Ok<PodcastTimelineDto>).value
                    val habitsValue = (habits as ApiResult.Ok<PodcastHabits>).value
                    val categoriesValue = (categories as ApiResult.Ok<PodcastCategories>).value
                    val recentValue = (recent as ApiResult.Ok<PodcastRecents>).value
                    val sortedRecent =
                        PodcastRecents(recentValue.items.sortedByDescending { it.startedAt })

                    if (summaryValue.totalMs <= 0L &&
                        summaryValue.episodesStarted <= 0 &&
                        sortedRecent.items.isEmpty()
                    ) {
                        _ui.value = PodcastStatsUi.Empty
                        return@launch
                    }

                    _ui.value =
                        PodcastStatsUi.Content(
                            summary = summaryValue,
                            topPodcasts = topPodcastsValue,
                            topEpisodes = topEpisodesValue,
                            timeline = timelineValue,
                            habits = habitsValue,
                            categories = categoriesValue,
                            recent = sortedRecent,
                            habitsMatrix = buildHabitsMatrix(habitsValue.toStatsHabits().items),
                        )
                } finally {
                    loading = false
                }
            }
        }
    }
