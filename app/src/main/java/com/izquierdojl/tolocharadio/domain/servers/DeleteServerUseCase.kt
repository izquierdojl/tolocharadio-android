package com.izquierdojl.tolocharadio.domain.servers

import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import javax.inject.Inject

/** Elimina un servidor y sus datos asociados. */
class DeleteServerUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
    ) {
        suspend operator fun invoke(serverId: String) {
            repository.delete(serverId)
        }
    }
