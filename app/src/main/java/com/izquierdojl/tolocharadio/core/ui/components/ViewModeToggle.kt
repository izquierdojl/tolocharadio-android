package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.izquierdojl.tolocharadio.core.ui.ViewMode

/**
 * Botón del alternador lista/tarjetas (spec 008, FR-002).
 * Muestra siempre el icono del modo destino: cuadrícula cuando se está
 * en lista, lista cuando se está en tarjetas.
 */
@Composable
fun ViewModeToggle(
    mode: ViewMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toGrid = mode == ViewMode.LIST
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            if (toGrid) Icons.Filled.GridView else Icons.Filled.ViewList,
            contentDescription =
                if (toGrid) "Cambiar a vista de tarjetas" else "Cambiar a vista de lista",
        )
    }
}
