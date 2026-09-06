package com.example.tolocharadio.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Emisora del catálogo. `url` solo se usa vía proxy autenticado,
 * nunca directamente en el player (FR-007).
 */
@Serializable
data class StationDto(
    val id: String,
    val name: String,
    val url: String = "",
    val homepage: String? = null,
    val favicon: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val language: String? = null,
    val tags: List<String> = emptyList(),
    val codec: String? = null,
    val bitrate: Int? = null,
    val isSsl: Boolean? = null,
    val lastCheckOk: Boolean? = null,
    val votes: Int? = null,
    val clickCount: Int? = null,
    val isCustom: Boolean = false,
)

/** Página de `GET /stations` con `pagination{offset,limit,hasMore}`. */
@Serializable
data class StationPageDto(
    val items: List<StationDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(0, 24, false),
)

@Serializable
data class PaginationDto(
    val offset: Int = 0,
    val limit: Int = 24,
    val hasMore: Boolean = false,
)

/** Favorito `{station,addedAt}`. */
@Serializable
data class FavoriteDto(
    val station: StationDto,
    val addedAt: Long = 0,
)

/** Entrada de historial `{station,playedAt}`. */
@Serializable
data class HistoryEntryDto(
    val station: StationDto,
    val playedAt: Long = 0,
)

/** Envoltorios `{items}` y `{favorite}` del API. */
@Serializable
data class StationListDto(val items: List<StationDto> = emptyList())

@Serializable
data class FavoriteListDto(val items: List<FavoriteDto> = emptyList())

@Serializable
data class FavoriteResultDto(val favorite: FavoriteDto)

@Serializable
data class StringListDto(val items: List<String> = emptyList())

/** Envoltorio `{items}` del API para historial. */
@Serializable
data class HistoryListDto(val items: List<HistoryEntryDto> = emptyList())

/** Cuerpo de `POST /custom-stations` (valores ya recortados por el ViewModel). */
@Serializable
data class CreateCustomStationBody(
    val name: String,
    val url: String,
)

/** Respuesta de `POST /custom-stations` (`{station}`). */
@Serializable
data class CustomStationResultDto(val station: StationDto)
