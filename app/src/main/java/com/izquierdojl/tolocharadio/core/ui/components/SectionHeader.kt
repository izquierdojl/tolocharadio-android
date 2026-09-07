package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Encabezado de sección reutilizable con título y subtítulo opcional.
 *
 * Garantiza consistencia visual entre todas las secciones de la app
 * (spec 012, FR-003/FR-004/FR-006).
 *
 * @param title Título de la sección (obligatorio).
 * @param subtitle Subtítulo descriptivo (opcional, no se muestra si es null).
 * @param modifier Modifier para personalización externa.
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
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
}
