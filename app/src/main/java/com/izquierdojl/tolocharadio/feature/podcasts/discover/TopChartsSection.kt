package com.izquierdojl.tolocharadio.feature.podcasts.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.PodcastSearchRow
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.domain.podcasts.TopCountry
import com.izquierdojl.tolocharadio.domain.podcasts.TopGenre
import com.izquierdojl.tolocharadio.feature.podcasts.TopChartsUi

/**
 * Sección «Populares» sin estado. La visibilidad la decide `DiscoverContent`;
 * esta composición solo renderiza carga, contenido, vacío, error y aviso de
 * fallback. Toda la interacción se delega en callbacks.
 *
 * Expone [topChartsItems] para que `DiscoverContent` pueda integrarla en un
 * único `LazyColumn` y desplazar toda la pantalla junta (bug 0059).
 */
@Suppress("LongParameterList")
@Composable
fun TopChartsSection(
    ui: TopChartsUi,
    subscribedFeedUrls: Set<String>,
    onCountryChange: (TopCountry) -> Unit,
    onGenreChange: (TopGenre) -> Unit,
    onToggleSubscribe: (PodcastSearchResult) -> Unit,
    onRetry: () -> Unit,
    onEditServer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(TolochaTestTags.TOP_CHARTS_SECTION),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TolochaTestTags.TOP_CHARTS_LIST),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            topChartsItems(
                ui = ui,
                subscribedFeedUrls = subscribedFeedUrls,
                onCountryChange = onCountryChange,
                onGenreChange = onGenreChange,
                onToggleSubscribe = onToggleSubscribe,
                onRetry = onRetry,
                onEditServer = onEditServer,
            )
        }
    }
}

/**
 * Ítems de «Populares» dentro del `LazyListScope` de la pantalla. No usan
 * `weight`: el alto lo marca el contenido y el scroll lo gestiona quien
 * contiene la lista. La longitud es declarativa, como la de un `@Composable`.
 */
@Suppress("LongMethod")
fun LazyListScope.topChartsItems(
    ui: TopChartsUi,
    subscribedFeedUrls: Set<String>,
    onCountryChange: (TopCountry) -> Unit,
    onGenreChange: (TopGenre) -> Unit,
    onToggleSubscribe: (PodcastSearchResult) -> Unit,
    onRetry: () -> Unit,
    onEditServer: () -> Unit = {},
) {
    item {
        Text(
            text = "Populares",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        )
    }
    item {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CountrySelector(
                country = ui.country,
                onSelect = onCountryChange,
                modifier = Modifier.weight(1f),
            )
            GenreSelector(
                genre = ui.genre,
                onSelect = onGenreChange,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (ui.genreUnavailable) {
        item { GenreUnavailableNotice(onRetry = onRetry) }
    }

    when {
        ui.loading -> {
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Cargando ranking…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        ui.error != null -> {
            item {
                TopChartsError(
                    message = ui.error,
                    isAuthError = ui.isAuthError,
                    onRetry = onRetry,
                    onEditServer = onEditServer,
                )
            }
        }

        ui.items.isEmpty() -> {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag(TolochaTestTags.TOP_CHARTS_EMPTY),
                ) {
                    EmptyState(title = "No hay ranking disponible para esta selección.")
                }
            }
        }

        else -> {
            itemsIndexed(ui.items, key = { _, result -> result.feedUrl }) { index, result ->
                PodcastSearchRow(
                    result = result,
                    subscribed = result.feedUrl in subscribedFeedUrls,
                    onToggleSubscribe = { onToggleSubscribe(result) },
                    position = index + 1,
                )
            }
        }
    }
}

@Composable
private fun CountrySelector(
    country: TopCountry,
    onSelect: (TopCountry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TolochaTestTags.TOP_CHARTS_COUNTRY)
                    .semantics {
                        contentDescription = "País del ranking"
                        stateDescription = country.label
                    },
        ) {
            Text(country.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp),
        ) {
            TopCountry.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun GenreSelector(
    genre: TopGenre,
    onSelect: (TopGenre) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TolochaTestTags.TOP_CHARTS_GENRE)
                    .semantics {
                        contentDescription = "Categoría del ranking"
                        stateDescription = genre.label
                    },
        ) {
            Text(genre.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp),
        ) {
            TopGenre.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun GenreUnavailableNotice(onRetry: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag(TolochaTestTags.TOP_CHARTS_NOTICE),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "El ranking por categoría no está disponible ahora. Mostrando el ranking general.",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        TextButton(
            onClick = onRetry,
            modifier = Modifier.testTag(TolochaTestTags.TOP_CHARTS_RETRY),
        ) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun TopChartsError(
    message: String,
    isAuthError: Boolean,
    onRetry: () -> Unit,
    onEditServer: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag(TolochaTestTags.TOP_CHARTS_STATUS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = if (isAuthError) onEditServer else onRetry,
            modifier =
                Modifier.testTag(
                    if (isAuthError) {
                        TolochaTestTags.TOP_CHARTS_AUTH_ACTION
                    } else {
                        TolochaTestTags.TOP_CHARTS_RETRY
                    },
                ),
        ) {
            Text(if (isAuthError) "Editar servidor" else "Reintentar")
        }
    }
}
