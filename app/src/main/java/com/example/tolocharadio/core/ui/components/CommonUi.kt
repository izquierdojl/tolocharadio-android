package com.example.tolocharadio.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tolocharadio.data.remote.dto.StationDto

/** Estado vacío con mensaje y acción (paridad con `EmptyState` web). */
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
        Icon(Icons.Filled.Radio, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Aviso de error con reintento. El texto ya viene localizado. */
@Composable
fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
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
        OutlinedButton(onClick = onRetry) { Text("Reintentar") }
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

/** Logo de la emisora con placeholder de inicial si no hay favicon. */
@Composable
fun StationArtwork(
    station: StationDto,
    modifier: Modifier = Modifier,
) {
    if (station.favicon.isNullOrBlank()) {
        Surface(
            modifier = modifier.clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                station.name.firstOrNull()?.uppercase() ?: "?",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    } else {
        AsyncImage(
            model = station.favicon,
            contentDescription = station.name,
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}

/** Tarjeta de emisora estilo Pocket Casts (grid). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationCard(
    station: StationDto,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onPlay, modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            StationArtwork(station, Modifier.size(64.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                station.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                listOfNotNull(station.country, station.language).joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
            FavoriteButton(isFavorite = isFavorite, onToggle = onToggleFavorite)
        }
    }
}

/** Fila densa de emisora (lista). */
@Composable
fun StationListItem(
    station: StationDto,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable { onPlay() },
        headlineContent = { Text(station.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                listOfNotNull(station.country, station.language, station.tags.firstOrNull())
                    .joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { StationArtwork(station, Modifier.size(48.dp)) },
        trailingContent = { FavoriteButton(isFavorite = isFavorite, onToggle = onToggleFavorite) },
    )
}
