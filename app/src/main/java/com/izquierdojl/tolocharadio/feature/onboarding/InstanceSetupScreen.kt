package com.izquierdojl.tolocharadio.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Pantalla de bienvenida: sin servidores configurados, indica que hay
 * que configurar uno para acceder y permite añadirlo (URL + alias).
 * No pide usuario ni contraseña (FR-002).
 */
@Composable
fun InstanceSetupScreen(
    onConnected: () -> Unit,
    viewModel: InstanceSetupViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val url =
        when (val s = ui) {
            is SetupUiState.Idle -> s.url
            is SetupUiState.Error -> s.url
            SetupUiState.Connecting -> ""
        }
    val alias =
        when (val s = ui) {
            is SetupUiState.Idle -> s.alias
            is SetupUiState.Error -> s.alias
            SetupUiState.Connecting -> ""
        }
    WelcomeContent(
        url = url,
        alias = alias,
        state = ui,
        onUrlChange = viewModel::onUrlChange,
        onAliasChange = viewModel::onAliasChange,
        onConnect = { viewModel.connect(url, alias, onConnected) },
    )
}

/** Contenido sin estado de la bienvenida (testable en androidTest). */
@Composable
internal fun WelcomeContent(
    url: String,
    alias: String,
    state: SetupUiState,
    onUrlChange: (String) -> Unit,
    onAliasChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("TolochaRadio", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Configura un servidor para acceder a tu contenido.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("URL del servidor") },
            placeholder = { Text("https://radio.mi-dominio.com") },
            singleLine = true,
            isError = state is SetupUiState.Error,
            supportingText = {
                if (state is SetupUiState.Error) Text(state.message)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state != SetupUiState.Connecting,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = alias,
            onValueChange = onAliasChange,
            label = { Text("Alias (opcional)") },
            placeholder = { Text("Mi servidor") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = state != SetupUiState.Connecting,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onConnect,
            enabled = url.isNotBlank() && state != SetupUiState.Connecting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state == SetupUiState.Connecting) {
                CircularProgressIndicator(Modifier.height(20.dp))
            } else {
                Text("Añadir servidor")
            }
        }
    }
}
