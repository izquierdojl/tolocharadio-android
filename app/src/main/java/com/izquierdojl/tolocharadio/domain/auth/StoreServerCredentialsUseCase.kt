package com.izquierdojl.tolocharadio.domain.auth

import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import javax.inject.Inject

/**
 * Persiste email+password cifrados para el servidor activo tras un
 * login (FR-005). Son el respaldo para el re-login silencioso
 * (FR-006b). Nunca se loguean (FR-009).
 */
class StoreServerCredentialsUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
        private val tokens: TokenStore,
    ) {
        suspend operator fun invoke(
            email: String,
            password: String,
        ) {
            val active = repository.getActive() ?: return
            tokens.setActiveServerId(active.id)
            tokens.saveServerAuth(active.id, email.trim(), password)
        }
    }
