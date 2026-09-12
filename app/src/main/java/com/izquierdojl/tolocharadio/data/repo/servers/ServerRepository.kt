package com.izquierdojl.tolocharadio.data.repo.servers

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.core.util.UrlNormalizer
import com.izquierdojl.tolocharadio.data.local.CacheManager
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.local.servers.ServerDao
import com.izquierdojl.tolocharadio.data.remote.api.SystemApi
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutClearer
import com.izquierdojl.tolocharadio.feature.onboarding.InstanceValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CRUD de servidores con **credenciales por servidor** (email +
 * contraseña cifradas en [TokenStore]). El alta y la edición validan
 * la instancia y hacen login antes de persistir (FR-001/FR-003/FR-007).
 *
 * `isActive` (sesión actual) e `isDefault` (arranque de la app) son
 * conceptos independientes: cambiar de servidor no modifica el por
 * defecto.
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
        private val tokens: TokenStore,
        private val authRepo: AuthRepo,
    ) {
        /** Lista de servidores guardados (Flow). */
        val servers: Flow<List<SavedServerEntity>> = dao.getAll()

        suspend fun getById(id: String): SavedServerEntity? = dao.getById(id)

        suspend fun getActive(): SavedServerEntity? = dao.getActive()

        /** Servidor de arranque: activo → por defecto → primero. */
        suspend fun getStartupServer(): SavedServerEntity? {
            dao.getActive()?.let { return it }
            val all = dao.getAll().first()
            return all.firstOrNull { it.isDefault } ?: all.firstOrNull()
        }

        /** true si el servidor tiene email y contraseña cifrados (FR-010). */
        fun hasCredentials(serverId: String): Boolean = tokens.hasCredentials(serverId)

        /**
         * Añade un servidor: valida la instancia, hace login con las
         * credenciales y persiste servidor + credenciales cifradas. El
         * primer servidor queda activo y por defecto.
         */
        suspend fun add(
            url: String,
            alias: String,
            email: String,
            password: String,
        ): ApiResult<SavedServerEntity> {
            val normalizedUrl = UrlNormalizer.normalize(url)
            if (!instanceValidator.validate(normalizedUrl)) {
                return ApiResult.Err(
                    DomainError.Unavailable("No se pudo conectar a la instancia. Revisa la URL y tu conexión."),
                )
            }
            val appName = readAppName()
            val id = UUID.randomUUID().toString()

            // Login primero contra la URL introducida: si las credenciales fallan, no se persiste nada.
            when (val auth = authRepo.login(id, normalizedUrl, email, password)) {
                is ApiResult.Err -> return auth
                is ApiResult.Ok -> Unit
            }

            val count = dao.count()
            val makeDefault = count == 0
            val makeActive = count == 0
            if (makeDefault) dao.clearDefault()
            if (makeActive) dao.clearActive()

            val server =
                SavedServerEntity(
                    id = id,
                    url = normalizedUrl,
                    alias = alias,
                    appName = appName,
                    isActive = makeActive,
                    isDefault = makeDefault,
                )
            dao.insert(server)
            if (makeActive) tokens.setActiveServerId(id)
            return ApiResult.Ok(server)
        }

        /**
         * Edita alias y credenciales de un servidor. Revalida con login
         * (FR-007); si falla, no se actualiza nada.
         */
        suspend fun update(
            serverId: String,
            alias: String,
            email: String,
            password: String,
        ): ApiResult<SavedServerEntity> {
            val server = dao.getById(serverId) ?: return ApiResult.Err(DomainError.NotFound("server_not_found"))
            when (val auth = authRepo.login(serverId, server.url, email, password)) {
                is ApiResult.Err -> return auth
                is ApiResult.Ok -> Unit
            }
            val updated = server.copy(alias = alias)
            dao.insert(updated)
            return ApiResult.Ok(updated)
        }

        /**
         * Marca el servidor como activo y asegura su sesión (refresh o
         * re-login). Si no se puede autenticar, no cambia el activo.
         */
        suspend fun switchTo(serverId: String): ApiResult<SavedServerEntity> {
            val server = dao.getById(serverId) ?: return ApiResult.Err(DomainError.NotFound("server_not_found"))
            when (val auth = authRepo.ensureSession(serverId, server.url)) {
                is ApiResult.Err -> return auth
                is ApiResult.Ok -> Unit
            }
            dao.clearActive()
            dao.setActive(serverId)
            tokens.setActiveServerId(serverId)
            cacheManager.clearAll()
            shortcutClearer.clear()
            return ApiResult.Ok(server)
        }

        /**
         * Elimina un servidor, sus credenciales cifradas y, si era el
         * por defecto/activo, promociona a otro.
         */
        suspend fun delete(serverId: String) {
            val server = dao.getById(serverId) ?: return
            tokens.deleteCredentials(serverId)
            dao.deleteById(serverId)

            if (server.isDefault) {
                dao.getAll().first().firstOrNull()?.let { dao.setDefault(it.id) }
            }
            if (server.isActive) {
                val remaining = dao.getAll().first()
                val newActive = remaining.firstOrNull { it.isDefault } ?: remaining.firstOrNull()
                if (newActive != null) {
                    dao.setActive(newActive.id)
                    tokens.setActiveServerId(newActive.id)
                } else {
                    tokens.setActiveServerId(null)
                }
            }
        }

        private suspend fun readAppName(): String? =
            try {
                val config = systemApi.config()
                if (config.isSuccessful) config.body()?.appName else null
            } catch (_: Exception) {
                null
            }
    }
