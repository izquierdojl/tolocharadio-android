package com.izquierdojl.tolocharadio.feature.podcasts.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastHome
import com.izquierdojl.tolocharadio.data.repo.EpisodesRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

        private val _refreshing = MutableStateFlow(false)

        /** Recarga manual en curso; mantiene el contenido visible. */
        val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

        private val _message = MutableStateFlow<String?>(null)

        /** Error accionable de la última recarga manual. */
        val message: StateFlow<String?> = _message.asStateFlow()

        private var loadJob: Job? = null

        init {
            refresh()
        }

        fun refresh() {
            if (_refreshing.value) return
            load(showLoading = true)
        }

        /** Recarga sin spinner para no perder el scroll (cambio de estado). */
        fun refreshSilently() {
            if (_refreshing.value) return
            load(showLoading = false)
        }

        /**
         * Recarga manual por arrastre. No publica [PodcastHomeUi.Loading]:
         * conserva el contenido y, si falla, informa sin vaciar la pantalla.
         */
        fun refreshManually() {
            if (_refreshing.value || _ui.value !is PodcastHomeUi.Content) return
            _refreshing.value = true
            _message.value = null
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    try {
                        when (val r = repo.home()) {
                            is ApiResult.Ok -> publishHome(r.value)
                            is ApiResult.Err -> _message.value = r.error.userMessage()
                        }
                    } finally {
                        _refreshing.value = false
                    }
                }
        }

        fun clearMessage() {
            _message.value = null
        }

        private fun load(showLoading: Boolean) {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    if (showLoading || _ui.value !is PodcastHomeUi.Content) {
                        _ui.value = PodcastHomeUi.Loading
                    }
                    when (val r = repo.home()) {
                        is ApiResult.Ok -> publishHome(r.value)
                        is ApiResult.Err -> _ui.value = PodcastHomeUi.Error(r.error.userMessage())
                    }
                }
        }

        private fun publishHome(home: PodcastHome) {
            _ui.value =
                if (home.subscriptions.isEmpty() && home.latestEpisodes.isEmpty() &&
                    home.continueListening.isEmpty()
                ) {
                    PodcastHomeUi.Empty
                } else {
                    PodcastHomeUi.Content(home)
                }
        }
    }
