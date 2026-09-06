package com.izquierdojl.tolocharadio.domain.servers

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Obtiene la lista de servidores guardados. */
class GetServersUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
    ) {
        operator fun invoke(): Flow<List<SavedServer>> =
            repository.servers.map { list ->
                list.map { it.toDomain() }
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
