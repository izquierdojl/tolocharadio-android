package com.izquierdojl.tolocharadio.data.remote.dto

import kotlinx.serialization.Serializable

/*
 * DTOs de `GET /stats/me/...` (contrato `contracts/stats-api.md`).
 * Los límites (`weekday` 0..6, `hour` 0..23, `limit` 1..50/1..200,
 * `from <= to`) los valida el servidor.
 */

/** Escucha agregada por emisora `{station,totalMs}` (top del periodo). */
@Serializable
data class StatsTopEntryDto(
    val station: StationDto,
    val totalMs: Long = 0,
)

/** Intervalo de la serie temporal `{bucket,totalMs}` (día, semana o mes). */
@Serializable
data class StatsTimelineEntryDto(
    val bucket: String,
    val totalMs: Long = 0,
)

/** Escucha por día de la semana y hora `{weekday,hour,totalMs}` (día 0..6 = lunes..domingo, hora 0..23). */
@Serializable
data class StatsHabitEntryDto(
    val weekday: Int = 0,
    val hour: Int = 0,
    val totalMs: Long = 0,
)

/** Tiempo de escucha por género `{genre,totalMs}`. */
@Serializable
data class StatsGenreEntryDto(
    val genre: String,
    val totalMs: Long = 0,
)

/** Tiempo de escucha por país `{country,countryCode,totalMs}` (`countryCode` anulable). */
@Serializable
data class StatsCountryEntryDto(
    val country: String,
    val countryCode: String? = null,
    val totalMs: Long = 0,
)

/** Escucha reciente `{station,startedAt,durationMs}` (`startedAt` en epoch millis). */
@Serializable
data class StatsRecentEntryDto(
    val station: StationDto,
    val startedAt: Long = 0,
    val durationMs: Long = 0,
)

/** Envoltorio `{items}` de `GET /stats/me/top`. */
@Serializable
data class StatsTopListDto(val items: List<StatsTopEntryDto> = emptyList())

/** Respuesta `{granularity,items}` de `GET /stats/me/timeline`. */
@Serializable
data class StatsTimelineDto(
    val granularity: String = "day",
    val items: List<StatsTimelineEntryDto> = emptyList(),
)

/** Envoltorio `{items}` de `GET /stats/me/habits`. */
@Serializable
data class StatsHabitListDto(val items: List<StatsHabitEntryDto> = emptyList())

/** Envoltorio `{items}` de `GET /stats/me/genres`. */
@Serializable
data class StatsGenreListDto(val items: List<StatsGenreEntryDto> = emptyList())

/** Envoltorio `{items}` de `GET /stats/me/countries`. */
@Serializable
data class StatsCountryListDto(val items: List<StatsCountryEntryDto> = emptyList())

/** Envoltorio `{items}` de `GET /stats/me/recent`. */
@Serializable
data class StatsRecentListDto(val items: List<StatsRecentEntryDto> = emptyList())
