package com.izquierdojl.tolocharadio.core.shortcuts

import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutStation

/**
 * Publica y elimina los accesos directos del icono. Se abstrae para poder
 * testear el coordinador con un fake y aislar la API Android
 * (research.md R1/R8).
 */
interface ShortcutPublisher {
    /** Reemplaza el conjunto de accesos; una lista vacía equivale a limpiar. */
    suspend fun publish(stations: List<ShortcutStation>)

    /** Elimina todos los accesos dinámicos publicados. */
    suspend fun clear()

    /** Huecos disponibles según el lanzador (0 = sin soporte). */
    fun maxSlots(): Int
}
