package com.izquierdojl.tolocharadio.domain.servers

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.FieldError
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import javax.inject.Inject

/** Añade un servidor validando URL y credenciales (FR-001/FR-003). */
class AddServerUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
    ) {
        suspend operator fun invoke(
            url: String,
            alias: String,
            email: String,
            password: String,
        ): ApiResult<SavedServer> {
            ServerFormValidation.validate(alias, email, password)?.let { return it }
            return when (val result = repository.add(url, alias, email, password)) {
                is ApiResult.Ok -> ApiResult.Ok(result.value.toDomain())
                is ApiResult.Err -> result
            }
        }

        private fun SavedServerEntity.toDomain() =
            SavedServer(
                id = id,
                url = url,
                alias = alias,
                appName = appName,
                isActive = isActive,
                isDefault = isDefault,
                createdAt = createdAt,
            )
    }

/** Validación compartida del formulario de servidor (alta y edición). */
internal object ServerFormValidation {
    fun validate(
        alias: String,
        email: String,
        password: String,
    ): ApiResult.Err? {
        if (alias.isBlank()) {
            return ApiResult.Err(
                DomainError.Validation(listOf(FieldError("alias", "El alias no puede estar vacío"))),
            )
        }
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains('@') || !trimmedEmail.substringAfter('@').contains('.')) {
            return ApiResult.Err(
                DomainError.Validation(listOf(FieldError("email", "Escribe un email válido"))),
            )
        }
        if (password.isEmpty()) {
            return ApiResult.Err(
                DomainError.Validation(listOf(FieldError("password", "La contraseña no puede estar vacía"))),
            )
        }
        return null
    }
}
