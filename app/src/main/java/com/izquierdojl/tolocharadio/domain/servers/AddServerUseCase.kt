package com.izquierdojl.tolocharadio.domain.servers

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.FieldError
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import javax.inject.Inject

/** Añade un servidor tras validar la URL (FR-012). */
class AddServerUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
    ) {
        /**
         * @param url URL de la instancia (se normaliza automáticamente)
         * @param alias Nombre asignado por el usuario
         * @param email Email de conexión (se cifra en el dispositivo)
         * @param password Password de conexión (se cifra en el dispositivo)
         * @param setAsDefault Si true, se marca como por defecto (arranque)
         * @return ApiResult con el servidor creado o error
         */
        suspend operator fun invoke(
            url: String,
            alias: String,
            email: String = "",
            password: String = "",
            setAsDefault: Boolean = false,
        ): ApiResult<SavedServer> {
            if (alias.isBlank()) {
                return ApiResult.Err(
                    DomainError.Validation(listOf(FieldError("alias", "El alias no puede estar vacío"))),
                )
            }
            return when (val result = repository.add(url, alias, email, password, setAsDefault)) {
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
                userEmail = userEmail,
                isActive = isActive,
                isDefault = isDefault,
                createdAt = createdAt,
            )
    }
