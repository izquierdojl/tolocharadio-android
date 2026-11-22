package com.izquierdojl.tolocharadio.feature.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.data.repo.SystemRepo
import com.izquierdojl.tolocharadio.domain.podcasts.LoadTopChartsUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.TopChartsLoad
import com.izquierdojl.tolocharadio.domain.podcasts.TopCountry
import com.izquierdojl.tolocharadio.domain.podcasts.TopGenre
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la sección «Populares».
 *
 * `visible` es true mientras se comprueba la capacidad, está soportada o
 * falló de forma transitoria; false solo si es definitivamente no soportada
 * o el endpoint responde 404. `isAuthError` cambia la acción principal de
 * «Reintentar» a «Editar servidor».
 */
data class TopChartsUi(
    val visible: Boolean = false,
    val country: TopCountry = TopCountry.SPAIN,
    val genre: TopGenre = TopGenre.ALL,
    val items: List<PodcastSearchResult> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val isAuthError: Boolean = false,
    val genreUnavailable: Boolean = false,
)

/**
 * Ranking de podcasts por país/género (spec 0053).
 *
 * La capacidad se comprueba con la caché de [SystemRepo]; un fallo
 * transitorio se muestra como error, no como «no soportado». Los filtros se
 * persisten en [InstancePrefs] y cada carga cancela la anterior.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class TopChartsViewModel
    @Inject
    constructor(
        private val prefs: InstancePrefs,
        private val system: SystemRepo,
        private val loadTopCharts: LoadTopChartsUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(TopChartsUi(visible = true, loading = true))
        val ui: StateFlow<TopChartsUi> = _ui.asStateFlow()

        private var loadJob: Job? = null
        private var generation = 0
        private var capabilityFailed = false
        private var selectionTouched = false

        init {
            viewModelScope.launch {
                val country = prefs.topChartsCountry.first()
                val genre = prefs.topChartsGenre.first()
                if (!selectionTouched) {
                    _ui.value = _ui.value.copy(country = country, genre = genre)
                    reload()
                }
            }
        }

        /** Cambia el país y recarga; la selección se persiste localmente. */
        fun selectCountry(country: TopCountry) {
            if (_ui.value.country == country) return
            selectionTouched = true
            _ui.value = _ui.value.copy(country = country, error = null, isAuthError = false)
            viewModelScope.launch { prefs.setTopChartsCountry(country) }
            reload()
        }

        /** Cambia el género y recarga; `ALL` no envía `genre`. */
        fun selectGenre(genre: TopGenre) {
            if (_ui.value.genre == genre) return
            selectionTouched = true
            _ui.value = _ui.value.copy(genre = genre, error = null, isAuthError = false)
            viewModelScope.launch { prefs.setTopChartsGenre(genre) }
            reload()
        }

        /** Reintenta la selección vigente; fuerza capacidad solo si falló. */
        fun retry() {
            reload(forceCapabilityRefresh = capabilityFailed)
        }

        private fun reload(forceCapabilityRefresh: Boolean = false) {
            loadJob?.cancel()
            val requestGeneration = ++generation
            loadJob =
                viewModelScope.launch {
                    val country = _ui.value.country
                    val genre = _ui.value.genre
                    _ui.value =
                        _ui.value.copy(
                            loading = true,
                            error = null,
                            isAuthError = false,
                            genreUnavailable = false,
                        )

                    when (val capabilities = system.podcastCapabilities(forceRefresh = forceCapabilityRefresh)) {
                        is ApiResult.Err -> {
                            if (requestGeneration != generation) return@launch
                            capabilityFailed = true
                            publishError(capabilities.error)
                        }
                        is ApiResult.Ok -> {
                            if (requestGeneration != generation) return@launch
                            capabilityFailed = false
                            if (!capabilities.value.topCharts) {
                                hide()
                            } else {
                                loadRanking(country, genre, requestGeneration)
                            }
                        }
                    }
                }
        }

        private suspend fun loadRanking(
            country: TopCountry,
            genre: TopGenre,
            requestGeneration: Int,
        ) {
            when (val result = loadTopCharts(country, genre)) {
                is ApiResult.Ok -> {
                    if (requestGeneration == generation) publishContent(result.value)
                }
                is ApiResult.Err -> {
                    if (requestGeneration != generation) return
                    if (result.error is DomainError.NotFound) {
                        hide()
                    } else {
                        publishError(result.error)
                    }
                }
            }
        }

        private fun publishContent(load: TopChartsLoad) {
            _ui.value =
                _ui.value.copy(
                    visible = true,
                    loading = false,
                    items = load.items,
                    error = null,
                    isAuthError = false,
                    genreUnavailable = load.genreUnavailable,
                )
        }

        private fun publishError(error: DomainError) {
            _ui.value =
                _ui.value.copy(
                    visible = true,
                    loading = false,
                    items = emptyList(),
                    error = error.userMessage(),
                    isAuthError = error is DomainError.Unauthorized,
                    genreUnavailable = false,
                )
        }

        private fun hide() {
            _ui.value =
                _ui.value.copy(
                    visible = false,
                    loading = false,
                    items = emptyList(),
                    error = null,
                    isAuthError = false,
                    genreUnavailable = false,
                )
        }
    }
