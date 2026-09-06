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

/** Primera pantalla: pide la URL de la instancia self-hosted (US-1). */
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
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("TolochaRadio", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Introduce la dirección de tu instancia para empezar.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = url,
            onValueChange = viewModel::onUrlChange,
            label = { Text("URL de la instancia") },
            placeholder = { Text("https://radio.mi-dominio.com") },
            singleLine = true,
            isError = ui is SetupUiState.Error,
            supportingText = {
                if (ui is SetupUiState.Error) Text((ui as SetupUiState.Error).message)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = ui != SetupUiState.Connecting,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.connect(url, onConnected) },
            enabled = ui != SetupUiState.Connecting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (ui == SetupUiState.Connecting) {
                CircularProgressIndicator(Modifier.height(20.dp))
            } else {
                Text("Conectar")
            }
        }
    }
}
