package com.izquierdojl.tolocharadio.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Pantalla de creación de cuenta (US-3). */
@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = ui.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Nombre (opcional)") },
            singleLine = true,
            isError = ui.nameError != null,
            supportingText = { ui.nameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !ui.loading,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = ui.emailError != null,
            supportingText = { ui.emailError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !ui.loading,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Contraseña (8–72)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = ui.passwordError != null,
            supportingText = { ui.passwordError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !ui.loading,
        )
        ui.generalError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.register(onRegistered) },
            enabled = !ui.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (ui.loading) {
                CircularProgressIndicator(Modifier.height(20.dp))
            } else {
                Text("Crear cuenta")
            }
        }
    }
}
