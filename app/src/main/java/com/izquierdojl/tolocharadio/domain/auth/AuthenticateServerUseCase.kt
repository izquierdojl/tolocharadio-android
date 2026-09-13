package com.izquierdojl.tolocharadio.domain.auth

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import javax.inject.Inject

/**
 * Asegura la sesión del servidor de arranque (activo → por defecto →
 * primero): intenta refresh y, si falla, re-login con las credenciales
 * guardadas (FR-004/FR-005). No pide nada al usuario.
 *
 * Además fija el servidor activo en [TokenStore]: la renovación
 * transparente ante 401 usa sus credenciales, y los servidores migrados
 * o editados sin `active_server_id` recuperan así la sesión tras el
 * reposo.
 */
class AuthenticateServerUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
        private val authRepo: AuthRepo,
        private val tokens: TokenStore,
    ) {
        suspend operator fun invoke(): ApiResult<Unit> {
            val server =
                repository.getStartupServer()
                    ?: return ApiResult.Err(DomainError.NotFound("server_not_found"))
            tokens.setActiveServerId(server.id)
            return authRepo.ensureSession(server.id, server.url)
        }
    }
