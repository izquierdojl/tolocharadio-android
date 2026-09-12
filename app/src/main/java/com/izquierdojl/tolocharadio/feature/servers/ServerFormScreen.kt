package com.izquierdojl.tolocharadio.feature.servers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Pantalla unificada de servidor: alta y edición con URL, alias, email
 * y contraseña (FR-002). Se usa en la bienvenida, en el arranque que
 * requiere credenciales y en la sección Servidores.
 */
@Composable
fun ServerFormScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    serverId: String? = null,
    showCancel: Boolean = true,
    viewModel: ServerFormViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(serverId) {
        if (serverId != null) viewModel.loadForEdit(serverId)
    }
    LaunchedEffect(ui.saved) {
        if (ui.saved) onDone()
    }

    ServerFormContent(
        ui = ui,
        showCancel = showCancel,
        onUrlChange = viewModel::onUrlChange,
        onAliasChange = viewModel::onAliasChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSave = viewModel::save,
        onCancel = onCancel,
    )
}

/** Contenido sin estado (testable en androidTest). */
@Composable
internal fun ServerFormContent(
    ui: ServerFormUi,
    showCancel: Boolean,
    onUrlChange: (String) -> Unit,
    onAliasChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val isEdit = ui.mode == ServerFormMode.EDIT
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            if (isEdit) "Editar servidor" else "Añadir servidor",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isEdit) {
                "Corrige el alias o las credenciales del servidor."
            } else {
                "Configura un servidor para acceder a tu contenido."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = ui.url,
            onValueChange = onUrlChange,
            label = { Text("URL del servidor") },
            placeholder = { Text("https://radio.mi-dominio.com") },
            singleLine = true,
            enabled = !isEdit && !ui.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.alias,
            onValueChange = onAliasChange,
            label = { Text("Alias") },
            placeholder = { Text("Mi servidor") },
            singleLine = true,
            enabled = !ui.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.email,
            onValueChange = onEmailChange,
            label = { Text("Email de la cuenta") },
            singleLine = true,
            enabled = !ui.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.password,
            onValueChange = onPasswordChange,
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !ui.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        ui.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onCancel,
                enabled = showCancel && !ui.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancelar")
            }
            Spacer(Modifier.height(0.dp))
            Button(
                onClick = onSave,
                enabled =
                    !ui.isSaving &&
                        ui.url.isNotBlank() &&
                        ui.alias.isNotBlank() &&
                        ui.email.isNotBlank() &&
                        ui.password.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                if (ui.isSaving) {
                    CircularProgressIndicator(Modifier.height(20.dp))
                } else {
                    Text(if (isEdit) "Guardar" else "Añadir servidor")
                }
            }
        }
    }
}
