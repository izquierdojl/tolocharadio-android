package com.izquierdojl.tolocharadio.feature.podcasts.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.ErrorBanner
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.feature.stats.blocks.HabitsHeatmap
import com.izquierdojl.tolocharadio.feature.stats.blocks.RankingBars
import com.izquierdojl.tolocharadio.feature.stats.blocks.RecentActivityList
import com.izquierdojl.tolocharadio.feature.stats.blocks.ShareBars
import com.izquierdojl.tolocharadio.feature.stats.blocks.StatsSummaryItem
import com.izquierdojl.tolocharadio.feature.stats.blocks.StatsTimelineChart
import com.izquierdojl.tolocharadio.feature.stats.blocks.SummaryCard

/**
 * Estadísticas privadas de podcast con el mismo lenguaje de gráficas
 * que radio (US6, FR-049–FR-058).
 */
@Composable
fun PodcastStatsScreen(
    viewModel: PodcastStatsViewModel = hiltViewModel(),
    onEditServer: () -> Unit = {},
) {
    val ui by viewModel.ui.collectAsState()
    PodcastStatsContent(
        state = ui,
        onRetry = viewModel::retry,
        onEditServer = onEditServer,
    )
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onForeground() }
}

/** Contenido puro de Gráficas de podcast, testeable sin Hilt. */
@Composable
internal fun PodcastStatsContent(
    state: PodcastStatsUi,
    onRetry: () -> Unit,
    onEditServer: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        SectionHeader(title = "Tus estadísticas de podcast", subtitle = "Solo tú puedes verlas.")
        when (state) {
            PodcastStatsUi.Loading ->
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            PodcastStatsUi.Empty ->
                EmptyState(
                    title = "Todavía no hay escucha de podcast. Reproduce un episodio y vuelve aquí.",
                )
            is PodcastStatsUi.Error ->
                ErrorBanner(
                    message = state.message,
                    onRetry = if (state.isAuthError) onEditServer else onRetry,
                    retryLabel = if (state.isAuthError) "Editar servidor" else "Reintentar",
                )
            is PodcastStatsUi.Content -> ContentBlocks(state)
        }
    }
}

@Composable
private fun ContentBlocks(state: PodcastStatsUi.Content) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "summary") { SummaryCards(state.summary.toSummaryItems()) }
        item(key = "timeline") {
            StatsTimelineChart(
                timeline = state.timeline.toStatsTimeline(),
                granularity = state.timeline.granularity,
            )
        }
        item(key = "top-podcasts") {
            RankingBars(
                title = "Programas más escuchados",
                items = state.topPodcasts.toRankingItems(),
            )
        }
        item(key = "top-episodes") {
            RankingBars(
                title = "Episodios más escuchados",
                items = state.topEpisodes.toRankingItems(),
            )
        }
        item(key = "habits") { HabitsHeatmap(state.habitsMatrix) }
        item(key = "categories") {
            ShareBars(
                title = "Categorías",
                entries = state.categories.toShareItems(),
            )
        }
        item(key = "recent") {
            RecentActivityList(
                title = "Actividad reciente",
                items = state.recent.toRecentItems(),
            )
        }
    }
}

@Composable
private fun SummaryCards(items: List<StatsSummaryItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            SummaryCard(label = item.label, value = item.value, hint = item.hint)
        }
    }
}
