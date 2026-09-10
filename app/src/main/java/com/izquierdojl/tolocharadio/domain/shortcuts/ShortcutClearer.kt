package com.izquierdojl.tolocharadio.domain.shortcuts

/**
 * Contrato para eliminar los accesos directos del icono (FR-010).
 *
 * Permite que capas puras (logout) y de datos (cambio de servidor) limpien
 * el menú sin acoplarse a la infraestructura Android que los publica.
 */
interface ShortcutClearer {
    /** Elimina de inmediato todos los accesos directos publicados. */
    suspend fun clear()
}
