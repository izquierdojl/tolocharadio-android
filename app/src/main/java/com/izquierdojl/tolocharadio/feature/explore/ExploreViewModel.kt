package com.izquierdojl.tolocharadio.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.FavoritesRepo
import com.izquierdojl.tolocharadio.data.repo.StationQuery
import com.izquierdojl.tolocharadio.data.repo.StationsRepo
import com.izquierdojl.tolocharadio.domain.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Filtros de Explorar (paridad `/explorar` web). */
data class ExploreFilters(
    val name: String = "",
    val country: String? = null,
    val language: String? = null,
    val tag: String? = null,
    val unique: Boolean = false,
)

/** UI de Explorar con paginación manual (`offset/limit/hasMore`). */
sealed interface ExploreUiState {
    data object Loading : ExploreUiState

    data class Content(
        val items: List<StationDto>,
        val favorites: Set<String>,
        val hasMore: Boolean,
        val loadingMore: Boolean = false,
        val offlineCache: Boolean = false,
    ) : ExploreUiState

    data class Error(val message: String) : ExploreUiState
}

/** Explorar: búsqueda, filtros, paginación y favorito rápido (US-4). */
@HiltViewModel
class ExploreViewModel
    @Inject
    constructor(
        private val repo: StationsRepo,
        private val favorites: FavoritesRepo,
        private val toggleFavorite: ToggleFavoriteUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
        val ui: StateFlow<ExploreUiState> = _ui.asStateFlow()

        var filters = ExploreFilters()
            private set

        private var offset = 0
        private val loaded = mutableListOf<StationDto>()
        private val favoriteIds = mutableSetOf<String>()

        init {
            refresh()
            // Marcado coherente (FR-003): la verdad vive en el flujo
            // compartido del repo; además se hidrata en silencio.
            viewModelScope.launch {
                favorites.favoriteIds.collect {
                    favoriteIds.clear()
                    favoriteIds.addAll(it)
                    emitContent()
                }
            }
            viewModelScope.launch { favorites.list() }
        }

        /** Recarga desde el offset 0 con los filtros actuales. */
        fun refresh() {
            offset = 0
            loaded.clear()
            loadMore(initial = true)
        }

        /** Cambia filtros y recarga. */
        fun setFilters(next: ExploreFilters) {
            filters = next
            refresh()
        }

        /** Carga la siguiente página si `hasMore`. */
        fun loadMore(initial: Boolean = false) {
            val current = _ui.value
            if (!initial && current !is ExploreUiState.Content) return
            if (!initial && (current as ExploreUiState.Content).loadingMore) return
            if (current is ExploreUiState.Content) {
                _ui.value = current.copy(loadingMore = true)
            } else {
                _ui.value = ExploreUiState.Loading
            }
            viewModelScope.launch {
                val query =
                    StationQuery(
                        name = filters.name.ifBlank { null },
                        country = filters.country,
                        language = filters.language,
                        tag = filters.tag,
                        limit = 24,
                        offset = offset,
                        unique = filters.unique,
                    )
                when (val r = repo.search(query)) {
                    is ApiResult.Ok -> {
                        loaded.addAll(r.value.items)
                        offset += r.value.items.size
                        _ui.value =
                            ExploreUiState.Content(
                                items = loaded.toList(),
                                favorites = favoriteIds.toSet(),
                                hasMore = r.value.pagination.hasMore,
                                offlineCache = !r.value.pagination.hasMore && isCacheFallback(r),
                            )
                    }
                    is ApiResult.Err -> {
                        if (loaded.isEmpty()) {
                            _ui.value = ExploreUiState.Error(r.error.userMessage())
                        } else {
                            val c = _ui.value
                            if (c is ExploreUiState.Content) _ui.value = c.copy(loadingMore = false)
                        }
                    }
                }
            }
        }

        private fun isCacheFallback(r: ApiResult.Ok<com.izquierdojl.tolocharadio.data.remote.dto.StationPageDto>): Boolean =
            r.value.items.isNotEmpty() && !r.value.pagination.hasMore && offset <= r.value.items.size

        /** Favorito optimista con rollback ante error (spec US-4). */
        fun toggleFavorite(stationId: String) {
            val wasFavorite = favoriteIds.contains(stationId)
            if (wasFavorite) favoriteIds.remove(stationId) else favoriteIds.add(stationId)
            emitContent()
            viewModelScope.launch {
                when (toggleFavorite(stationId, wasFavorite)) {
                    is ApiResult.Ok -> Unit
                    is ApiResult.Err -> {
                        // Rollback.
                        if (wasFavorite) favoriteIds.add(stationId) else favoriteIds.remove(stationId)
                        emitContent()
                    }
                }
            }
        }

        private fun emitContent() {
            val c = _ui.value as? ExploreUiState.Content ?: return
            _ui.value = c.copy(items = loaded.toList(), favorites = favoriteIds.toSet())
        }
    }
