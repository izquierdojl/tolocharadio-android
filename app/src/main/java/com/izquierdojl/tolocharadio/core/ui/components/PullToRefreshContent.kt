package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.izquierdojl.tolocharadio.R

/**
 * Contenedor de recarga manual por arrastre para listas y cuadrículas.
 *
 * Mantiene el contenido visible mientras actualiza y expone una acción
 * accesible «Actualizar» para que la recarga no dependa del gesto con
 * lector de pantalla (FR-081).
 */
@Composable
fun PullToRefreshContent(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val actionLabel = stringResource(R.string.pull_to_refresh_action)
    val updatingLabel = stringResource(R.string.pull_to_refresh_updating)
    val updatedLabel = stringResource(R.string.pull_to_refresh_updated)
    val state = rememberPullToRefreshState()
    var hasRefreshed by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) {
        if (refreshing) hasRefreshed = true
    }
    val status =
        when {
            refreshing -> updatingLabel
            hasRefreshed -> updatedLabel
            else -> null
        }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier =
            modifier.semantics {
                if (status != null) {
                    stateDescription = status
                    liveRegion = LiveRegionMode.Polite
                }
                if (!refreshing) {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(actionLabel) {
                                onRefresh()
                                true
                            },
                        )
                }
            },
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = refreshing,
                modifier = Modifier.testTag(TolochaTestTags.PULL_REFRESH_INDICATOR),
            )
        },
        content = content,
    )
}
