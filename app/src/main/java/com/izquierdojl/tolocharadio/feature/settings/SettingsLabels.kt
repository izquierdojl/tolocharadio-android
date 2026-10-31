package com.izquierdojl.tolocharadio.feature.settings

import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile

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

/** Etiqueta del perfil de buffer (spec 0042, FR-011). */
fun BufferProfile.label(): String =
    when (this) {
        BufferProfile.DATA_SAVER -> "Ahorro de datos"
        BufferProfile.BALANCED -> "Equilibrado"
        BufferProfile.MAX_STABILITY -> "Máxima estabilidad"
    }

/** Descripción del compromiso de cada perfil (spec 0042, FR-011). */
fun BufferProfile.description(): String =
    when (this) {
        BufferProfile.DATA_SAVER ->
            "Arranque más rápido y menor uso de memoria. Más sensible a los cortes en redes inestables."
        BufferProfile.BALANCED ->
            "Buen equilibrio entre arranque y tolerancia a cortes. Recomendado."
        BufferProfile.MAX_STABILITY ->
            "Máxima tolerancia a cortes. Puede tardar hasta 5 segundos más en arrancar y usa más memoria."
    }
