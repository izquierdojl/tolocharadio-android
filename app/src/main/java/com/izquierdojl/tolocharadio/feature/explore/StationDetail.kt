package com.izquierdojl.tolocharadio.feature.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.core.ui.components.ErrorBanner
import com.izquierdojl.tolocharadio.core.ui.components.FavoriteButton
import com.izquierdojl.tolocharadio.core.ui.components.StationArtwork
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.FavoritesRepo
import com.izquierdojl.tolocharadio.data.repo.StationsRepo
import com.izquierdojl.tolocharadio.domain.ToggleFavoriteUseCase
import com.izquierdojl.tolocharadio.domain.playback.PlaybackStatusReason
import com.izquierdojl.tolocharadio.domain.playback.PlayerErrorAction
import com.izquierdojl.tolocharadio.domain.playback.StationAvailability
import com.izquierdojl.tolocharadio.feature.player.PlayerState
import com.izquierdojl.tolocharadio.feature.player.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Content(val station: StationDto, val isFavorite: Boolean) : DetailUiState

    data class Error(val message: String) : DetailUiState
}

/** Ficha de emisora (`GET /stations/:id`) con play y favorito. */
@HiltViewModel
class StationDetailViewModel
    @Inject
    constructor(
        private val repo: StationsRepo,
        private val favorites: FavoritesRepo,
        private val toggleFavorite: ToggleFavoriteUseCase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val stationId: String = checkNotNull(savedStateHandle["stationId"])

        private val _ui = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
        val ui: StateFlow<DetailUiState> = _ui.asStateFlow()

        init {
            refresh()
            // Coherencia con el resto de pantallas (FR-003).
            viewModelScope.launch {
                favorites.favoriteIds.collect { ids ->
                    val c = _ui.value as? DetailUiState.Content ?: return@collect
                    if ((c.station.id in ids) != c.isFavorite) {
                        _ui.value = c.copy(isFavorite = c.station.id in ids)
                    }
                }
            }
            viewModelScope.launch { favorites.list() }
        }

        fun refresh() {
            _ui.value = DetailUiState.Loading
            viewModelScope.launch {
                _ui.value =
                    when (val r = repo.detail(stationId)) {
                        is ApiResult.Ok -> DetailUiState.Content(r.value, stationId in favorites.favoriteIds.value)
                        is ApiResult.Err -> DetailUiState.Error(r.error.userMessage())
                    }
            }
        }

        fun toggleFavorite() {
            val c = _ui.value as? DetailUiState.Content ?: return
            _ui.value = c.copy(isFavorite = !c.isFavorite)
            viewModelScope.launch {
                if (toggleFavorite(c.station.id, c.isFavorite) is ApiResult.Err) {
                    _ui.value = c
                }
            }
        }
    }

/** Pantalla de ficha de emisora. */
@Composable
fun StationDetailScreen(
    viewModel: StationDetailViewModel,
    onBack: () -> Unit,
    player: PlayerViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val availability by player.availability.collectAsState()
    val playerState by player.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        when (val s = ui) {
            DetailUiState.Loading -> CircularProgressIndicator()
            is DetailUiState.Error -> {
                ErrorBanner(s.message, onRetry = viewModel::refresh)
                Button(onClick = onBack) { Text("Volver") }
            }
            is DetailUiState.Content -> {
                LaunchedEffect(s.station.id) {
                    player.checkAvailability(s.station)
                }
                DisposableEffect(s.station.id) {
                    onDispose { player.cancelAvailability() }
                }
                StationDetailContent(
                    station = s.station,
                    isFavorite = s.isFavorite,
                    availability = availability?.takeIf { it.stationId == s.station.id },
                    playerState = playerState,
                    onPlay = { player.play(s.station) },
                    onRetry = player::retry,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onBack = onBack,
                )
            }
        }
    }
}

/** Contenido sin estado de la ficha; testeable sin Hilt. */
@Composable
@Suppress("LongParameterList", "CyclomaticComplexMethod")
internal fun StationDetailContent(
    station: StationDto,
    isFavorite: Boolean,
    availability: StationAvailability?,
    playerState: PlayerState,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        StationArtwork(station, Modifier.size(96.dp))
        Spacer(Modifier.height(12.dp))
        Text(station.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            listOfNotNull(station.country, station.language).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (station.tags.isNotEmpty()) {
            Text(station.tags.joinToString(", "), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))

        val notPlayable = availability?.playable == false
        if (notPlayable) {
            Text(
                PlaybackStatusReason.reasonToMessage(availability.reason),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = onPlay,
            enabled = !notPlayable,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (notPlayable) "No disponible" else "Reproducir")
        }

        val current = playerState.stationOrNull()?.takeIf { it.id == station.id }
        if (current != null) {
            Spacer(Modifier.height(12.dp))
            val message =
                when (playerState) {
                    is PlayerState.Buffering ->
                        when {
                            playerState.waitingForNetwork ->
                                "Sin conexión. Se reanudará al volver la red."
                            playerState.retrying -> "Reintentando…"
                            else -> "Cargando…"
                        }
                    is PlayerState.Playing -> "Reproduciendo"
                    is PlayerState.Paused -> "Pausado"
                    is PlayerState.Reconnecting ->
                        if (playerState.waitingForNetwork) {
                            "Sin conexión. Se reanudará al volver la red."
                        } else {
                            "Reconectando (${playerState.attempt}/${playerState.maxAttempts})…"
                        }
                    is PlayerState.Error -> playerState.message
                    PlayerState.Idle -> null
                }
            if (message != null) {
                Text(
                    message,
                    color =
                        if (playerState is PlayerState.Error) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (playerState is PlayerState.Error && playerState.action == PlayerErrorAction.RETRY) {
                TextButton(onClick = onRetry) { Text("Reintentar") }
            }
        }

        Spacer(Modifier.height(16.dp))
        FavoriteButton(isFavorite = isFavorite, onToggle = onToggleFavorite)
        TextButton(onClick = onBack) { Text("Volver") }
    }
}
