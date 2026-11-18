package com.izquierdojl.tolocharadio.feature.podcasts.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.repo.EpisodeFavoritesRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de episodios favoritos (US5, FR-023). */
sealed interface EpisodeFavoritesUi {
    data object Loading : EpisodeFavoritesUi

    data object Empty : EpisodeFavoritesUi

    data class Content(val items: List<PodcastEpisode>) : EpisodeFavoritesUi

    data class Error(val message: String) : EpisodeFavoritesUi
}

/** Favoritos de episodios, independientes de emisoras (US5). */
@HiltViewModel
class EpisodeFavoritesViewModel
    @Inject
    constructor(private val repo: EpisodeFavoritesRepo) : ViewModel() {
        private val _ui = MutableStateFlow<EpisodeFavoritesUi>(EpisodeFavoritesUi.Loading)
        val ui: StateFlow<EpisodeFavoritesUi> = _ui.asStateFlow()

        init {
            refresh()
            viewModelScope.launch {
                repo.favorites.collect { items ->
                    if (_ui.value !is EpisodeFavoritesUi.Loading) {
                        _ui.value =
                            if (items.isEmpty()) {
                                EpisodeFavoritesUi.Empty
                            } else {
                                EpisodeFavoritesUi.Content(items)
                            }
                    }
                }
            }
        }

        fun refresh() {
            load(showLoading = true)
        }

        /** Recarga sin spinner para no perder el scroll (cambio de estado). */
        fun refreshSilently() {
            load(showLoading = false)
        }

        private fun load(showLoading: Boolean) {
            viewModelScope.launch {
                if (showLoading || _ui.value !is EpisodeFavoritesUi.Content) {
                    _ui.value = EpisodeFavoritesUi.Loading
                }
                when (val r = repo.list()) {
                    is ApiResult.Ok ->
                        _ui.value =
                            if (r.value.isEmpty()) {
                                EpisodeFavoritesUi.Empty
                            } else {
                                EpisodeFavoritesUi.Content(r.value)
                            }
                    is ApiResult.Err -> _ui.value = EpisodeFavoritesUi.Error(r.error.userMessage())
                }
            }
        }

        fun unfavorite(episodeId: Long) {
            viewModelScope.launch {
                repo.remove(episodeId)
            }
        }
    }
