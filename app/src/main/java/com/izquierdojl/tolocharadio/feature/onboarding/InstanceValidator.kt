package com.izquierdojl.tolocharadio.feature.onboarding

import com.izquierdojl.tolocharadio.data.remote.InstanceApiFactory
import javax.inject.Inject
import javax.inject.Singleton

/** Valida una base candidata con `/health`+`/config` sin tocar el grafo. */
@Singleton
class InstanceValidator
    @Inject
    constructor(
        private val apiFactory: InstanceApiFactory,
    ) {
        /** true si responde como instancia TolochaRadio válida. Nunca lanza: es una sonda. */
        suspend fun validate(baseUrl: String): Boolean =
            runCatching {
                val api = apiFactory.systemApi(baseUrl)
                api.health().isSuccessful && api.config().isSuccessful
            }.getOrDefault(false)
    }
