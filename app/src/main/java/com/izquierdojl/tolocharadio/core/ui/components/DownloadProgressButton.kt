package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.R
import kotlin.math.roundToInt

/**
 * Botón de descarga con progreso animado (spec 0047, US6).
 * En las filas ocupa el hueco del icono; en la ficha se usa `downloadLabel`.
 */
@Composable
fun DownloadProgressButton(
    state: DownloadUiState,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        DownloadUiState.NotDownloaded -> DownloadIconButton(onDownload, modifier)
        is DownloadUiState.Failure -> DownloadIconButton(onDownload, modifier, retry = true)
        DownloadUiState.Downloaded ->
            Icon(
                Icons.Filled.OfflinePin,
                contentDescription = stringResource(R.string.downloaded),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = modifier.size(24.dp),
            )
        is DownloadUiState.Downloading -> {
            val fraction = state.fraction
            val description =
                if (fraction == null) {
                    stringResource(R.string.downloading)
                } else {
                    stringResource(R.string.downloading_percent, (fraction * 100).roundToInt())
                }
            Box(
                modifier =
                    modifier
                        .size(48.dp)
                        .testTag(TolochaTestTags.DOWNLOAD_BUTTON)
                        .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                if (fraction == null) {
                    CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
                } else {
                    CircularProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp,
                    )
                    Text(
                        "${(fraction * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadIconButton(
    onDownload: () -> Unit,
    modifier: Modifier,
    retry: Boolean = false,
) {
    IconButton(onClick = onDownload, modifier = modifier.testTag(TolochaTestTags.DOWNLOAD_BUTTON)) {
        Icon(
            Icons.Filled.Download,
            contentDescription =
                stringResource(if (retry) R.string.download_retry else R.string.download_action),
        )
    }
}
