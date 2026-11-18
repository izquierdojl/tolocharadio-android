package com.izquierdojl.tolocharadio.feature.podcasts.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import java.util.concurrent.TimeUnit

/** Minutos por hora para el formato compacto. */
private const val MINUTES_PER_HOUR = 60L

/** Formato compacto de ms a "X h Y min". */
private fun formatMs(totalMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMs)
    val hours = minutes / MINUTES_PER_HOUR
    return if (hours > 0) "$hours h ${minutes % MINUTES_PER_HOUR} min" else "$minutes min"
}

/**
 * Estadísticas privadas de podcast (US6, FR-026): resumen, rankings,
 * evolución, hábitos, categorías y sesiones recientes.
 */
@Composable
fun PodcastStatsScreen(viewModel: PodcastStatsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsState()
    Column(Modifier.fillMaxSize()) {
        SectionHeader(title = "Estadísticas", subtitle = "Solo tú puedes verlas.")
        when (val state = ui) {
            is PodcastStatsUi.Loading ->
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            is PodcastStatsUi.Error ->
                Column {
                    Text(state.message, modifier = Modifier.padding(16.dp))
                    TextButton(onClick = viewModel::refresh) {
                        Text("Reintentar")
                    }
                }
            is PodcastStatsUi.Content -> {
                val summary = state.summary
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Text("Resumen", style = MaterialTheme.typography.titleSmall)
                    Text("Escuchado: ${formatMs(summary.totalMs)}")
                    Text("Programas: ${summary.podcastCount}")
                    Text("Episodios empezados: ${summary.startedEpisodes}")
                    Text("Episodios completados: ${summary.completedEpisodes}")
                    Text("Tasa de finalización: ${(summary.completionRate * 100).toInt()} %")
                    if (state.topPodcasts.items.isNotEmpty()) {
                        Text("Programas más escuchados", style = MaterialTheme.typography.titleSmall)
                        state.topPodcasts.items.forEach {
                            Text("${it.podcast.title} · ${formatMs(it.totalMs)}")
                        }
                    }
                    if (state.topEpisodes.items.isNotEmpty()) {
                        Text("Episodios más escuchados", style = MaterialTheme.typography.titleSmall)
                        state.topEpisodes.items.forEach {
                            Text("${it.episode.title} · ${formatMs(it.totalMs)}")
                        }
                    }
                    if (state.categories.items.isNotEmpty()) {
                        Text("Por categoría", style = MaterialTheme.typography.titleSmall)
                        state.categories.items.forEach {
                            Text("${it.category} · ${formatMs(it.totalMs)}")
                        }
                    }
                    if (state.recent.items.isNotEmpty()) {
                        Text("Sesiones recientes", style = MaterialTheme.typography.titleSmall)
                        state.recent.items.forEach {
                            Text("${it.episode?.title ?: "Episodio"} · ${formatMs(it.durationMs)}")
                        }
                    }
                }
            }
        }
    }
}
