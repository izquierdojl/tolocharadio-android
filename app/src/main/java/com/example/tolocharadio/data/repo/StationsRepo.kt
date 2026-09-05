package com.example.tolocharadio.data.repo

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.DomainError
import com.example.tolocharadio.core.network.safeCall
import com.example.tolocharadio.data.local.CachedStation
import com.example.tolocharadio.data.local.TolochaDb
import com.example.tolocharadio.data.remote.api.StationsApi
import com.example.tolocharadio.data.remote.dto.StationDto
import com.example.tolocharadio.data.remote.dto.StationPageDto
import javax.inject.Inject
import javax.inject.Singleton

/** Filtros de `GET /stations` (paridad con `/explorar` web). */
data class StationQuery(
    val name: String? = null,
    val country: String? = null,
    val language: String? = null,
    val tag: String? = null,
    val limit: Int = 24,
    val offset: Int = 0,
    val unique: Boolean = false,
)

/** Catálogo con caché de lectura para 503/offline. */
@Singleton
class StationsRepo
    @Inject
    constructor(
        private val api: StationsApi,
        private val db: TolochaDb,
    ) {
        /** Búsqueda paginada; `limit` se clamp a 1–100 (FR-006). */
        suspend fun search(query: StationQuery): ApiResult<StationPageDto> {
            val q = query.copy(limit = query.limit.coerceIn(1, 100))
            return when (
                val r =
                    safeCall {
                        api.search(q.name, q.country, q.language, q.tag, q.limit, q.offset, q.unique)
                    }
            ) {
                is ApiResult.Ok -> {
                    cache(r.value.items)
                    r
                }
                is ApiResult.Err -> {
                    if (r.error is DomainError.Unavailable) fromCache(q) ?: r else r
                }
            }
        }

        /** Detalle por UUID. */
        suspend fun detail(id: String): ApiResult<StationDto> = safeCall { api.detail(id) }

        /** Listas para los filtros (países, idiomas, tags). */
        suspend fun countries(): ApiResult<List<String>> = strings { api.countries() }

        /** @see countries */
        suspend fun languages(): ApiResult<List<String>> = strings { api.languages() }

        /** @see countries */
        suspend fun tags(): ApiResult<List<String>> = strings { api.tags() }

        private suspend fun strings(
            call: suspend () -> retrofit2.Response<com.example.tolocharadio.data.remote.dto.StringListDto>,
        ): ApiResult<List<String>> =
            when (val r = safeCall(call)) {
                is ApiResult.Ok -> ApiResult.Ok(r.value.items)
                is ApiResult.Err -> r
            }

        private suspend fun cache(items: List<StationDto>) {
            val now = System.currentTimeMillis()
            db.stationsCache().upsertAll(
                items.map {
                    CachedStation(
                        id = it.id,
                        name = it.name,
                        favicon = it.favicon,
                        country = it.country,
                        language = it.language,
                        tagsCsv = it.tags.joinToString(","),
                        cachedAt = now,
                    )
                },
            )
        }

        private suspend fun fromCache(query: StationQuery): ApiResult<StationPageDto>? {
            val cached = db.stationsCache().search(query.name.orEmpty(), query.limit)
            if (cached.isEmpty()) return null
            return ApiResult.Ok(
                StationPageDto(
                    items =
                        cached.map {
                            StationDto(
                                id = it.id,
                                name = it.name,
                                favicon = it.favicon,
                                country = it.country,
                                language = it.language,
                                tags = it.tagsCsv.split(",").filter(String::isNotBlank),
                            )
                        },
                    pagination =
                        com.example.tolocharadio.data.remote.dto.PaginationDto(
                            offset = query.offset,
                            limit = query.limit,
                            hasMore = false,
                        ),
                ),
            )
        }
    }
