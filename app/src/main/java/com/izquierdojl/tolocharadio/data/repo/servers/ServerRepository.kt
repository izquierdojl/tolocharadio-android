package com.izquierdojl.tolocharadio.data.repo.servers

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.session.TokenStore
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
 * CRUD de servidores guardados. Cada servidor tiene sus propias
 * credenciales cifradas (refresh + email + password en [TokenStore]).
 *
 * `isActive` (sesión actual) e `isDefault` (arranque de la app) son
 * conceptos independientes (FR-005): cambiar de servidor no modifica
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
        private val tokenStore: TokenStore,
        private val shortcutClearer: ShortcutClearer,
    ) {
        /** Lista de servidores guardados (Flow). */
        val servers: Flow<List<SavedServerEntity>> = dao.getAll()

        /** Obtiene un servidor por ID. */
        suspend fun getById(id: String): SavedServerEntity? = dao.getById(id)

        /** Obtiene el servidor por defecto (arranque de la app). */
        suspend fun getDefault(): SavedServerEntity? = dao.getDefault()

        /** Obtiene el servidor activo (sesión actual). */
        suspend fun getActive(): SavedServerEntity? = dao.getActive()

        /**
         * Añade un servidor validando la URL contra GET /health.
         * Guarda email+password cifrados (FR-005). El primer servidor
         * se convierte en activo y por defecto automáticamente.
         */
        suspend fun add(
            url: String,
            alias: String,
            email: String = "",
            password: String = "",
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
                    userEmail = email.ifBlank { null },
                    isActive = makeActive,
                    isDefault = makeDefault,
                )
            dao.insert(server)

            if (makeActive) {
                tokenStore.setActiveServerId(server.id)
            }
            if (email.isNotBlank()) {
                tokenStore.saveServerAuth(server.id, email, password)
            }

            return ApiResult.Ok(server)
        }

        /**
         * Marca el servidor como **activo** (no toca `isDefault`, FR-006/C2),
         * intercambia las credenciales cifradas entre servidor anterior y
         * nuevo, y limpia la caché del anterior (FR-008b).
         *
         * El re-apuntado de la red (baseUrl + rebirth) y el auto-login con
         * el refresh guardado ocurren tras el renacimiento
         * ([com.izquierdojl.tolocharadio.core.session.SessionRestorer]).
         */
        suspend fun switchTo(serverId: String): ApiResult<SavedServerEntity> {
            val server =
                dao.getById(serverId)
                    ?: return ApiResult.Err(DomainError.NotFound("server_not_found"))

            // 1. Persistir el refresh actual bajo el servidor activo anterior
            val previousId = tokenStore.getActiveServerId() ?: dao.getActive()?.id
            if (previousId != null && previousId != serverId) {
                tokenStore.getRefresh()?.let { refresh ->
                    val prev = tokenStore.getServerCredentials(previousId)
                    tokenStore.setServerCredentials(
                        previousId,
                        refresh = refresh,
                        email = prev?.email,
                        password = prev?.password,
                    )
                }
            }

            // 2. Cambiar el activo sin tocar el por defecto
            dao.clearActive()
            dao.setActive(serverId)
            tokenStore.setActiveServerId(serverId)

            // 3. Cargar el refresh del servidor destino como sesión global;
            //    si no tiene, la app pedirá login (queda como credencial solo email)
            tokenStore.getServerCredentials(serverId)?.let { creds ->
                tokenStore.setRefresh(creds.refresh)
            }

            // 4. Limpiar caché del servidor anterior
            cacheManager.clearAll()

            // 5. Eliminar accesos directos del icono de la cuenta anterior (FR-010)
            shortcutClearer.clear()

            return ApiResult.Ok(server)
        }

        /**
         * Elimina un servidor, sus credenciales cifradas (FR-008) y,
         * si era el por defecto, promociona a otro servidor.
         */
        suspend fun delete(serverId: String) {
            val wasDefault = dao.getById(serverId)?.isDefault ?: false
            val wasActive = dao.getById(serverId)?.isActive ?: false
            tokenStore.deleteServerCredentials(serverId)
            dao.deleteById(serverId)

            if (wasDefault) {
                val remaining = dao.getAll().first()
                remaining.firstOrNull()?.let { dao.setDefault(it.id) }
            }
            if (wasActive) {
                val remaining = dao.getAll().first()
                val newActive = remaining.firstOrNull { it.isDefault } ?: remaining.firstOrNull()
                if (newActive != null) {
                    dao.setActive(newActive.id)
                    tokenStore.setActiveServerId(newActive.id)
                    tokenStore.setRefresh(tokenStore.getServerCredentials(newActive.id)?.refresh)
                } else {
                    tokenStore.setActiveServerId(null)
                }
            }
        }
    }
