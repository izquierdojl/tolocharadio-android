package com.izquierdojl.tolocharadio.feature.stats.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.data.remote.dto.StatsCountryEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsCountryListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsGenreListDto
import com.izquierdojl.tolocharadio.feature.stats.StatsFormat

private const val BAR_HEIGHT_DP = 6
private const val MIN_BAR_FRACTION = 0.02f

/** Paleta de sectores espejo de `CHART_COLORS` de la web (R1, Tema Tolocha). */
private const val COLOR_OCHRE = 0xFFC0883E
private const val COLOR_PINE = 0xFF5F8F73
private const val COLOR_SAGE = 0xFFB4C47E
private const val COLOR_SAND = 0xFFD3A568
private const val COLOR_MINT = 0xFF8CB29A
private const val COLOR_BARK = 0xFFA67430
private const val COLOR_FOREST = 0xFF3C6A4D
private const val COLOR_CREAM = 0xFFE2C091
private const val COLOR_LIME = 0xFFCFDAA2
private const val COLOR_ESPRESSO = 0xFF6B4A1D

private val SHARE_COLORS =
    listOf(
        Color(COLOR_OCHRE),
        Color(COLOR_PINE),
        Color(COLOR_SAGE),
        Color(COLOR_SAND),
        Color(COLOR_MINT),
        Color(COLOR_BARK),
        Color(COLOR_FOREST),
        Color(COLOR_CREAM),
        Color(COLOR_LIME),
        Color(COLOR_ESPRESSO),
    )

/**
 * Repartos por género y por país con barras de color (R1): categorías
 * únicas incluidas, sin romper el diseño (US2-3).
 */
@Composable
fun GenreCountryCharts(
    genres: StatsGenreListDto,
    countries: StatsCountryListDto,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        ShareList(title = "Géneros favoritos", entries = genres.items.map { it.genre to it.totalMs })
        Spacer(Modifier.height(16.dp))
        ShareList(title = "Países", entries = countries.items.map { it.countryLabel() to it.totalMs })
    }
}

private fun StatsCountryEntryDto.countryLabel(): String {
    val code = countryCode?.takeIf { it.isNotBlank() } ?: return country
    return "$country ($code)"
}

@Composable
private fun ShareList(
    title: String,
    entries: List<Pair<String, Long>>,
) {
    val maxMs = entries.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        if (entries.isEmpty()) {
            Text(
                "Sin datos en este periodo",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        entries.forEachIndexed { index, (label, totalMs) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(BAR_HEIGHT_DP.dp)
                            .clip(RoundedCornerShape(BAR_HEIGHT_DP.dp))
                            .background(trackColor),
                    ) {
                        val fraction = (totalMs.toFloat() / maxMs).coerceIn(MIN_BAR_FRACTION, 1f)
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(BAR_HEIGHT_DP.dp)
                                .background(SHARE_COLORS[index % SHARE_COLORS.size]),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    StatsFormat.formatDurationMs(totalMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                )
            }
        }
    }
}
