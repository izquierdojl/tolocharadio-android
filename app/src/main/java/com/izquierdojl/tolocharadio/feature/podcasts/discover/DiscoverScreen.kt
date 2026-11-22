package com.izquierdojl.tolocharadio.feature.podcasts.discover

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.DiscoverViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.TopChartsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Descubrir podcasts (US2): búsqueda en directorio, ranking «Populares»,
 * alta manual RSS e importación/exportación OPML (FR-006–FR-010).
 */
@Composable
fun DiscoverScreen(
    onBrowseSubscriptions: () -> Unit,
    onEditServer: () -> Unit,
    snackbar: SnackbarHostState,
    viewModel: DiscoverViewModel = hiltViewModel(),
    topChartsViewModel: TopChartsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val topChartsUi by topChartsViewModel.ui.collectAsState()
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

    DiscoverContent(
        ui = ui,
        topChartsUi = topChartsUi,
        manualUrl = manualUrl,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::search,
        onManualUrlChange = { manualUrl = it },
        onAddManual = {
            viewModel.resolveManual(manualUrl)
            manualUrl = ""
        },
        onImportOpml = { importLauncher.launch("*/*") },
        onExportOpml = {
            scope.launch {
                val text = viewModel.exportOpmlText() ?: return@launch
                pendingExport = text
                exportLauncher.launch("tolocharadio-suscripciones.opml")
            }
        },
        onBrowseSubscriptions = onBrowseSubscriptions,
        onToggleSubscribe = viewModel::toggleSubscribe,
        onCountryChange = topChartsViewModel::selectCountry,
        onGenreChange = topChartsViewModel::selectGenre,
        onRetryTopCharts = topChartsViewModel::retry,
        onEditServer = onEditServer,
    )
}

/** Lee un texto desde un `content://` URI en IO, o null si falla. */
private suspend fun readText(
    resolver: android.content.ContentResolver,
    uri: android.net.Uri,
): String? =
    withContext(Dispatchers.IO) {
        runCatching { resolver.openInputStream(uri)?.bufferedReader()?.readText() }.getOrNull()
    }
