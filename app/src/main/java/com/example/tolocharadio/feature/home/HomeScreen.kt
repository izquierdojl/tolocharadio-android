package com.example.tolocharadio.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tolocharadio.core.session.AuthState
import com.example.tolocharadio.core.session.SessionManager

/** Home pública (paridad con `/` web). */
@Composable
fun HomeScreen(
    onExplore: () -> Unit,
    sessionManager: SessionManager? = null,
) {
    val auth = sessionManager?.authState?.collectAsState()?.value
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Radio libre, datos tuyos.", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Explora el catálogo, guarda favoritas y escucha sin interrupciones.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        if (auth is AuthState.Authenticated) {
            Text("Hola, ${auth.user.name ?: auth.user.email}.")
            Spacer(Modifier.height(8.dp))
        }
        Button(onClick = onExplore) { Text("Explorar emisoras") }
    }
}
