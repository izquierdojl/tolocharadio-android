package com.izquierdojl.tolocharadio.domain.servers

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import javax.inject.Inject

/**
 * Cambia el servidor **activo** y limpia la caché del anterior.
 * NO modifica el servidor por defecto (FR-006/C2).
 */
class SwitchServerUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
    ) {
        /**
         * Cambia al servidor con el ID dado.
         * Limpia la caché local antes de cargar datos del nuevo servidor.
         *
         * @param serverId ID del servidor a seleccionar
         * @return ApiResult con el servidor seleccionado o error
         */
        suspend operator fun invoke(serverId: String): ApiResult<SavedServer> =
            when (val result = repository.switchTo(serverId)) {
                is ApiResult.Ok -> ApiResult.Ok(
                    SavedServer(
                        id = result.value.id,
                        url = result.value.url,
                        alias = result.value.alias,
                        appName = result.value.appName,
                        userEmail = result.value.userEmail,
                        isActive = result.value.isActive,
                        isDefault = result.value.isDefault,
                        createdAt = result.value.createdAt,
                    ),
                )
                is ApiResult.Err -> result
            }
    }
