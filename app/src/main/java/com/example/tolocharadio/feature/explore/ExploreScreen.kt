package com.example.tolocharadio.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tolocharadio.core.ui.components.EmptyState
import com.example.tolocharadio.core.ui.components.ErrorBanner
import com.example.tolocharadio.core.ui.components.StationCard
import com.example.tolocharadio.core.ui.components.StationListItem

/**
 * Explorar el catálogo con filtros y paginación (US-4).
 *
 * @param onStation abre la ficha al pulsar la tarjeta.
 */
@Composable
fun ExploreScreen(
    onStation: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = viewModel.filters.name,
            onValueChange = { viewModel.setFilters(viewModel.filters.copy(name = it)) },
            label = { Text("Buscar por nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            trailingIcon = {
                IconButton(onClick = viewModel::toggleGrid) {
                    Icon(
                        if (viewModel.gridMode) Icons.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = "Cambiar vista",
                    )
                }
            },
        )
        when (val s = ui) {
            ExploreUiState.Loading -> CircularProgressIndicator(Modifier.padding(32.dp))
            is ExploreUiState.Error -> ErrorBanner(s.message, onRetry = viewModel::refresh)
            is ExploreUiState.Content -> {
                if (s.items.isEmpty()) {
                    EmptyState("Sin resultados. Prueba con otra búsqueda.")
                } else {
                    if (s.offlineCache) {
                        Text(
                            "Mostrando caché sin conexión.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    if (viewModel.gridMode) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(s.items, key = { it.id }) { station ->
                                StationCard(
                                    station = station,
                                    isFavorite = station.id in s.favorites,
                                    onPlay = { onStation(station.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(station.id) },
                                )
                            }
                            if (s.hasMore) {
                                item { MoreButton(s.loadingMore, viewModel::loadMore) }
                            }
                        }
                    } else {
                        LazyColumn {
                            items(s.items, key = { it.id }) { station ->
                                StationListItem(
                                    station = station,
                                    isFavorite = station.id in s.favorites,
                                    onPlay = { onStation(station.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(station.id) },
                                )
                            }
                            if (s.hasMore) {
                                item { MoreButton(s.loadingMore, viewModel::loadMore) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreButton(
    loading: Boolean,
    onMore: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { onMore() }) { Text("Cargar más") }
        }
    }
}
