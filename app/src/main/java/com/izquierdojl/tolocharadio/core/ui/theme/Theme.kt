package com.izquierdojl.tolocharadio.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Escala pine (web @theme). Fuente: apps/web/src/index.css.
val Pine50 = Color(0xFFF0F5F1)
val Pine100 = Color(0xFFDEEAE2)
val Pine200 = Color(0xFFBCD2C4)
val Pine300 = Color(0xFF8CB29A)
val Pine400 = Color(0xFF5F8F73)
val Pine500 = Color(0xFF3C6A4D)
val Pine600 = Color(0xFF2C4F38)
val Pine700 = Color(0xFF203A28)
val Pine800 = Color(0xFF17291C)
val Pine850 = Color(0xFF122016)
val Pine900 = Color(0xFF0F1A12)
val Pine950 = Color(0xFF08100B)

// Escala ochre (web @theme).
val Ochre100 = Color(0xFFF7ECDB)
val Ochre200 = Color(0xFFEFDBBB)
val Ochre300 = Color(0xFFE2C091)
val Ochre400 = Color(0xFFD3A568)
val Ochre500 = Color(0xFFC0883E)
val Ochre600 = Color(0xFFA67430)
val Ochre700 = Color(0xFF8A5F26)
val Ochre800 = Color(0xFF6B4A1D)
val Ochre900 = Color(0xFF4A3314)
val Ochre950 = Color(0xFF2B1D0C)

// Acentos moss (decorativos, nunca texto).
val Moss300 = Color(0xFFCFDAA2)
val Moss400 = Color(0xFFB4C47E)
val Moss500 = Color(0xFF9AAE63)

// Aliases legacy (compatibilidad transitoria, SC-002 los elimina fuera de aquí).
val TolochaForest = Pine600
val TolochaSage = Color(0xFF95D5B2)
val TolochaOchre = Ochre400
val TolochaBark = Ochre600

// Roles web -> M3 (contracts/design-tokens.md).
val MountainDark = Color(0x73203A28)
val MountainLight = Color(0xFFB4C9B9)

// Fondos claros web.
val LightSurface = Color(0xFFEEF3EF)
val LightSurfaceSoft = Color(0xFFF5F9F6)
val LightForeground = Color(0xFF123424)
val LightMuted = Color(0xFF4C6B58)
val LightSoft = Color(0xFF263F30)
val LightFaint = Pine600
val LightLine = Color(0xFFD9E4DC)
val LightLineStrong = Color(0xFFC4D4C9)

// Emblema resuelto por tema (SVG usa var(--*) en web).
val EmblemBadgeADark = Pine950
val EmblemBadgeBDark = Pine700
val EmblemSunDark = Ochre300
val EmblemLineDark = Pine200
val EmblemBadgeALight = Color(0xFFE3ECE5)
val EmblemBadgeBLight = Color(0xFFC8DBCE)
val EmblemSunLight = Ochre600
val EmblemLineLight = Pine500

// Botón play tarjeta web: reposo black/50+pine-100, activo ochre-500+pine-950.
val PlayRestBackground = Color(0x80000000)
val PlayRestContent = Pine100
val PlayActiveBackground = Ochre500
val PlayActiveContent = Pine950

private val DarkTolochaScheme =
    darkColorScheme(
        primary = Ochre400,
        onPrimary = Pine950,
        primaryContainer = Pine700,
        onPrimaryContainer = Pine100,
        secondary = Ochre400,
        onSecondary = Pine950,
        secondaryContainer = Pine800,
        onSecondaryContainer = Pine200,
        tertiary = Ochre300,
        onTertiary = Pine950,
        background = Pine950,
        onBackground = Pine100,
        surface = Pine950,
        onSurface = Pine100,
        surfaceVariant = Pine800,
        onSurfaceVariant = Pine400,
        surfaceContainerLowest = Pine950,
        surfaceContainerLow = Pine900,
        surfaceContainer = Pine900,
        surfaceContainerHigh = Pine850,
        surfaceContainerHighest = Pine800,
        outlineVariant = Pine800,
        outline = Pine700,
        error = Color(0xFFF2B8B5),
        onError = Pine950,
    )

private val LightTolochaScheme =
    lightColorScheme(
        primary = Ochre700,
        onPrimary = Color.White,
        primaryContainer = Ochre100,
        onPrimaryContainer = Ochre900,
        secondary = Ochre700,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE3ECE5),
        onSecondaryContainer = Pine700,
        tertiary = Ochre700,
        onTertiary = Color.White,
        background = LightSurface,
        onBackground = LightForeground,
        surface = LightSurface,
        onSurface = LightForeground,
        surfaceVariant = LightLine,
        onSurfaceVariant = LightMuted,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = LightSurfaceSoft,
        surfaceContainer = Color.White,
        surfaceContainerHigh = LightSurfaceSoft,
        surfaceContainerHighest = LightLine,
        outlineVariant = LightLine,
        outline = LightLineStrong,
        error = Color(0xFFBA1A1A),
        onError = Color.White,
    )

/** Formas web: tarjetas 16dp, botones/campos 12dp, chips/play/avatar full. */
val TolochaShapes =
    Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    )

/** Jerarquía web con fuente del sistema (clarificación B): título semibold, meta pequeña, chip UPPER. */
val TolochaTypography =
    Typography(
        titleLarge =
            TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.01).sp,
            ),
        titleMedium =
            TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        titleSmall =
            TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodySmall =
            TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        // Chips web: 10px uppercase, tracking amplio.
        labelSmall =
            TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.08.sp * 10,
            ),
    )

/**
 * Tema TolochaRadio. Oscuro por defecto (constitución); el selector
 * Sistema/Claro/Oscuro (ThemeMode) gobierna [darkTheme].
 */
@Composable
fun TolochaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkTolochaScheme else LightTolochaScheme,
        typography = TolochaTypography,
        shapes = TolochaShapes,
        content = content,
    )
}
