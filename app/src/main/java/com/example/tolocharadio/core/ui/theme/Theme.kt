package com.example.tolocharadio.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Verde-bosque de la marca Tolocha. */
val TolochaForest = Color(0xFF1B4332)

/** Verde claro para superficies sobre oscuro. */
val TolochaSage = Color(0xFF95D5B2)

/** Ocre-montaña de la marca Tolocha. */
val TolochaOchre = Color(0xFFDDA15C)

/** Ocre profundo para acentos en claro. */
val TolochaBark = Color(0xFF9C6644)

private val DarkTolochaScheme =
    darkColorScheme(
        primary = TolochaSage,
        onPrimary = Color(0xFF0B1F16),
        secondary = TolochaOchre,
        onSecondary = Color(0xFF2A1A08),
        tertiary = TolochaOchre,
        background = Color(0xFF101915),
        surface = Color(0xFF16211B),
    )

private val LightTolochaScheme =
    lightColorScheme(
        primary = TolochaForest,
        onPrimary = Color.White,
        secondary = TolochaBark,
        onSecondary = Color.White,
        tertiary = TolochaBark,
        background = Color(0xFFF7F5F0),
        surface = Color.White,
    )

/**
 * Tema TolochaRadio. Oscuro por defecto (constitución); el perfil
 * (`User.theme`) gobierna claro/oscuro.
 */
@Composable
fun TolochaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkTolochaScheme else LightTolochaScheme,
        typography = Typography(),
        content = content,
    )
}
