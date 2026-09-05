package com.example.tolocharadio.feature.profile

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Perfil: nombre, tema, instancia y salir (US-6). */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onLoggedOut: () -> Unit = {},
) {
    val ui by viewModel.ui.collectAsState()
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (ui.loading) {
            CircularProgressIndicator()
            return@Column
        }
        ui.user?.let { Text(it.email, style = MaterialTheme.typography.bodyLarge) }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::saveName, modifier = Modifier.fillMaxWidth()) {
            Text("Guardar")
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tema oscuro", modifier = Modifier.weight(1f))
            Switch(checked = ui.darkTheme, onCheckedChange = viewModel::onThemeChange)
        }
        ui.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { viewModel.logout(onLoggedOut) }, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
    }
}
