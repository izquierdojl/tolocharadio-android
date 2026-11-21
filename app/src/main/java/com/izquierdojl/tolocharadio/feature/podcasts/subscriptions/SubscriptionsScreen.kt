package com.izquierdojl.tolocharadio.feature.podcasts.subscriptions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.PodcastCard
import com.izquierdojl.tolocharadio.core.ui.components.PodcastRow
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.components.ViewModeToggle
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.domain.podcasts.filterSubscriptionsByTitle
import com.izquierdojl.tolocharadio.feature.podcasts.SubscriptionSearchScope
import com.izquierdojl.tolocharadio.feature.podcasts.SubscriptionsUiState
import com.izquierdojl.tolocharadio.feature.podcasts.SubscriptionsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Suscripciones propias (US2, FR-009): lista o tarjetas, orden por
 * nombre/episodios recientes y baja con confirmación. Toda la fila abre
 * la ficha; la baja es un icono (paridad con la web).
 */
@Composable
@Suppress("CyclomaticComplexMethod")
fun SubscriptionsScreen(
    onDiscover: () -> Unit,
    onOpenShow: (Long) -> Unit,
    snackbar: SnackbarHostState,
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val search by viewModel.searchUi.collectAsState()
    val message by viewModel.message.collectAsState()
    val manualOrder by viewModel.manualOrder.collectAsState()
    var viewMode by rememberSaveable { mutableStateOf(ViewMode.LIST) }
    var order by rememberSaveable { mutableStateOf(SubscriptionSort.RECENT_EPISODES) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var dragOrder by remember { mutableStateOf<List<Long>?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var pendingUnsubscribe by remember { mutableStateOf<PodcastSummary?>(null) }

    BackHandler(enabled = search.showResults) {
        viewModel.closeEpisodeSearch()
    }

    LaunchedEffect(ui) {
        if (ui !is SubscriptionsUiState.Content) {
            searchVisible = false
            viewModel.setSearchTerm("")
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val content = ui as? SubscriptionsUiState.Content
    val subtitle =
        content?.let { c ->
            val total = c.items.size
            "$total ${if (total == 1) "suscripción" else "suscripciones"}"
        } ?: "Tus programas."

    Column(Modifier.fillMaxSize()) {
        SectionHeader(
            title = "Suscripciones",
            subtitle = subtitle,
            actions = {
                if (ui is SubscriptionsUiState.Content) {
                    IconButton(
                        onClick = {
                            val next = !searchVisible
                            searchVisible = next
                            if (!next) {
                                viewModel.setSearchTerm("")
                                viewModel.setSearchScope(SubscriptionSearchScope.PODCASTS)
                            }
                        },
                        modifier = Modifier.testTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_TOGGLE),
                    ) {
                        Icon(
                            if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription =
                                stringResource(
                                    if (searchVisible) {
                                        R.string.subscriptions_search_hide
                                    } else {
                                        R.string.subscriptions_search_show
                                    },
                                ),
                        )
                    }
                }
                if (!search.showResults) {
                    SubscriptionSortMenu(
                        order = order,
                        onSelect = {
                            order = it
                            dragOrder = null
                            if (it == SubscriptionSort.MANUAL) viewMode = ViewMode.LIST
                        },
                    )
                    ViewModeToggle(
                        mode = viewMode,
                        onToggle = {
                            viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                        },
                    )
                }
            },
        )
        AnimatedVisibility(
            visible = searchVisible && ui is SubscriptionsUiState.Content,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            SubscriptionSearchBar(
                term = search.term,
                scope = search.scope,
                searching = search.loading,
                onTermChange = viewModel::setSearchTerm,
                onScopeChange = viewModel::setSearchScope,
                onSubmit = viewModel::searchEpisodes,
                onClear = { viewModel.setSearchTerm("") },
            )
        }
        when (val state = ui) {
            is SubscriptionsUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            is SubscriptionsUiState.Empty ->
                EmptyState(
                    title = "Aún no sigues ningún podcast",
                    actionLabel = "Descubrir podcasts",
                    onAction = onDiscover,
                )
            is SubscriptionsUiState.Error ->
                Column {
                    Text(state.message, modifier = Modifier.padding(16.dp))
                    TextButton(onClick = viewModel::refresh) {
                        Text("Reintentar")
                    }
                }
            is SubscriptionsUiState.Content -> {
                if (search.showResults) {
                    SubscriptionSearchHost(
                        state = search,
                        snackbar = snackbar,
                        onLoadMore = viewModel::loadMoreEpisodes,
                        onRetry = viewModel::searchEpisodes,
                        onProgressChanged = viewModel::onEpisodeProgressChanged,
                    )
                } else {
                    val items = sortSubscriptions(state.items, order, dragOrder ?: manualOrder)
                    val listFiltered =
                        search.scope == SubscriptionSearchScope.PODCASTS && search.term.isNotBlank()
                    val visible = visibleSubscriptions(items, search.term, search.scope)
                    if (visible.isEmpty() && listFiltered) {
                        Text(
                            stringResource(
                                R.string.subscriptions_search_no_podcasts,
                                search.term.trim(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        if (order == SubscriptionSort.MANUAL &&
                            viewMode == ViewMode.LIST &&
                            !listFiltered
                        ) {
                            Text(
                                "Arrastra el asa para ordenar. El orden se guarda en este dispositivo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        if (viewMode == ViewMode.GRID) {
                            SubscriptionsGrid(
                                items = visible,
                                onOpenShow = onOpenShow,
                                onUnsubscribe = { pendingUnsubscribe = it },
                            )
                        } else {
                            SubscriptionsList(
                                items = visible,
                                manual = order == SubscriptionSort.MANUAL && !listFiltered,
                                draggingId = draggingId,
                                onMove = { from, to ->
                                    val ids = (dragOrder ?: items.map { it.id }).toMutableList()
                                    if (from in ids.indices && to in ids.indices && from != to) {
                                        ids.add(to, ids.removeAt(from))
                                        dragOrder = ids
                                    }
                                },
                                onCommit = {
                                    viewModel.saveManualOrder(dragOrder ?: items.map { it.id })
                                },
                                onDragStateChange = { id -> draggingId = id },
                                onOpenShow = onOpenShow,
                                onUnsubscribe = { pendingUnsubscribe = it },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingUnsubscribe?.let { podcast ->
        AlertDialog(
            onDismissRequest = { pendingUnsubscribe = null },
            title = { Text("¿Cancelar la suscripción?") },
            text = { Text("Dejarás de seguir «${podcast.title}». Podrás volver a suscribirte cuando quieras.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unsubscribe(podcast.id)
                        pendingUnsubscribe = null
                    },
                ) {
                    Text("Cancelar suscripción", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnsubscribe = null }) {
                    Text("Mantener")
                }
            },
        )
    }
}

/** Lista de suscripciones con asa de arrastre cuando el orden es manual. */
@Composable
@Suppress("LongParameterList")
private fun SubscriptionsList(
    items: List<PodcastSummary>,
    manual: Boolean,
    draggingId: Long?,
    onMove: (Int, Int) -> Unit,
    onCommit: () -> Unit,
    onDragStateChange: (Long?) -> Unit,
    onOpenShow: (Long) -> Unit,
    onUnsubscribe: (PodcastSummary) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var columnTop by remember { mutableFloatStateOf(Float.NaN) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    LazyColumn(
        state = listState,
        modifier = Modifier.onGloballyPositioned { columnTop = it.positionInWindow().y },
    ) {
        items(items, key = { it.id }) { summary ->
            val dragHandle =
                if (manual) {
                    Modifier.reorderDragHandle(
                        key = summary.id,
                        listState = listState,
                        columnTop = { columnTop },
                        indexOf = { items.indexOfFirst { it.id == summary.id } },
                        callbacks =
                            ReorderDragCallbacks(
                                onMove = onMove,
                                onCommit = onCommit,
                                onAutoScroll = { dy ->
                                    if (scrollJob?.isActive != true) {
                                        scrollJob = scope.launch { listState.scrollBy(dy) }
                                    }
                                },
                                onDragStateChange = { active ->
                                    onDragStateChange(if (active) summary.id else null)
                                },
                            ),
                    )
                } else {
                    null
                }
            PodcastRow(
                summary = summary,
                onOpen = { onOpenShow(summary.id) },
                onUnsubscribe = { onUnsubscribe(summary) },
                modifier = Modifier.zIndex(if (draggingId == summary.id) 1f else 0f),
                dragHandle = dragHandle,
            )
        }
    }
}

/** Cuadrícula de suscripciones (2 columnas de tarjetas). */
@Composable
private fun SubscriptionsGrid(
    items: List<PodcastSummary>,
    onOpenShow: (Long) -> Unit,
    onUnsubscribe: (PodcastSummary) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { summary ->
            PodcastCard(
                summary = summary,
                onOpen = { onOpenShow(summary.id) },
                onUnsubscribe = { onUnsubscribe(summary) },
            )
        }
    }
}

/** Menú de orden anclado al icono de la cabecera (paridad con la web). */
@Composable
private fun SubscriptionSortMenu(
    order: SubscriptionSort,
    onSelect: (SubscriptionSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Sort, contentDescription = "Ordenar suscripciones")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SubscriptionSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = {
                        Row(Modifier.size(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (option == order) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/**
 * Lista visible según el ámbito: en «Podcasts» se filtra por título; en
 * «Episodios» la lista permanece completa hasta que hay resultados.
 */
internal fun visibleSubscriptions(
    items: List<PodcastSummary>,
    term: String,
    scope: SubscriptionSearchScope,
): List<PodcastSummary> =
    if (scope == SubscriptionSearchScope.PODCASTS) {
        filterSubscriptionsByTitle(items, term)
    } else {
        items
    }
