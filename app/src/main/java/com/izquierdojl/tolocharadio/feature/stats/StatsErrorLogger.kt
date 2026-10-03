package com.izquierdojl.tolocharadio.feature.stats

import android.util.Log
import com.izquierdojl.tolocharadio.core.network.DomainError
import javax.inject.Inject

/**
 * Log estructurado de los fallos al cargar las gráficas (Principio IV):
 * tag fijo, tipo de error, código o campos del contrato y causa raíz;
 * sin PII, credenciales ni mensajes crudos del servidor.
 */
class StatsErrorLogger
    @Inject
    constructor() {
        /** Registra [error] como error de carga de gráficas. */
        fun log(error: DomainError) {
            Log.e(TAG, "stats load failed: ${describe(error)}", rootCause(error))
        }

        private fun describe(error: DomainError): String =
            when (error) {
                is DomainError.Unauthorized -> "unauthorized code=${error.code}"
                is DomainError.NotFound -> "not_found code=${error.code}"
                is DomainError.Conflict -> "conflict code=${error.code}"
                is DomainError.Validation ->
                    "validation fields=${error.details.joinToString(separator = ",") { it.field }}"
                is DomainError.Unavailable -> "unavailable"
                is DomainError.Unknown -> "unknown"
            }

        private fun rootCause(error: DomainError): Throwable? =
            when (error) {
                is DomainError.Unavailable -> error.cause
                is DomainError.Unknown -> error.cause
                else -> null
            }

        private companion object {
            const val TAG = "StatsLoad"
        }
    }
