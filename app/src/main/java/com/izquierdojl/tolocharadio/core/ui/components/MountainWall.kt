package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.core.ui.theme.MountainDark
import com.izquierdojl.tolocharadio.core.ui.theme.MountainLight

/**
 * Franja decorativa de silueta de montañas (paridad con `MountainWall`
 * de `AppShell.tsx` web), tintada con el color de montaña del tema.
 */
@Composable
fun MountainWall(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.mountain_wall),
        contentDescription = null,
        modifier = modifier.fillMaxWidth().height(48.dp),
        contentScale = ContentScale.FillBounds,
        colorFilter =
            ColorFilter.tint(
                if (isSystemInDarkTheme()) MountainDark else MountainLight,
            ),
    )
}

/** Overload que respeta el modo resuelto (no solo el sistema). */
@Composable
fun MountainWall(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.mountain_wall),
        contentDescription = null,
        modifier = modifier.fillMaxWidth().height(48.dp),
        contentScale = ContentScale.FillBounds,
        colorFilter = ColorFilter.tint(if (darkTheme) MountainDark else MountainLight),
    )
}
