package com.izquierdojl.tolocharadio.core.ui

/**
 * Modo de presentación de las listas de emisoras (spec 008).
 * Preferencia global de la app; el default es [LIST].
 */
enum class ViewMode {
    /** Lista compacta vertical (`StationListItem` / list items de sección). */
    LIST,

    /** Cuadrícula de tarjetas (`StationCard`, 2 columnas). */
    GRID,
}
