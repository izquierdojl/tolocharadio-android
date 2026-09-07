package com.izquierdojl.tolocharadio.feature.player

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import java.text.NumberFormat
import java.util.Locale

/**
 * Bottom sheet modal que muestra información completa de la emisora
 * al tocar el logo/favicon (spec 010, US2, FR-004/FR-005).
 *
 * Layout: favicon + nombre → audio/stats → homepage → cerrar.
 * Dismissible sin interrumpir reproducción.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StationInfoSheet(
    station: StationDto,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            // Header: favicon + name + country/language
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = station.favicon,
                    contentDescription = "Logo de ${station.name}",
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        station.name.ifBlank { "Emisora" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val locationParts =
                        buildList {
                            station.country?.let { add(it) }
                            station.language?.let { add(it) }
                        }
                    if (locationParts.isNotEmpty()) {
                        Text(
                            locationParts.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Tags
            if (station.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    station.tags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                            colors =
                                SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Audio info + Stats
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Audio section
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Audio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    InfoRow("Codec", station.codec?.uppercase() ?: "—")
                    InfoRow("Bitrate", station.bitrate?.let { "$it kbps" } ?: "—")
                    InfoRow(
                        "SSL",
                        when (station.isSsl) {
                            true -> "Sí"
                            false -> "No"
                            null -> "—"
                        },
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Stats section
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Estadísticas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    InfoRow("Votos", station.votes?.let { numberFormat.format(it) } ?: "—")
                    InfoRow("Clicks", station.clickCount?.let { numberFormat.format(it) } ?: "—")
                    InfoRow(
                        "Último check",
                        when (station.lastCheckOk) {
                            true -> "OK"
                            false -> "Con problemas"
                            null -> "—"
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Homepage link
            station.homepage?.let { url ->
                Spacer(Modifier.height(12.dp))
                Text(
                    "Homepage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .semantics {
                                contentDescription = "Abrir sitio web de ${station.name}"
                            }
                            .clickable {
                                runCatching {
                                    ContextCompat.startActivity(
                                        context,
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                        null,
                                    )
                                }
                            },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Close button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Text("Cerrar")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
