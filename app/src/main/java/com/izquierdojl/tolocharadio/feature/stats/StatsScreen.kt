package com.izquierdojl.tolocharadio.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.ErrorBanner
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.domain.stats.StatsPeriod
import com.izquierdojl.tolocharadio.feature.stats.blocks.GenreCountryCharts
import com.izquierdojl.tolocharadio.feature.stats.blocks.HabitsHeatmap
import com.izquierdojl.tolocharadio.feature.stats.blocks.RecentList
import com.izquierdojl.tolocharadio.feature.stats.blocks.StatsSummaryCards
import com.izquierdojl.tolocharadio.feature.stats.blocks.StatsTimelineChart
import com.izquierdojl.tolocharadio.feature.stats.blocks.TopStationsChart

/**
 * Gráficas de escucha (entrada: fila "Gráficas" en Configuración,
 * FR-001). [StatsScreenContent] es stateless para el test de UI.
 */
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.ui.collectAsState()
    val period by viewModel.period.collectAsState()
    StatsScreenContent(
        state = state,
        period = period,
        onPeriodChange = viewModel::onPeriodChange,
        onRetry = viewModel::retry,
    )
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onForeground() }
}

/** Contenido puro de la vista: filtro de periodo + estados carga/vacío/error/contenido. */
@Composable
fun StatsScreenContent(
    state: StatsUiState,
    period: StatsPeriod,
    onPeriodChange: (StatsPeriod) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        SectionHeader(title = "Tus estadísticas")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Lo que escuchas, resumido en privado: solo tú puedes ver estos datos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            PeriodFilter(period = period, onPeriodChange = onPeriodChange)
        }
        Spacer(Modifier.height(8.dp))
        when (val current = state) {
            StatsUiState.Loading -> LoadingBlock()
            StatsUiState.Empty ->
                EmptyState(
                    title = "Todavía no hay escucha en este periodo. Reproduce una emisora y vuelve aquí.",
                )
            is StatsUiState.Error -> ErrorBanner(message = current.message, onRetry = onRetry)
            is StatsUiState.Content -> ContentBlocks(current)
        }
    }
}

/** Filtro 7/30/90/todo con defecto en 30 días (FR-003). */
@Composable
private fun PeriodFilter(
    period: StatsPeriod,
    onPeriodChange: (StatsPeriod) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        StatsPeriod.entries.forEachIndexed { index, candidate ->
            SegmentedButton(
                selected = period == candidate,
                onClick = { onPeriodChange(candidate) },
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = StatsPeriod.entries.size,
                    ),
                label = { Text(candidate.label()) },
            )
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ContentBlocks(state: StatsUiState.Content) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "summary") { StatsSummaryCards(state.summary) }
        item(key = "timeline") {
            StatsTimelineChart(
                timeline = state.bundle.timeline,
                granularity = state.granularity.wire,
            )
        }
        item(key = "top") { TopStationsChart(state.bundle.top) }
        item(key = "habits") { HabitsHeatmap(state.habitsMatrix) }
        item(key = "shares") {
            GenreCountryCharts(
                genres = state.bundle.genres,
                countries = state.bundle.countries,
            )
        }
        item(key = "recent") { RecentList(state.bundle.recent) }
    }
}

private fun StatsPeriod.label(): String =
    when (this) {
        StatsPeriod.SEVEN -> "7 días"
        StatsPeriod.THIRTY -> "30 días"
        StatsPeriod.NINETY -> "90 días"
        StatsPeriod.ALL -> "Todo"
    }
