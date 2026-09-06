package com.izquierdojl.tolocharadio.core.ui.theme

/** Selector de tema (clarificación A 2026-09-05): local, default SYSTEM. */
enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
}

/** Resuelve el modo a booleano oscuro. Pura y testeable (T008). */
fun resolveDarkTheme(
    mode: ThemeMode,
    systemDark: Boolean,
): Boolean =
    when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
