package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary

/** Botón de suscripción (paridad con el alta de la web). */
@Composable
fun SubscribeButton(
    subscribed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onToggle, modifier = modifier) {
        Text(if (subscribed) "Suscrito" else "Suscribirse")
    }
}

/** Fila de resultado del directorio con portada y suscripción. */
@Composable
fun PodcastSearchRow(
    result: PodcastSearchResult,
    subscribed: Boolean,
    onToggleSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    position: Int? = null,
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(result.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                listOfNotNull(result.author, result.categories.firstOrNull())
                    .joinToString(" · "),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (position != null) {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                AsyncImage(
                    model = result.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.small),
                )
            }
        },
        trailingContent = {
            SubscribeButton(subscribed = subscribed, onToggle = onToggleSubscribe)
        },
    )
}

/**
 * Fila de programa suscrito: toda la tarjeta abre la ficha y la baja es un
 * icono (paridad con la web, que no muestra acciones redundantes).
 */
@Composable
fun PodcastRow(
    summary: PodcastSummary,
    onOpen: () -> Unit,
    onUnsubscribe: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    dragHandle: Modifier? = null,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onOpen),
        headlineContent = { Text(summary.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                listOfNotNull(
                    summary.author,
                    if (summary.episodeCount > 0) "${summary.episodeCount} episodios" else null,
                ).joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dragHandle != null) {
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = "Arrastrar para ordenar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = dragHandle.padding(end = 4.dp),
                    )
                }
                AsyncImage(
                    model = summary.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.small),
                )
            }
        },
        trailingContent = {
            if (onUnsubscribe != null) {
                IconButton(onClick = onUnsubscribe) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Cancelar suscripción",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

/**
 * Tarjeta de programa para la vista de cuadrícula: portada, título y
 * metadatos; tocar la tarjeta abre la ficha y el icono cancela la baja.
 */
@Composable
fun PodcastCard(
    summary: PodcastSummary,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onUnsubscribe: (() -> Unit)? = null,
) {
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth().testTag(TolochaTestTags.PODCAST_CARD),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
            ) {
                AsyncImage(
                    model = summary.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            }
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    summary.title,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    listOfNotNull(
                        summary.author,
                        if (summary.episodeCount > 0) "${summary.episodeCount} episodios" else null,
                    ).joinToString(" · "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onUnsubscribe != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(
                            onClick = onUnsubscribe,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Cancelar suscripción",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
