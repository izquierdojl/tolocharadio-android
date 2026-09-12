package com.izquierdojl.tolocharadio.domain.servers

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import javax.inject.Inject

/** Edita alias y credenciales de un servidor y revalida con login (FR-007). */
class UpdateServerUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
    ) {
        suspend operator fun invoke(
            serverId: String,
            alias: String,
            email: String,
            password: String,
        ): ApiResult<SavedServer> {
            ServerFormValidation.validate(alias, email, password)?.let { return it }
            return when (val result = repository.update(serverId, alias, email, password)) {
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
