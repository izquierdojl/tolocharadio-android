package com.izquierdojl.tolocharadio.feature.profile

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode

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
        Text("Tema", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = ui.themeMode == mode,
                    onClick = { viewModel.onThemeModeChange(mode) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size,
                        ),
                    label = {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "Sistema"
                                ThemeMode.DARK -> "Oscuro"
                                ThemeMode.LIGHT -> "Claro"
                            },
                        )
                    },
                )
            }
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

