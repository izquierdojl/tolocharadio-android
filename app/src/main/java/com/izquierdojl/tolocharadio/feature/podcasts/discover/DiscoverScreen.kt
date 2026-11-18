package com.izquierdojl.tolocharadio.feature.podcasts.discover

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.PodcastSearchRow
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.feature.podcasts.DiscoverViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Descubrir podcasts (US2): búsqueda en directorio, alta manual RSS e
 * importación/exportación OPML con el selector del sistema (FR-006–FR-010).
 */
@Composable
fun DiscoverScreen(
    onBrowseSubscriptions: () -> Unit,
    snackbar: SnackbarHostState,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var manualUrl by remember { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val xml = readText(context.contentResolver, uri)
                viewModel.importOpml(xml ?: "")
            }
        }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/xml")) { uri ->
            val text = pendingExport ?: return@rememberLauncherForActivityResult
            pendingExport = null
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val ok =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
                        }.isSuccess
                    }
                if (!ok) viewModel.showMessage("No se pudo guardar el OPML.")
            }
        }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        SectionHeader(title = "Descubrir", subtitle = "Busca programas o añade un RSS.")
        Column(Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = ui.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Buscar podcasts") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = viewModel::search, enabled = !ui.searching) {
                    Text("Buscar")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = manualUrl,
                onValueChange = { manualUrl = it },
                label = { Text("URL RSS manual") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedButton(
                    onClick = {
                        viewModel.resolveManual(manualUrl)
                        manualUrl = ""
                    },
                ) {
                    Text("Añadir RSS")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = { importLauncher.launch("*/*") }) {
                    Text("Importar OPML")
                }
                TextButton(onClick = {
                    scope.launch {
                        val text = viewModel.exportOpmlText() ?: return@launch
                        pendingExport = text
                        exportLauncher.launch("tolocharadio-suscripciones.opml")
                    }
                }) {
                    Text("Exportar OPML")
                }
                TextButton(onClick = onBrowseSubscriptions) {
                    Text("Suscripciones")
                }
            }
        }
        if (ui.searching) {
            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
        } else if (ui.results.isEmpty()) {
            EmptyState(title = "Sin resultados")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(ui.results, key = { it.feedUrl }) { result ->
                    PodcastSearchRow(
                        result = result,
                        subscribed = result.feedUrl in ui.subscribedFeedUrls,
                        onToggleSubscribe = { viewModel.toggleSubscribe(result) },
                    )
                }
            }
        }
    }
}

/** Lee un texto desde un `content://` URI en IO, o null si falla. */
private suspend fun readText(
    resolver: android.content.ContentResolver,
    uri: android.net.Uri,
): String? =
    withContext(Dispatchers.IO) {
        runCatching { resolver.openInputStream(uri)?.bufferedReader()?.readText() }.getOrNull()
    }
