package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.core.ui.playback.StationPlaybackUiState
import com.izquierdojl.tolocharadio.core.ui.theme.LocalTolochaColors
import com.izquierdojl.tolocharadio.core.ui.theme.PlayActiveBackground
import com.izquierdojl.tolocharadio.core.ui.theme.PlayActiveContent
import com.izquierdojl.tolocharadio.core.ui.theme.PlayRestBackground
import com.izquierdojl.tolocharadio.core.ui.theme.PlayRestContent
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto

/** Relación de aspecto 16:9 de la carátula de [StationCard] (paridad con `StationCard` web). */
private const val CARD_ARTWORK_ASPECT_RATIO = 16f / 9f

/** Velo inferior sobre la carátula para legibilidad del botón de play (pine-950 ~80%). */
private const val CARD_SCRIM_COLOR = 0xCC08100B

/** Nº máximo de etiquetas visibles en [StationCard] (paridad con `StationCard` web). */
private const val MAX_VISIBLE_TAGS = 3

/** Estado vacío con mensaje y acción (paridad con `EmptyState` web, tono muted). */
@Composable
fun EmptyState(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.sierra_emblem),
            contentDescription = null,
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Aviso de error con acción primaria (reintentar o editar servidor). El texto ya viene localizado. */
@Composable
fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Reintentar",
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = onRetry) { Text(retryLabel) }
    }
}

/** Botón de favorito omnipresente (paridad con `FavoriteButton` web). */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Logo de la emisora. Sin favicon muestra el emblema Sierra (paridad web:
 * las custom-stations usan `SierraEmblem` como placeholder); con favicon
 * usa Coil con fallback al emblema si falla la carga.
 */
@Composable
fun StationArtwork(
    station: StationDto,
    modifier: Modifier = Modifier,
    isCustom: Boolean = false,
) {
    if (station.favicon.isNullOrBlank() || isCustom) {
        Image(
            painter = painterResource(R.drawable.sierra_emblem),
            contentDescription = station.name,
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier = modifier) {
            Image(
                painter = painterResource(R.drawable.sierra_emblem),
                contentDescription = null,
                modifier = Modifier.matchParentSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            AsyncImage(
                model = station.favicon,
                contentDescription = station.name,
                modifier = Modifier.matchParentSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** Chip de etiqueta web: mayúsculas, tracking amplio, fondo pine-800 en dark. */
@Composable
fun TagChip(
    tag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            tag.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * Tarjeta de emisora con jerarquía web (`StationCard.tsx`): imagen 16:9 con
 * degradado inferior, play circular abajo-izquierda (ocre al reproducir),
 * favorito arriba-derecha, título, país · idioma, ≤3 chips y bitrate.
 *
 * `onOpen` abre la ficha; `onPlay` reproduce o alterna play/pausa. La tarjeta
 * activa muestra pausa y un badge no basado solo en color.
 */
@Suppress("CyclomaticComplexMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationCard(
    station: StationDto,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    playback: StationPlaybackUiState = StationPlaybackUiState.Inactive,
    isCustom: Boolean = false,
) {
    val colors = LocalTolochaColors.current
    val isActive =
        playback == StationPlaybackUiState.Playing || playback == StationPlaybackUiState.Paused
    Card(
        onClick = onOpen,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardContainer),
        border = BorderStroke(1.dp, colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 2.dp),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(CARD_ARTWORK_ASPECT_RATIO)
                        .background(colors.cardPlaceholder),
            ) {
                if (station.favicon.isNullOrBlank() || isCustom) {
                    Image(
                        painter = painterResource(R.drawable.sierra_emblem),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(96.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.sierra_emblem),
                        fallback = painterResource(R.drawable.sierra_emblem),
                    )
                }
                Box(
                    modifier =
                        Modifier.matchParentSize().background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(CARD_SCRIM_COLOR)),
                            ),
                        ),
                )
                if (playback != StationPlaybackUiState.Inactive) {
                    StationPlaybackBadge(
                        state = playback,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    )
                }
                FilledIconButton(
                    onClick = onPlay,
                    enabled = playback != StationPlaybackUiState.Loading,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(48.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isActive) PlayActiveBackground else PlayRestBackground,
                            contentColor = if (isActive) PlayActiveContent else PlayRestContent,
                        ),
                ) {
                    when (playback) {
                        StationPlaybackUiState.Playing ->
                            Icon(Icons.Filled.Pause, contentDescription = "Pausar", modifier = Modifier.size(24.dp))
                        StationPlaybackUiState.Loading ->
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PlayRestContent,
                            )
                        StationPlaybackUiState.Error ->
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Reintentar",
                                modifier = Modifier.size(22.dp),
                            )
                        StationPlaybackUiState.Paused ->
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Reanudar",
                                modifier = Modifier.size(26.dp),
                            )
                        StationPlaybackUiState.Inactive ->
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Reproducir",
                                modifier = Modifier.size(26.dp),
                            )
                    }
                }
                FavoriteButton(
                    isFavorite = isFavorite,
                    onToggle = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    station.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    listOfNotNull(station.country, station.language).joinToString(" · ").ifBlank { "Emisora" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    station.tags.take(MAX_VISIBLE_TAGS).forEach {
                        TagChip(it)
                        Spacer(Modifier.width(4.dp))
                    }
                    station.bitrate?.let {
                        Spacer(Modifier.weight(1f))
                        Text(
                            "$it kbps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Fila densa de emisora (lista) con placeholder Sierra. */
@Composable
fun StationListItem(
    station: StationDto,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    playback: StationPlaybackUiState = StationPlaybackUiState.Inactive,
    isCustom: Boolean = false,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onOpen),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    station.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (playback != StationPlaybackUiState.Inactive) {
                    StationPlaybackBadge(
                        state = playback,
                        compact = true,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        },
        supportingContent = {
            Text(
                listOfNotNull(station.country, station.language, station.tags.firstOrNull())
                    .joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            if (station.favicon.isNullOrBlank() || isCustom) {
                Icon(
                    Icons.Filled.Radio,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                StationArtwork(station, Modifier.size(48.dp))
            }
        },
        trailingContent = { FavoriteButton(isFavorite = isFavorite, onToggle = onToggleFavorite) },
    )
}

@Preview(name = "StationCard dark", showBackground = true, backgroundColor = 0xFF08100B)
@Composable
internal fun StationCardDarkPreview() {
    TolochaTheme(darkTheme = true) {
        StationCard(
            station =
                StationDto(
                    id = "preview",
                    name = "Radio Sierra",
                    country = "España",
                    language = "español",
                ),
            isFavorite = true,
            playback = StationPlaybackUiState.Playing,
            onOpen = {},
            onPlay = {},
            onToggleFavorite = {},
        )
    }
}

@Preview(name = "StationCard light", showBackground = true, backgroundColor = 0xFFEEF3EF)
@Composable
internal fun StationCardLightPreview() {
    TolochaTheme(darkTheme = false) {
        StationCard(
            station =
                StationDto(
                    id = "preview",
                    name = "Radio Sierra",
                    country = "España",
                    language = "español",
                ),
            isFavorite = false,
            playback = StationPlaybackUiState.Inactive,
            onOpen = {},
            onPlay = {},
            onToggleFavorite = {},
        )
    }
}
