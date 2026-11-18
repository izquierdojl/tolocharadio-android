package com.izquierdojl.tolocharadio.feature.podcasts.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastHome
import com.izquierdojl.tolocharadio.data.repo.EpisodesRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de la portada (US6): continuar, novedades, programas y cola. */
sealed interface PodcastHomeUi {
    data object Loading : PodcastHomeUi

    data object Empty : PodcastHomeUi

    data class Content(val home: PodcastHome) : PodcastHomeUi

    data class Error(val message: String) : PodcastHomeUi
}

/** Portada agregada de podcasts (US6). */
@HiltViewModel
class PodcastHomeViewModel
    @Inject
    constructor(private val repo: EpisodesRepo) : ViewModel() {
        private val _ui = MutableStateFlow<PodcastHomeUi>(PodcastHomeUi.Loading)
        val ui: StateFlow<PodcastHomeUi> = _ui.asStateFlow()

        init {
            refresh()
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
                if (showLoading || _ui.value !is PodcastHomeUi.Content) {
                    _ui.value = PodcastHomeUi.Loading
                }
                when (val r = repo.home()) {
                    is ApiResult.Ok -> {
                        val home = r.value
                        _ui.value =
                            if (home.subscriptions.isEmpty() && home.latestEpisodes.isEmpty() &&
                                home.continueListening.isEmpty()
                            ) {
                                PodcastHomeUi.Empty
                            } else {
                                PodcastHomeUi.Content(home)
                            }
                    }
                    is ApiResult.Err -> _ui.value = PodcastHomeUi.Error(r.error.userMessage())
                }
            }
        }
    }
