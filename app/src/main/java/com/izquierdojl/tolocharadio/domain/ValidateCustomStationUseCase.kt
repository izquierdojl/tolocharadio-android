package com.izquierdojl.tolocharadio.domain

import javax.inject.Inject

/**
 * Validación cliente del formulario de emisora personalizada (FR-004).
 * Puro Kotlin, sin dependencias Android. El límite de 256 caracteres del
 * nombre lo impone el servidor (422); aquí solo se exige no vacío.
 */
class ValidateCustomStationUseCase
    @Inject
    constructor() {
        /** null si válido: nombre no vacío tras recortar espacios. */
        fun name(raw: String): String? {
            if (raw.trim().isEmpty()) return "Escribe un nombre para la emisora."
            return null
        }

        /**
         * null si válido: URL parseable con esquema `http` o `https`.
         * A diferencia de `NormalizeBaseUrlUseCase`, se acepta `http://`
         * en cualquier host (un stream http público es válido).
         */
        fun streamUrl(raw: String): String? {
            val v = raw.trim()
            val schemeEnd = v.indexOf("://")
            if (schemeEnd <= 0 || v.substring(0, schemeEnd).contains(' ') ||
                v.substring(schemeEnd + SCHEME_SEPARATOR_LENGTH).isBlank()
            ) {
                return "La URL del stream no es válida."
            }
            val scheme = v.substring(0, schemeEnd).lowercase()
            if (scheme != "http" && scheme != "https") return "La URL debe empezar por http:// o https://."
            return null
        }

        private companion object {
            /** Longitud de `"://"`: el host empieza justo después. */
            const val SCHEME_SEPARATOR_LENGTH = 3
        }
    }

