package com.izquierdojl.tolocharadio.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens semánticos de superficie y contraste (spec 0061).
 *
 * La paleta M3 tiene los roles `surface`/`surfaceContainer*` muy próximos; estos
 * tokens permiten separar tarjeta, panel y chrome sin reescribir los roles
 * globales (que afectan a sheets, menús y campos).
 */
@Immutable
data class TolochaColors(
    val isDark: Boolean,
    val cardContainer: Color,
    val cardBorder: Color,
    val cardPlaceholder: Color,
    val playerContainer: Color,
    val playerBorder: Color,
    val chromeDivider: Color,
    val navIndicator: Color,
    val navSelectedContent: Color,
    val navUnselectedContent: Color,
)

/** Tokens del tema oscuro (por defecto). */
val DarkTolochaColors =
    TolochaColors(
        isDark = true,
        cardContainer = Pine650,
        cardBorder = Pine500,
        cardPlaceholder = Pine800,
        playerContainer = Pine725,
        playerBorder = Pine500,
        chromeDivider = Pine700,
        navIndicator = Ochre400,
        navSelectedContent = Pine950,
        navUnselectedContent = Pine300,
    )

/** Tokens del tema claro, con la misma jerarquía que el oscuro. */
val LightTolochaColors =
    TolochaColors(
        isDark = false,
        cardContainer = Color.White,
        cardBorder = LightLineStrong,
        cardPlaceholder = LightLine,
        playerContainer = LightSurfaceSoft,
        playerBorder = LightLineStrong,
        chromeDivider = LightLine,
        navIndicator = Ochre700,
        navSelectedContent = Color.White,
        navUnselectedContent = LightMuted,
    )

/** Paleta activa; [TolochaTheme] la provee y los componentes la leen. */
val LocalTolochaColors = staticCompositionLocalOf { DarkTolochaColors }
