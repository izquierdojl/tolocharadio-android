package com.izquierdojl.tolocharadio.domain.shortcuts

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto

/**
 * Resultado de resolver el lanzamiento de un acceso directo (contrato
 * `contracts/shortcut-intent.md`). Cubre FR-005, FR-006, FR-007 y FR-011.
 */
sealed interface ShortcutLaunchResolution {
    /** Reproducir la emisora y abrir el reproductor a pantalla completa. */
    data class Play(val station: StationDto) : ShortcutLaunchResolution

    /** Sin sesión válida: navegar a Login con aviso (reason puede ser "expired"). */
    data class GoLogin(val reason: String?) : ShortcutLaunchResolution

    /** Sesión restaurándose: no consumir el pendiente todavía. */
    data object Wait : ShortcutLaunchResolution

    /** Emisora inexistente o no reproducible: mensaje accionable, sin abrir el player. */
    data class Unavailable(val message: String) : ShortcutLaunchResolution
}
