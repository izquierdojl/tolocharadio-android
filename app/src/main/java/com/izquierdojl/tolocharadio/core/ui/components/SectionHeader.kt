package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Encabezado de sección reutilizable con título, subtítulo opcional y
 * acciones a la derecha (alternador de vista, orden, etc.).
 *
 * Garantiza consistencia visual entre todas las secciones de la app
 * (spec 012, FR-003/FR-004/FR-006).
 *
 * @param title Título de la sección (obligatorio).
 * @param subtitle Subtítulo descriptivo (opcional, no se muestra si es null).
 * @param modifier Modifier para personalización externa.
 * @param actions Acciones alineadas a la derecha (opcional).
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
