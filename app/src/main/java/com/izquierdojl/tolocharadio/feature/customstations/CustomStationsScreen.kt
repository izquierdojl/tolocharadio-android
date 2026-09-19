package com.izquierdojl.tolocharadio.feature.customstations

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.ErrorBanner
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.core.ui.components.StationArtwork
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.feature.ViewModeViewModel
import com.izquierdojl.tolocharadio.feature.player.PlayerViewModel

/** Relación de aspecto 16:9 del artwork de la tarjeta de cuadrícula. */
private const val GRID_ARTWORK_ASPECT_RATIO = 16f / 9f

/** Color del degradado inferior del artwork en cuadrícula (~70% opacidad). */
private const val GRID_SCRIM_COLOR = 0xB3000000.toInt()

/**
 * Mis emisoras: formulario de alta (nombre + URL del stream) siempre
 * visible y lista de emisoras personalizadas con reproducir y eliminar.
 *
 * Sin ficha de detalle: la ficha (`StationDetailScreen`) carga del
 * endpoint de catálogo, que no conoce ids personalizados; tocar una
 * emisora la reproduce directamente (la spec solo exige reproducir y
 * eliminar para personalizadas).
 *
 * @param onExplore atajo del estado vacío al catálogo.
 */
@Composable
fun CustomStationsScreen(
    onExplore: () -> Unit,
    viewModel: CustomStationsViewModel = hiltViewModel(),
    player: PlayerViewModel = hiltViewModel(),
    viewModeVm: ViewModeViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.ui.collectAsState()
    val form by viewModel.form.collectAsState()
    val mode by viewModeVm.mode.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    // El Scaffold externo (TolochaNavGraph) ya aplica los insets; el interno
    // solo aloja el Snackbar y no debe reañadir el inset superior (bug 0038).
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            SectionHeader(
                title = "Mis emisoras",
                subtitle = "Añade emisoras que no están en el catálogo para escucharlas desde el reproductor.",
            )
            CustomStationForm(
                form = form,
                onNameChange = viewModel::onNameChange,
                onUrlChange = viewModel::onUrlChange,
                onSubmit = viewModel::onSubmit,
            )
            CustomStationsContent(
                state = ui,
                mode = mode,
                onExplore = onExplore,
                onPlay = { player.play(it) },
                onDelete = viewModel::onDelete,
                onRetry = viewModel::retry,
            )
        }
    }
}

/** Formulario de alta, siempre visible aunque la lista esté vacía o en error (FR-003). */
@Composable
private fun CustomStationForm(
    form: CustomStationFormState,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChange,
                label = { Text("Nombre") },
                placeholder = { Text("Nombre de la emisora") },
                isError = form.nameError != null,
                supportingText = form.nameError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.url,
                onValueChange = onUrlChange,
                label = { Text("URL del stream") },
                placeholder = { Text("https://stream.ejemplo.org/live.mp3") },
                isError = form.urlError != null,
                supportingText = form.urlError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSubmit,
                enabled = !form.submitting,
                modifier = Modifier.align(Alignment.End),
            ) {
                if (form.submitting) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Añadir")
            }
        }
    }
}

/** Contenido bajo el formulario según el estado de la lista. */
@Composable
private fun CustomStationsContent(
    state: CustomStationsUiState,
    mode: ViewMode = ViewMode.LIST,
    onExplore: () -> Unit,
    onPlay: (StationDto) -> Unit,
    onDelete: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        CustomStationsUiState.Loading ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        CustomStationsUiState.Empty ->
            EmptyState(
                title = "Aún no tienes emisoras personalizadas",
                actionLabel = "Explorar",
                onAction = onExplore,
            )
        is CustomStationsUiState.Error -> ErrorBanner(state.message, onRetry = onRetry)
        is CustomStationsUiState.Content ->
            Column(Modifier.fillMaxSize()) {
                if (state.offline) {
                    Text(
                        "Mostrando caché sin conexión.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                if (mode == ViewMode.GRID) {
                    // Tarjeta de cuadrícula propia: el corazón de StationCard
                    // significa "favoritos" y aquí la acción de lista es borrar.
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items, key = { it.id }) { station ->
                            CustomStationGridCard(
                                station = station,
                                isDeleting = station.id in state.pendingDeletes,
                                onPlay = { onPlay(station) },
                                onDelete = { onDelete(station.id) },
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(state.items, key = { it.id }) { station ->
                            CustomStationRow(
                                station = station,
                                isDeleting = station.id in state.pendingDeletes,
                                onPlay = { onPlay(station) },
                                onDelete = { onDelete(station.id) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tus emisoras personalizadas usan el emblema de TolochaRadio al no disponer de imagen propia.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
    }
}

@Composable
private fun CustomStationRow(
    station: StationDto,
    isDeleting: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationArtwork(station, Modifier.size(48.dp), isCustom = true)
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onPlay).padding(vertical = 4.dp),
        ) {
            Text(
                station.name.ifBlank { "Emisora sin nombre" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                station.url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir")
        }
        IconButton(onClick = onDelete, enabled = !isDeleting) {
            val tint =
                if (isDeleting) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar ${station.name}",
                tint = tint,
            )
        }
    }
}

/**
 * Tarjeta de cuadrícula de emisora personalizada (spec 008): artwork 16:9
 * con emblema, nombre y URL; play directo y borrar con las mismas
 * descripciones de accesibilidad que la lista (FR-008).
 */
@Composable
private fun CustomStationGridCard(
    station: StationDto,
    isDeleting: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onPlay,
        shape = MaterialTheme.shapes.large,
        colors =
            androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(GRID_ARTWORK_ASPECT_RATIO)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(GRID_SCRIM_COLOR)),
                        ),
                    ),
        ) {
            StationArtwork(
                station,
                Modifier.matchParentSize().clip(MaterialTheme.shapes.large),
                isCustom = true,
            )
            IconButton(onClick = onPlay, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).size(36.dp)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir")
            }
            IconButton(
                onClick = onDelete,
                enabled = !isDeleting,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(36.dp),
            ) {
                val tint =
                    if (isDeleting) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${station.name}", tint = tint)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(
                station.name.ifBlank { "Emisora sin nombre" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                station.url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
