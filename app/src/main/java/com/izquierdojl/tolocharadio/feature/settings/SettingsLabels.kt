package com.izquierdojl.tolocharadio.feature.settings

import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode

/** Etiqueta legible del tema activo para el diálogo "Acerca de" (FR-002). */
fun ThemeMode.label(): String =
    when (this) {
        ThemeMode.SYSTEM -> "Sistema"
        ThemeMode.DARK -> "Oscuro"
        ThemeMode.LIGHT -> "Claro"
    }

/** Etiqueta legible de la pantalla de arranque para el diálogo (FR-002). */
fun StartScreen.label(): String =
    when (this) {
        StartScreen.EXPLORE -> "Explorar"
        StartScreen.FAVORITES -> "Favoritos"
        StartScreen.HISTORY -> "Historial"
    }
