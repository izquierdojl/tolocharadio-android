package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.local.TolochaDb
import com.izquierdojl.tolocharadio.data.local.toCustomCached
import com.izquierdojl.tolocharadio.data.local.toCustomStations
import com.izquierdojl.tolocharadio.data.remote.api.CustomStationsApi
import com.izquierdojl.tolocharadio.data.remote.dto.CreateCustomStationBody
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Lista de emisoras personalizadas con bandera offline (FR-011). */
data class CustomStationsResult(val items: List<StationDto>, val offline: Boolean)

/**
 * Emisoras personalizadas: lista completa del servidor (verdad), caché
 * Room de solo lectura. Sin paginación (el contrato devuelve `{items}`).
 */
@Singleton
class CustomStationsRepo
    @Inject
    constructor(
        private val api: CustomStationsApi,
        private val db: TolochaDb,
    ) {
        private val _items = MutableStateFlow<List<StationDto>>(emptyList())

        /** Emisoras personalizadas: fuente observada por el ViewModel. */
        val items: StateFlow<List<StationDto>> = _items.asStateFlow()

        /**
         * Lista completa del servidor en orden recibido. Sin red y con
         * caché devuelve la última conocida marcada `offline`.
         */
        suspend fun list(): ApiResult<CustomStationsResult> {
            return when (val r = safeCall { api.list() }) {
                is ApiResult.Ok -> {
                    val items = r.value.items.filter { it.id.isNotBlank() }
                    _items.value = items
                    db.customStationsCache().replaceAll(items.toCustomCached(System.currentTimeMillis()))
                    ApiResult.Ok(CustomStationsResult(items, offline = false))
                }
                is ApiResult.Err -> {
                    if (r.error is DomainError.Unavailable) fromCache() ?: r else r
                }
            }
        }

        /**
         * Crea una emisora personalizada. Los valores ya vienen recortados
         * y validados en cliente; el 422 del servidor se propaga con
         * `details` por campo para mostrarlo junto al formulario.
         */
        suspend fun create(
            name: String,
            url: String,
        ): ApiResult<StationDto> {
            if (name.isBlank() || url.isBlank()) return ApiResult.Err(DomainError.Validation(emptyList()))
            return when (val r = safeCall { api.create(CreateCustomStationBody(name, url)) }) {
                is ApiResult.Ok -> {
                    _items.value = _items.value + r.value.station
                    val cached = listOf(r.value.station).toCustomCached(System.currentTimeMillis())
                    db.customStationsCache().upsertAll(cached)
                    ApiResult.Ok(r.value.station)
                }
                is ApiResult.Err -> r
            }
        }

        /**
         * Elimina una emisora personalizada. 404 se reconcilia como éxito
         * (la emisora ya no está). La invalidación de Favoritos la hace
         * el ViewModel (research D4).
         */
        suspend fun delete(id: String): ApiResult<Unit> {
            if (id.isBlank()) return ApiResult.Err(DomainError.Validation(emptyList()))
            return when (val r = safeCall { api.delete(id) }) {
                is ApiResult.Ok -> {
                    _items.value = _items.value.filterNot { it.id == id }
                    db.customStationsCache().deleteById(id)
                    ApiResult.Ok(Unit)
                }
                is ApiResult.Err -> {
                    if (r.error is DomainError.NotFound) {
                        _items.value = _items.value.filterNot { it.id == id }
                        db.customStationsCache().deleteById(id)
                        ApiResult.Ok(Unit)
                    } else {
                        r
                    }
                }
            }
        }

        /** Vacía caché al cerrar sesión o cambiar de instancia. */
        suspend fun clearLocal() {
            _items.value = emptyList()
            db.customStationsCache().clear()
        }

        private suspend fun fromCache(): ApiResult<CustomStationsResult>? {
            val cached = db.customStationsCache().loadAll()
            if (cached.isEmpty()) return null
            _items.value = cached.toCustomStations()
            return ApiResult.Ok(CustomStationsResult(cached.toCustomStations(), offline = true))
        }
    }
