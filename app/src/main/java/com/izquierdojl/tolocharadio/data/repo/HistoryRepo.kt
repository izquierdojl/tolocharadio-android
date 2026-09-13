package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.local.TolochaDb
import com.izquierdojl.tolocharadio.data.local.toCached
import com.izquierdojl.tolocharadio.data.local.toHistoryEntries
import com.izquierdojl.tolocharadio.data.remote.api.HistoryApi
import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Lista de historial con bandera offline (FR-010). */
data class HistoryResult(val items: List<HistoryEntryDto>, val offline: Boolean)

/**
 * Historial: lista completa del servidor (verdad), caché Room de
 * solo lectura. Deduplica por `station.id` conservando la reproducción
 * más reciente por emisora.
 */
@Singleton
class HistoryRepo
    @Inject
    constructor(
        private val api: HistoryApi,
        private val db: TolochaDb,
    ) {
        private val _items = MutableStateFlow<List<HistoryEntryDto>>(emptyList())

        /** Entradas de historial: fuente observada por el ViewModel. */
        val items: StateFlow<List<HistoryEntryDto>> = _items.asStateFlow()

        /**
         * Lista completa del servidor. Deduplica por id (más reciente
         * por emisora) y refresca caché. Sin red o con sesión no
         * renovable y con caché devuelve la última conocida marcada
         * `offline`.
         */
        suspend fun list(): ApiResult<HistoryResult> {
            return when (val r = safeCall { api.list() }) {
                is ApiResult.Ok -> {
                    val items = deduplicate(r.value.items)
                    _items.value = items
                    db.historyCache().replaceAll(items.toCached(System.currentTimeMillis()))
                    ApiResult.Ok(HistoryResult(items, offline = false))
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

        /**
         * Elimina una emisora del historial. 404 se reconcilia como éxito
         * (la emisora ya no está en el historial).
         */
        suspend fun remove(stationId: String): ApiResult<Unit> {
            if (stationId.isBlank()) return ApiResult.Err(DomainError.Validation(emptyList()))
            return when (val r = safeCall { api.remove(stationId) }) {
                is ApiResult.Ok -> {
                    _items.value = _items.value.filterNot { it.station.id == stationId }
                    db.historyCache().deleteById(stationId)
                    ApiResult.Ok(Unit)
                }
                is ApiResult.Err -> {
                    if (r.error is DomainError.NotFound) {
                        _items.value = _items.value.filterNot { it.station.id == stationId }
                        db.historyCache().deleteById(stationId)
                        ApiResult.Ok(Unit)
                    } else {
                        r
                    }
                }
            }
        }

        /**
         * Limpia todo el historial. Devuelve éxito o error de red.
         * La reversión se gestiona en el ViewModel.
         */
        suspend fun clear(): ApiResult<Unit> {
            return when (val r = safeCall { api.clear() }) {
                is ApiResult.Ok -> {
                    _items.value = emptyList()
                    db.historyCache().clear()
                    ApiResult.Ok(Unit)
                }
                is ApiResult.Err -> r
            }
        }

        /** Vacía caché al cerrar sesión o cambiar de instancia. */
        suspend fun clearLocal() {
            _items.value = emptyList()
            db.historyCache().clear()
        }

        /**
         * Última lista cacheada (offline), sin tocar [items]. La usa el
         * publicador de accesos directos cuando el refresco no está
         * disponible (FR-015).
         */
        suspend fun snapshot(): List<HistoryEntryDto> = db.historyCache().loadOrdered().toHistoryEntries()

        private suspend fun fromCache(): ApiResult<HistoryResult>? {
            val cached = db.historyCache().loadOrdered()
            if (cached.isEmpty()) return null
            _items.value = cached.toHistoryEntries()
            return ApiResult.Ok(HistoryResult(cached.toHistoryEntries(), offline = true))
        }

        /**
         * Deduplica por `station.id` conservando la entrada con `playedAt`
         * más reciente (paridad con la web).
         */
        private fun deduplicate(entries: List<HistoryEntryDto>): List<HistoryEntryDto> =
            entries
                .groupBy { it.station.id }
                .map { (_, group) -> group.maxByOrNull { it.playedAt }!! }
                .sortedByDescending { it.playedAt }
    }
