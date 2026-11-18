package com.izquierdojl.tolocharadio.feature.podcasts.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.api.PodcastCategories
import com.izquierdojl.tolocharadio.data.remote.api.PodcastHabits
import com.izquierdojl.tolocharadio.data.remote.api.PodcastRecents
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopEpisodes
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopPodcasts
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastStatsSummaryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTimelineDto
import com.izquierdojl.tolocharadio.data.repo.PodcastStatsRepo
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

    data class Content(
        val summary: PodcastStatsSummaryDto,
        val topPodcasts: PodcastTopPodcasts = PodcastTopPodcasts(),
        val topEpisodes: PodcastTopEpisodes = PodcastTopEpisodes(),
        val timeline: PodcastTimelineDto = PodcastTimelineDto(),
        val habits: PodcastHabits = PodcastHabits(),
        val categories: PodcastCategories = PodcastCategories(),
        val recent: PodcastRecents = PodcastRecents(),
    ) : PodcastStatsUi

    data class Error(val message: String) : PodcastStatsUi
}

/** Estadísticas de podcast, visibles solo para la cuenta activa (US6). */
@HiltViewModel
class PodcastStatsViewModel
    @Inject
    constructor(private val repo: PodcastStatsRepo) : ViewModel() {
        private val _ui = MutableStateFlow<PodcastStatsUi>(PodcastStatsUi.Loading)
        val ui: StateFlow<PodcastStatsUi> = _ui.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _ui.value = PodcastStatsUi.Loading
                val summary = async { repo.summary() }
                val topPodcasts = async { repo.topPodcasts() }
                val topEpisodes = async { repo.topEpisodes() }
                val timeline = async { repo.timeline() }
                val habits = async { repo.habits() }
                val categories = async { repo.categories() }
                val recent = async { repo.recent() }
                val s = summary.await()
                if (s is ApiResult.Err) {
                    _ui.value = PodcastStatsUi.Error(s.error.userMessage())
                    return@launch
                }
                _ui.value =
                    PodcastStatsUi.Content(
                        summary = (s as ApiResult.Ok).value,
                        topPodcasts = (topPodcasts.await() as? ApiResult.Ok)?.value ?: PodcastTopPodcasts(),
                        topEpisodes = (topEpisodes.await() as? ApiResult.Ok)?.value ?: PodcastTopEpisodes(),
                        timeline = (timeline.await() as? ApiResult.Ok)?.value ?: PodcastTimelineDto(),
                        habits = (habits.await() as? ApiResult.Ok)?.value ?: PodcastHabits(),
                        categories = (categories.await() as? ApiResult.Ok)?.value ?: PodcastCategories(),
                        recent = (recent.await() as? ApiResult.Ok)?.value ?: PodcastRecents(),
                    )
            }
        }
    }
