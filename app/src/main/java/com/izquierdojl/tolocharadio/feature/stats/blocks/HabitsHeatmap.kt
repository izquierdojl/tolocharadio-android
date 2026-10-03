package com.izquierdojl.tolocharadio.feature.stats.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private const val DAYS_PER_WEEK = 7
private const val HOURS_PER_DAY = 24
private const val GRID_HEIGHT_DP = 112

/** Proporción de separación entre celdas de la matriz (R1). */
private const val CELL_GAP_FRACTION = 0.15f

private const val MIN_ALPHA = 0.12f

/** Etiquetas de días en español: lunes..domingo (paridad con la web). */
private val WEEKDAY_LABELS = listOf("L", "M", "X", "J", "V", "S", "D")

/**
 * Matriz de hábitos día×hora con intensidad proporcional al tiempo
 * escuchado, dibujada en `Canvas` (R1, paridad con `HabitsHeatmap.tsx`).
 * La matriz es 7×24 (día 0..6 = lunes..domingo, hora 0..23).
 */
@Composable
fun HabitsHeatmap(
    matrix: List<List<Long>>,
    modifier: Modifier = Modifier,
) {
    val cellColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        Text("Tus hábitos de escucha", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        val hasData = matrix.any { row -> row.any { it > 0L } }
        if (!hasData) {
            Text(
                "Sin datos en este periodo",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        } else {
            val maxMs = matrix.maxOf { row -> row.max() }.coerceAtLeast(1L)
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    Modifier.height(GRID_HEIGHT_DP.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    WEEKDAY_LABELS.forEach { label ->
                        Text(label, style = MaterialTheme.typography.labelSmall, color = muted)
                    }
                }
                Spacer(Modifier.width(4.dp))
                Canvas(
                    Modifier
                        .weight(1f)
                        .height(GRID_HEIGHT_DP.dp)
                        .semantics {
                            contentDescription =
                                "Matriz de hábitos: días de la semana por horas, intensidad por tiempo escuchado"
                        },
                ) {
                    val cellWidth = size.width / HOURS_PER_DAY
                    val cellHeight = size.height / DAYS_PER_WEEK
                    val gapWidth = cellWidth * CELL_GAP_FRACTION
                    val gapHeight = cellHeight * CELL_GAP_FRACTION
                    for (day in 0 until DAYS_PER_WEEK) {
                        for (hour in 0 until HOURS_PER_DAY) {
                            val value = matrix.getOrNull(day)?.getOrNull(hour) ?: 0L
                            val ratio = (value.toFloat() / maxMs).coerceIn(0f, 1f)
                            drawRect(
                                color =
                                    if (value > 0L) {
                                        cellColor.copy(alpha = MIN_ALPHA + ratio * (1f - MIN_ALPHA))
                                    } else {
                                        emptyColor
                                    },
                                topLeft =
                                    Offset(
                                        hour * cellWidth + gapWidth / 2f,
                                        day * cellHeight + gapHeight / 2f,
                                    ),
                                size = Size(cellWidth - gapWidth, cellHeight - gapHeight),
                            )
                        }
                    }
                }
            }
            Text(
                "Cada columna es una hora, de 0 a 23",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
    }
}
