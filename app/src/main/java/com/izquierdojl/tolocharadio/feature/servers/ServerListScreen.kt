package com.izquierdojl.tolocharadio.feature.servers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.domain.servers.SavedServer

/**
 * Sección Servidores: activo ≠ por defecto; el icono Editar abre el
 * formulario unificado y el toque de la tarjeta cambia el activo (FR-002).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    viewModel: ServerListViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onAdd: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onNoServers: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val servers by viewModel.servers.collectAsState()

    Scaffold(
        topBar = {
            Column {
                Text(
                    "Servidores",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir servidor")
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
        ) {
            if (servers.isEmpty()) {
                Text(
                    "No hay servidores guardados. Añade uno para acceder al contenido.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 32.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(servers, key = { it.id }) { server ->
                        ServerCard(
                            server = server,
                            onSwitch = { viewModel.switchServer(server.id) },
                            onEdit = { onEdit(server.id) },
                            onDelete = { viewModel.deleteServer(server.id, onNoServers) },
                        )
                    }
                }
            }

            uiState.error?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
                viewModel.clearError()
            }

            uiState.successMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, color = MaterialTheme.colorScheme.primary)
                viewModel.clearSuccess()
            }
        }
    }
}

@Composable
internal fun ServerCard(
    server: SavedServer,
    onSwitch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onSwitch() },
        colors =
            if (server.isActive) {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            } else {
                CardDefaults.cardColors()
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(server.alias, style = MaterialTheme.typography.titleMedium)
                Text(server.url, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (server.isActive) {
                        Text("Activo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (server.isDefault) {
                        Text("Por defecto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            Row {
                if (server.isActive) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Servidor activo",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (server.isDefault) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Servidor por defecto",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }
}
