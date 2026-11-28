package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.core.ui.theme.LocalTolochaColors

/**
 * Contenedor común del reproductor persistente (spec 0061).
 *
 * Se ancla a la barra inferior con esquinas superiores redondeadas y un borde
 * superior de 1 dp; sin `tonalElevation`, para no teñir el panel de ocre.
 */
@Composable
fun NowPlayingContainer(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalTolochaColors.current
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = colors.playerContainer,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column {
            HorizontalDivider(thickness = 1.dp, color = colors.playerBorder)
            content()
        }
    }
}
