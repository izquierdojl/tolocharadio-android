package com.izquierdojl.tolocharadio.feature.podcasts.discover

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.PodcastSearchRow
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.domain.podcasts.TopCountry
import com.izquierdojl.tolocharadio.domain.podcasts.TopGenre
import com.izquierdojl.tolocharadio.feature.podcasts.DiscoverUi
import com.izquierdojl.tolocharadio.feature.podcasts.TopChartsUi

/**
 * Contenido sin estado de Descubrir. Separa la pantalla de Hilt para poder
 * probar la exclusión ranking/búsqueda y los controles no bloqueados.
 */
@Suppress("LongParameterList")
@Composable
fun DiscoverContent(
    ui: DiscoverUi,
    topChartsUi: TopChartsUi,
    manualUrl: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onManualUrlChange: (String) -> Unit,
    onAddManual: () -> Unit,
    onImportOpml: () -> Unit,
    onExportOpml: () -> Unit,
    onBrowseSubscriptions: () -> Unit,
    onToggleSubscribe: (PodcastSearchResult) -> Unit,
    onCountryChange: (TopCountry) -> Unit,
    onGenreChange: (TopGenre) -> Unit,
    onRetryTopCharts: () -> Unit,
    onEditServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        SectionHeader(title = "Descubrir", subtitle = "Busca programas o añade un RSS.")
        Column(Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = ui.query,
                onValueChange = onQueryChange,
                label = { Text("Buscar podcasts") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = onSearch, enabled = !ui.searching) {
                    Text("Buscar")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = manualUrl,
                onValueChange = onManualUrlChange,
                label = { Text("URL RSS manual") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedButton(onClick = onAddManual) {
                    Text("Añadir RSS")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onImportOpml) {
                    Text("Importar OPML")
                }
                TextButton(onClick = onExportOpml) {
                    Text("Exportar OPML")
                }
                TextButton(onClick = onBrowseSubscriptions) {
                    Text("Suscripciones")
                }
            }
        }

        when {
            ui.submittedQuery.isNotBlank() -> {
                when {
                    ui.searching -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                        }
                    }

                    ui.results.isEmpty() -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                        ) {
                            EmptyState(title = "Sin resultados")
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                        ) {
                            items(ui.results, key = { it.feedUrl }) { result ->
                                PodcastSearchRow(
                                    result = result,
                                    subscribed = result.feedUrl in ui.subscribedFeedUrls,
                                    onToggleSubscribe = { onToggleSubscribe(result) },
                                )
                            }
                        }
                    }
                }
            }

            topChartsUi.visible -> {
                TopChartsSection(
                    ui = topChartsUi,
                    subscribedFeedUrls = ui.subscribedFeedUrls,
                    onCountryChange = onCountryChange,
                    onGenreChange = onGenreChange,
                    onToggleSubscribe = onToggleSubscribe,
                    onRetry = onRetryTopCharts,
                    onEditServer = onEditServer,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )
            }

            else -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                ) {
                    EmptyState(title = "Sin resultados")
                }
            }
        }
    }
}
