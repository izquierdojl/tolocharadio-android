package com.izquierdojl.tolocharadio.domain.shortcuts

/**
 * Emisora lista para publicarse como acceso directo del icono (spec 0018).
 *
 * @property stationId identidad estable de la emisora (se usa para reproducir).
 * @property name etiqueta visible; nunca vacía (el caso de uso aplica fallback).
 * @property faviconUrl imagen opcional; `null` implica icono de la app.
 * @property rank posición según recencia (0 = más reciente).
 */
data class ShortcutStation(
    val stationId: String,
    val name: String,
    val faviconUrl: String?,
    val rank: Int,
)
