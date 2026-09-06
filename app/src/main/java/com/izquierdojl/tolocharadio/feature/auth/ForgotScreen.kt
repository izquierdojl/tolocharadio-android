package com.izquierdojl.tolocharadio.feature.auth

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import com.izquierdojl.tolocharadio.domain.ValidateAuthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Recuperación con mensaje neutro anti-enumeración (US-6). */
@HiltViewModel
class ForgotViewModel
    @Inject
    constructor(
        private val auth: AuthRepo,
        private val validate: ValidateAuthUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(ForgotUi())
        val ui: StateFlow<ForgotUi> = _ui.asStateFlow()

        fun onEmailChange(v: String) {
            _ui.value = _ui.value.copy(email = v, error = null, sent = false)
        }

        /** Pide el reset; el mensaje es idéntico exista o no la cuenta. */
        fun send() {
            val emailErr = validate.email(_ui.value.email)
            if (emailErr != null) {
                _ui.value = _ui.value.copy(error = emailErr)
                return
            }
            _ui.value = _ui.value.copy(loading = true)
            viewModelScope.launch {
                when (auth.forgot(_ui.value.email)) {
                    is ApiResult.Ok -> _ui.value = _ui.value.copy(loading = false, sent = true)
                    is ApiResult.Err -> _ui.value = _ui.value.copy(loading = false, sent = true)
                }
            }
        }

        /** Completa el reset con el token de un solo uso. */
        fun reset(
            token: String,
            password: String,
            onDone: () -> Unit,
        ) {
            val passErr = validate.password(password)
            if (passErr != null) {
                _ui.value = _ui.value.copy(error = passErr)
                return
            }
            _ui.value = _ui.value.copy(loading = true)
            viewModelScope.launch {
                when (val r = auth.reset(token, password)) {
                    is ApiResult.Ok -> onDone()
                    is ApiResult.Err -> _ui.value = _ui.value.copy(loading = false, error = r.error.userMessage())
                }
            }
        }
    }

data class ForgotUi(
    val email: String = "",
    val error: String? = null,
    val loading: Boolean = false,
    val sent: Boolean = false,
)

/** Pantalla de "olvidé mi contraseña" + reset con token. */
@Composable
fun ForgotScreen(
    onDone: () -> Unit,
    viewModel: ForgotViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var token by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Recuperar contraseña", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (ui.sent) {
            Text("Si existe una cuenta con ese email, tienes instrucciones para continuar.")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Token recibido") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Nueva contraseña") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.reset(token, password, onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text("Restablecer")
            }
        } else {
            OutlinedTextField(
                value = ui.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                isError = ui.error != null,
                supportingText = { ui.error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = viewModel::send, modifier = Modifier.fillMaxWidth()) {
                if (ui.loading) {
                    CircularProgressIndicator(Modifier.height(20.dp))
                } else {
                    Text("Enviar")
                }
            }
        }
    }
}

