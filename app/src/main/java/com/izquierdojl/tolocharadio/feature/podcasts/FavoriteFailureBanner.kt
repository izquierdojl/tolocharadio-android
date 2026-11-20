package com.izquierdojl.tolocharadio.feature.podcasts

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.izquierdojl.tolocharadio.core.ui.components.ErrorBanner

/**
 * Aviso accionable de un fallo de favorito (spec 0048, FR-027):
 * conserva el estado anterior y ofrece reintentar la misma operación.
 */
@Composable
internal fun FavoriteFailureBanner(
    message: String?,
    onRetry: () -> Unit,
) {
    message?.let {
        ErrorBanner(
            message = it,
            onRetry = onRetry,
            retryLabel = "Reintentar",
        )
    }
}

/** Snackbar con reintento para fallos de favorito lanzados desde filas y tarjetas. */
@Composable
internal fun FavoriteFailureEffect(
    failure: EpisodeFavoriteFailure?,
    snackbar: SnackbarHostState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(failure) {
        val current = failure ?: return@LaunchedEffect
        when (
            snackbar.showSnackbar(
                message = current.message,
                actionLabel = "Reintentar",
            )
        ) {
            SnackbarResult.ActionPerformed -> onRetry()
            SnackbarResult.Dismissed -> onDismiss()
        }
    }
}
