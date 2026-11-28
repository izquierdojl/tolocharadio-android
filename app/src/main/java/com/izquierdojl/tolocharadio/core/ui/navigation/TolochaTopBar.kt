package com.izquierdojl.tolocharadio.core.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.core.ui.components.ModeSwitchButton
import com.izquierdojl.tolocharadio.core.ui.components.TolochaLogo
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.theme.LocalTolochaColors

/** Umbral a partir del cual cabe el texto completo del logotipo. */
private val LOGO_TEXT_MIN_WIDTH = 160.dp

/**
 * Cabecera compartida (spec 0047, US3): hamburguesa a la izquierda,
 * logotipo adaptable al centro y solo modo + Cast a la derecha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TolochaTopBar(
    mode: ContentMode,
    podcastsEnabled: Boolean,
    onSelectMode: (ContentMode) -> Unit,
    onOpenMenu: () -> Unit,
    castAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        TopAppBar(
            modifier = modifier.testTag(TolochaTestTags.TOP_BAR),
            navigationIcon = {
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.testTag(TolochaTestTags.MENU_BUTTON),
                ) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.menu_open),
                    )
                }
            },
            title = {
                BoxWithConstraints {
                    TolochaLogo(showText = maxWidth >= LOGO_TEXT_MIN_WIDTH)
                }
            },
            actions = {
                if (podcastsEnabled) {
                    ModeSwitchButton(mode = mode, onSelect = onSelectMode)
                }
                castAction()
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
        )
        HorizontalDivider(color = LocalTolochaColors.current.chromeDivider)
    }
}
