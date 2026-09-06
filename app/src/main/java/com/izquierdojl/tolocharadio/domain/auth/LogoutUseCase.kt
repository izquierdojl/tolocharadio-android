package com.izquierdojl.tolocharadio.domain.auth

import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import javax.inject.Inject

/**
 * Cierra sesión en servidor y limpia tokens locales.
 *
 * FR-007: borra el refresh global y el snapshot de refresh del
 * servidor activo (revocado por `POST /auth/logout`). Conserva el
 * email+password cifrados del servidor para la reconexión desde la
 * lista de servidores (FR-014).
 *
 * Caso de uso puro del domain layer (constitución I).
 */
class LogoutUseCase
    @Inject
    constructor(
        private val authRepo: AuthRepo,
        private val tokens: TokenStore,
    ) {
        /**
         * Cierra sesión. Llama a POST /auth/logout en el servidor
         * y limpia tokens locales aunque la red falle.
         */
        suspend operator fun invoke() {
            authRepo.logout()
            tokens.getActiveServerId()?.let { serverId ->
                tokens.getServerCredentials(serverId)?.let { creds ->
                    tokens.setServerCredentials(
                        serverId,
                        refresh = null,
                        email = creds.email,
                        password = creds.password,
                    )
                }
            }
        }
    }
