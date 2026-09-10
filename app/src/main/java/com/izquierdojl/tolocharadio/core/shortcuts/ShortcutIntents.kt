package com.izquierdojl.tolocharadio.core.shortcuts

/**
 * Contrato del intent que arranca la app desde un acceso directo del icono.
 * Ver `contracts/shortcut-intent.md`.
 */
object ShortcutIntents {
    const val ACTION_OPEN_STATION = "com.izquierdojl.tolocharadio.OPEN_STATION"

    const val EXTRA_STATION_ID = "station_id"

    const val EXTRA_STATION_NAME = "station_name"

    /**
     * Parsea los datos del intent. Devuelve `null` si la acción no es de
     * shortcut o si falta un `station_id` válido (contrato: se ignora en
     * silencio, sin crash).
     */
    fun parse(
        action: String?,
        stationId: String?,
        stationName: String?,
    ): PendingShortcut? {
        if (action != ACTION_OPEN_STATION) return null
        val id = stationId?.trim().orEmpty()
        if (id.isEmpty()) return null
        return PendingShortcut(
            stationId = id,
            stationName = stationName?.trim()?.ifBlank { null },
        )
    }
}
