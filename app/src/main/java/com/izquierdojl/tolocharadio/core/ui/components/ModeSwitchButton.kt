package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.izquierdojl.tolocharadio.core.ui.navigation.ContentMode

/**
 * Alternador compacto Radio↔Podcasts para la barra superior (spec 0046,
 * US1). Muestra el icono del modo destino y alterna con un toque para
 * no ocupar una fila fija bajo la cabecera.
 */
@Composable
fun ModeSwitchButton(
    mode: ContentMode,
    onSelect: (ContentMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = if (mode == ContentMode.RADIO) ContentMode.PODCAST else ContentMode.RADIO
    IconButton(
        onClick = { onSelect(target) },
        modifier = modifier,
    ) {
        Icon(
            if (target == ContentMode.PODCAST) Icons.Filled.Podcasts else Icons.Filled.Radio,
            contentDescription =
                if (target == ContentMode.PODCAST) "Cambiar a Podcasts" else "Cambiar a Radio",
        )
    }
}
