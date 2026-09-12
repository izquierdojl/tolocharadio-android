package com.izquierdojl.tolocharadio.domain.playback

/**
 * Fuente de reproducción de una emisora (spec 0021).
 *
 * Invariante: toda reproducción se sirve por el **proxy autenticado** del
 * servicio; [stationId] se resuelve con `streamUrl(baseUrl, stationId)` en la
 * capa de presentación, que es quien conoce la `baseUrl`. El token nunca viaja
 * en la URL.
 *
 * [hls] indica que la emisora es un recurso HLS (`.m3u8`): se reproduce con el
 * motor HLS sobre el manifiesto que el servicio reescribe, y sus subrecursos
 * (variantes/segmentos) también se piden al proxy con Bearer.
 */
data class PlaybackSource(
    val stationId: String,
    val hls: Boolean,
)
