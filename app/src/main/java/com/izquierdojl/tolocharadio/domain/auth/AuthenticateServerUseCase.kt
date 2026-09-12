package com.izquierdojl.tolocharadio.domain.auth

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import javax.inject.Inject

/**
 * Asegura la sesión del servidor de arranque (activo → por defecto →
 * primero): intenta refresh y, si falla, re-login con las credenciales
 * guardadas (FR-004/FR-005). No pide nada al usuario.
 */
class AuthenticateServerUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
        private val authRepo: AuthRepo,
    ) {
        suspend operator fun invoke(): ApiResult<Unit> {
            val server =
                repository.getStartupServer()
                    ?: return ApiResult.Err(DomainError.NotFound("server_not_found"))
            return authRepo.ensureSession(server.id, server.url)
        }
    }
