package com.izquierdojl.tolocharadio.feature.explore

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.ErrorBanner
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.core.ui.components.StationCard
import com.izquierdojl.tolocharadio.core.ui.components.StationListItem
import com.izquierdojl.tolocharadio.feature.ViewModeViewModel

/** Filtro seleccionado para abrir en el bottom sheet. */
private enum class ActiveFilter { COUNTRY, LANGUAGE, TAG }

/**
 * Explorar el catálogo con filtros y paginación (US-4, spec 008).
 *
 * Los filtros se muestran como chips compactos. Al tocar uno se abre
 * un [ModalBottomSheet] con el selector completo (combobox + autocompletado).
 *
 * @param onStation abre la ficha al pulsar la tarjeta.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onStation: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
    viewModeVm: ViewModeViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.ui.collectAsState()
    val mode by viewModeVm.mode.collectAsState()
    val countries by viewModel.countries.collectAsState()
    val languages by viewModel.languages.collectAsState()
    val tags by viewModel.tags.collectAsState()

    var sheetFilter by remember { mutableStateOf<ActiveFilter?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Column(Modifier.fillMaxSize()) {
        SectionHeader(title = "Explorar", subtitle = "Descubre emisoras de todo el mundo.")
        OutlinedTextField(
            value = viewModel.filters.name,
            onValueChange = { viewModel.setFilters(viewModel.filters.copy(name = it)) },
            label = { Text("Buscar por nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        // Chips de filtro compactos
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = viewModel.filters.country != null,
                onClick = { sheetFilter = ActiveFilter.COUNTRY },
                label = { Text(viewModel.filters.country ?: "País") },
                trailingIcon =
                    if (viewModel.filters.country != null) {
                        {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar",
                                modifier =
                                    Modifier
                                        .size(18.dp)
                                        .clickable {
                                            viewModel.setFilters(
                                                viewModel.filters.copy(country = null),
                                            )
                                        },
                            )
                        }
                    } else {
                        null
                    },
            )
            FilterChip(
                selected = viewModel.filters.language != null,
                onClick = { sheetFilter = ActiveFilter.LANGUAGE },
                label = { Text(viewModel.filters.language ?: "Idioma") },
                trailingIcon =
                    if (viewModel.filters.language != null) {
                        {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar",
                                modifier =
                                    Modifier
                                        .size(18.dp)
                                        .clickable {
                                            viewModel.setFilters(
                                                viewModel.filters.copy(language = null),
                                            )
                                        },
                            )
                        }
                    } else {
                        null
                    },
            )
            FilterChip(
                selected = viewModel.filters.tag != null,
                onClick = { sheetFilter = ActiveFilter.TAG },
                label = { Text(viewModel.filters.tag ?: "Género") },
                trailingIcon =
                    if (viewModel.filters.tag != null) {
                        {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar",
                                modifier =
                                    Modifier
                                        .size(18.dp)
                                        .clickable {
                                            viewModel.setFilters(
                                                viewModel.filters.copy(tag = null),
                                            )
                                        },
                            )
                        }
                    } else {
                        null
                    },
            )
            if (
                viewModel.filters.country != null ||
                viewModel.filters.language != null ||
                viewModel.filters.tag != null
            ) {
                TextButton(onClick = {
                    viewModel.setFilters(ExploreFilters(name = viewModel.filters.name))
                }) {
                    Text("Limpiar filtros")
                }
            }
        }
        ExploreContent(
            ui = ui,
            mode = mode,
            onStation = onStation,
            onToggleFavorite = viewModel::toggleFavorite,
            onRetry = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
        )
    }

    // Bottom sheet para seleccionar filtro
    if (sheetFilter != null) {
        var selectedValue by remember(sheetFilter) {
            mutableStateOf(
                when (sheetFilter) {
                    ActiveFilter.COUNTRY -> viewModel.filters.country.orEmpty()
                    ActiveFilter.LANGUAGE -> viewModel.filters.language.orEmpty()
                    ActiveFilter.TAG -> viewModel.filters.tag.orEmpty()
                    null -> ""
                },
            )
        }

        ModalBottomSheet(
            onDismissRequest = { sheetFilter = null },
            sheetState = sheetState,
        ) {
            Column(Modifier.padding(16.dp)) {
                when (sheetFilter) {
                    ActiveFilter.COUNTRY -> {
                        Text("País", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        FilterComboBox(
                            value = selectedValue,
                            onValueChange = { selectedValue = it },
                            label = "Seleccionar país",
                            catalogList = countries,
                        )
                    }
                    ActiveFilter.LANGUAGE -> {
                        Text("Idioma", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        FilterComboBox(
                            value = selectedValue,
                            onValueChange = { selectedValue = it },
                            label = "Seleccionar idioma",
                            catalogList = languages,
                        )
                    }
                    ActiveFilter.TAG -> {
                        Text("Género", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        FilterComboBox(
                            value = selectedValue,
                            onValueChange = { selectedValue = it },
                            label = "Seleccionar género",
                            catalogList = tags,
                        )
                    }
                    null -> {}
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        when (sheetFilter) {
                            ActiveFilter.COUNTRY ->
                                viewModel.setFilters(
                                    viewModel.filters.copy(country = selectedValue.ifBlank { null }),
                                )
                            ActiveFilter.LANGUAGE ->
                                viewModel.setFilters(
                                    viewModel.filters.copy(language = selectedValue.ifBlank { null }),
                                )
                            ActiveFilter.TAG ->
                                viewModel.setFilters(
                                    viewModel.filters.copy(tag = selectedValue.ifBlank { null }),
                                )
                            null -> {}
                        }
                        sheetFilter = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Aceptar")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Contenido de Explorar independiente del ViewModel (testeable con
 * Compose Test): renderiza el [ui] en [mode] sin recargar datos (FR-003).
 */
@Composable
internal fun ExploreContent(
    ui: ExploreUiState,
    mode: ViewMode,
    onStation: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    when (val s = ui) {
        ExploreUiState.Loading -> CircularProgressIndicator(Modifier.padding(32.dp))
        is ExploreUiState.Error -> ErrorBanner(s.message, onRetry = onRetry)
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
                if (mode == ViewMode.GRID) {
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
                                onToggleFavorite = { onToggleFavorite(station.id) },
                            )
                        }
                        if (s.hasMore) {
                            item { MoreButton(s.loadingMore, onLoadMore) }
                        }
                    }
                } else {
                    LazyColumn {
                        items(s.items, key = { it.id }) { station ->
                            StationListItem(
                                station = station,
                                isFavorite = station.id in s.favorites,
                                onPlay = { onStation(station.id) },
                                onToggleFavorite = { onToggleFavorite(station.id) },
                            )
                        }
                        if (s.hasMore) {
                            item { MoreButton(s.loadingMore, onLoadMore) }
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
