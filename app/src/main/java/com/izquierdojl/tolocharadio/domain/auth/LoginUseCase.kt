package com.izquierdojl.tolocharadio.domain.auth

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.dto.UserDto
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import javax.inject.Inject

/**
 * Login con email y password. Delega a [AuthRepo] y maneja errores.
 *
 * Caso de uso puro del domain layer (constitución I).
 */
class LoginUseCase
    @Inject
    constructor(
        private val authRepo: AuthRepo,
    ) {
        /**
         * Realiza login con las credenciales dadas.
         *
         * @param email Email del usuario
         * @param password Contraseña
         * @return [ApiResult] con el usuario en caso de éxito
         */
        suspend operator fun invoke(
            email: String,
            password: String,
        ): ApiResult<UserDto> = authRepo.login(email, password)
    }
