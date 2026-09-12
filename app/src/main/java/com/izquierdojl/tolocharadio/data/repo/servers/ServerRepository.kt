package com.izquierdojl.tolocharadio.data.repo.servers

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.util.UrlNormalizer
import com.izquierdojl.tolocharadio.data.local.CacheManager
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.local.servers.ServerDao
import com.izquierdojl.tolocharadio.data.remote.api.SystemApi
import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutClearer
import com.izquierdojl.tolocharadio.feature.onboarding.InstanceValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CRUD de servidores guardados. No almacena credenciales (FR-009):
 * un servidor es URL + alias + estado activo/por defecto.
 *
 * `isActive` (sesión actual) e `isDefault` (arranque de la app) son
 * conceptos independientes (FR-006): cambiar de servidor no modifica
 * el por defecto.
 */
@Singleton
class ServerRepository
    @Inject
    constructor(
        private val dao: ServerDao,
        private val instanceValidator: InstanceValidator,
        private val systemApi: SystemApi,
        private val cacheManager: CacheManager,
        private val shortcutClearer: ShortcutClearer,
    ) {
        /** Lista de servidores guardados (Flow). */
        val servers: Flow<List<SavedServerEntity>> = dao.getAll()

        /** Obtiene un servidor por ID. */
        suspend fun getById(id: String): SavedServerEntity? = dao.getById(id)

        /** Obtiene el servidor activo (sesión actual). */
        suspend fun getActive(): SavedServerEntity? = dao.getActive()

        /**
         * Añade un servidor validando la URL contra GET /health.
         * El primer servidor se convierte en activo y por defecto
         * automáticamente.
         */
        suspend fun add(
            url: String,
            alias: String,
            setAsDefault: Boolean = false,
        ): ApiResult<SavedServerEntity> {
            val normalizedUrl = UrlNormalizer.normalize(url)

            // Validar que la instancia es accesible
            val isValid = instanceValidator.validate(normalizedUrl)
            if (!isValid) {
                return ApiResult.Err(
                    DomainError.Unavailable("No se pudo conectar a la instancia"),
                )
            }

            // Obtener nombre de la app
            val appName =
                try {
                    val config = systemApi.config()
                    if (config.isSuccessful) config.body()?.appName else null
                } catch (_: Exception) {
                    null
                }

            val count = dao.count()
            val makeDefault = setAsDefault || count == 0
            val makeActive = count == 0

            if (makeDefault) dao.clearDefault()
            if (makeActive) dao.clearActive()

            val server =
                SavedServerEntity(
                    url = normalizedUrl,
                    alias = alias,
                    appName = appName,
                    isActive = makeActive,
                    isDefault = makeDefault,
                )
            dao.insert(server)

            return ApiResult.Ok(server)
        }

        /**
         * Marca el servidor como **activo** (no toca `isDefault`, FR-006)
         * y limpia la caché del servidor anterior (FR-007).
         */
        suspend fun switchTo(serverId: String): ApiResult<SavedServerEntity> {
            val server =
                dao.getById(serverId)
                    ?: return ApiResult.Err(DomainError.NotFound("server_not_found"))

            dao.clearActive()
            dao.setActive(serverId)
            cacheManager.clearAll()
            shortcutClearer.clear()

            return ApiResult.Ok(server)
        }

        /**
         * Elimina un servidor y, si era el por defecto o el activo,
         * promociona a otro servidor (FR-013).
         */
        suspend fun delete(serverId: String) {
            val server = dao.getById(serverId) ?: return
            dao.deleteById(serverId)

            if (server.isDefault) {
                dao.getAll().first().firstOrNull()?.let { dao.setDefault(it.id) }
            }
            if (server.isActive) {
                val remaining = dao.getAll().first()
                val newActive = remaining.firstOrNull { it.isDefault } ?: remaining.firstOrNull()
                if (newActive != null) {
                    dao.setActive(newActive.id)
                }
            }
        }
    }
