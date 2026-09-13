package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.local.CachedFavorite
import com.izquierdojl.tolocharadio.data.local.TolochaDb
import com.izquierdojl.tolocharadio.data.local.toCached
import com.izquierdojl.tolocharadio.data.local.toFavorites
import com.izquierdojl.tolocharadio.data.remote.api.AddFavoriteBody
import com.izquierdojl.tolocharadio.data.remote.api.FavoritesApi
import com.izquierdojl.tolocharadio.data.remote.api.ReorderBody
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Lista de favoritas con bandera offline (FR-010). */
data class FavoritesResult(val items: List<FavoriteDto>, val offline: Boolean)

/**
 * Favoritos: lista completa del servidor (verdad), caché Room de
 * solo lectura y flujo compartido de ids para el marcado coherente
 * en Explorar, ficha y Favoritos (FR-003).
 */
@Singleton
class FavoritesRepo
    @Inject
    constructor(
        private val api: FavoritesApi,
        private val db: TolochaDb,
    ) {
        private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())

        /** Ids favoritas: fuente única observada por todos los ViewModels. */
        val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

        private val _favorites = MutableStateFlow<List<FavoriteDto>>(emptyList())

        /**
         * Lista completa en orden del servidor: fuente observada por la
         * pantalla de Favoritos para reflejar altas/bajas hechas en otras
         * pantallas al momento.
         */
        val favorites: StateFlow<List<FavoriteDto>> = _favorites.asStateFlow()

        /**
         * Lista completa en orden del servidor. Deduplica por id (VR-01)
         * y refresca caché + flujo. Sin red o con sesión no renovable y
         * con caché devuelve la última conocida marcada `offline`
         * (la verdad sigue siendo el servidor, FR-010).
         */
        suspend fun list(): ApiResult<FavoritesResult> {
            return when (val r = safeCall { api.list() }) {
                is ApiResult.Ok -> {
                    val items = r.value.items.distinctBy { it.station.id }
                    publish(items)
                    db.favoritesCache().replaceAll(items.toCached(System.currentTimeMillis()))
                    ApiResult.Ok(FavoritesResult(items, offline = false))
                }
                is ApiResult.Err -> {
                    if (r.error is DomainError.Unavailable || r.error is DomainError.Unauthorized) {
                        fromCache() ?: r
                    } else {
                        r
                    }
                }
            }
        }

        /** Añade un favorito por `stationId` (VR-03: en blanco se rechaza local). */
        suspend fun add(stationId: String): ApiResult<FavoriteDto> {
            if (stationId.isBlank()) return ApiResult.Err(DomainError.Validation(emptyList()))
            return when (val r = safeCall { api.add(AddFavoriteBody(stationId)) }) {
                is ApiResult.Ok -> {
                    _favoriteIds.value = _favoriteIds.value + stationId
                    _favorites.value = _favorites.value.filterNot { it.station.id == stationId } + r.value.favorite
                    val dao = db.favoritesCache()
                    dao.upsert(
                        r.value.favorite.toCachedSingle(
                            sortIndex = dao.nextSortIndex(),
                            now = System.currentTimeMillis(),
                        ),
                    )
                    ApiResult.Ok(r.value.favorite)
                }
                is ApiResult.Err -> r
            }
        }

        /**
         * Quita un favorito. Idempotente: 404 se reconcilia como éxito
         * (la emisora ya no está) y el id sale del flujo igualmente.
         */
        suspend fun remove(stationId: String): ApiResult<Unit> {
            if (stationId.isBlank()) return ApiResult.Err(DomainError.Validation(emptyList()))
            return when (val r = safeCall { api.remove(stationId) }) {
                is ApiResult.Ok -> {
                    _favoriteIds.value = _favoriteIds.value - stationId
                    _favorites.value = _favorites.value.filterNot { it.station.id == stationId }
                    db.favoritesCache().deleteById(stationId)
                    ApiResult.Ok(Unit)
                }
                is ApiResult.Err -> {
                    if (r.error is DomainError.NotFound) {
                        _favoriteIds.value = _favoriteIds.value - stationId
                        _favorites.value = _favorites.value.filterNot { it.station.id == stationId }
                        db.favoritesCache().deleteById(stationId)
                        ApiResult.Ok(Unit)
                    } else {
                        r
                    }
                }
            }
        }

        /** Guarda el orden personalizado (lista completa exacta). */
        suspend fun reorder(ids: List<String>): ApiResult<Unit> {
            return when (val r = safeCall { api.reorder(ReorderBody(ids)) }) {
                is ApiResult.Ok -> ApiResult.Ok(Unit)
                is ApiResult.Err -> r
            }
        }

        /** Vacía ids y caché al cerrar sesión o cambiar de instancia. */
        suspend fun clearLocal() {
            _favoriteIds.value = emptySet()
            _favorites.value = emptyList()
            db.favoritesCache().clear()
        }

        private suspend fun fromCache(): ApiResult<FavoritesResult>? {
            val cached = db.favoritesCache().loadOrdered()
            if (cached.isEmpty()) return null
            val favorites = cached.toFavorites()
            publish(favorites)
            return ApiResult.Ok(FavoritesResult(favorites, offline = true))
        }

        /** Publica la lista completa y sincroniza los ids observados. */
        private fun publish(items: List<FavoriteDto>) {
            _favorites.value = items
            _favoriteIds.value = items.map { it.station.id }.toSet()
        }

        private fun FavoriteDto.toCachedSingle(
            sortIndex: Int,
            now: Long,
        ): CachedFavorite = listOf(this).toCached(now).map { it.copy(sortIndex = sortIndex) }.single()
    }
