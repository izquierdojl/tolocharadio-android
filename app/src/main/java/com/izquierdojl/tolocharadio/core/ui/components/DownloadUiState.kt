package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.data.local.podcasts.DownloadRecord
import com.izquierdojl.tolocharadio.data.local.podcasts.DownloadStatus
import com.izquierdojl.tolocharadio.data.repo.DownloadProgress
import kotlin.math.roundToInt

/** Estados de presentación de una descarga (spec 0047, US6). */
sealed interface DownloadUiState {
    data object NotDownloaded : DownloadUiState

    data class Downloading(val fraction: Float?) : DownloadUiState

    data object Downloaded : DownloadUiState

    data class Failure(val message: String) : DownloadUiState
}

/** Deriva el estado visual a partir del registro de Room y del progreso. */
fun downloadUiState(
    record: DownloadRecord?,
    progress: DownloadProgress?,
): DownloadUiState =
    when {
        record?.status == DownloadStatus.COMPLETE && !record.localUri.isNullOrBlank() ->
            DownloadUiState.Downloaded
        record?.status == DownloadStatus.FAILED -> DownloadUiState.Failure("No se pudo descargar")
        record?.status == DownloadStatus.RUNNING || progress != null ->
            DownloadUiState.Downloading(progress?.fraction)
        else -> DownloadUiState.NotDownloaded
    }

/** Texto del botón de descarga para la ficha del episodio. */
@Composable
fun downloadLabel(state: DownloadUiState): String =
    when (state) {
        DownloadUiState.NotDownloaded -> stringResource(R.string.download_action)
        is DownloadUiState.Downloading -> {
            val fraction = state.fraction
            if (fraction == null) {
                stringResource(R.string.downloading)
            } else {
                stringResource(R.string.downloading_percent, (fraction * 100).roundToInt())
            }
        }
        DownloadUiState.Downloaded -> stringResource(R.string.downloaded)
        is DownloadUiState.Failure -> stringResource(R.string.download_retry)
    }
