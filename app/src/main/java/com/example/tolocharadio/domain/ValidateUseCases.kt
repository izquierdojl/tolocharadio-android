package com.example.tolocharadio.domain

import javax.inject.Inject

/** Normaliza la base de la instancia (FR-002). Rechaza `http://` salvo local. */
class NormalizeBaseUrlUseCase
    @Inject
    constructor() {
        /** Devuelve la URL normalizada o null si no es válida. */
        operator fun invoke(raw: String): String? {
            val url = raw.trim().takeIf { it.isNotEmpty() }?.withScheme()
            return url?.let { normalize(it) }
        }

        private fun String.withScheme(): String = if (contains("://")) this else "https://$this"

        private fun normalize(url: String): String? {
            val scheme = url.substringBefore("://").lowercase()
            val rest = url.substringAfter("://")
            val host = rest.substringBefore('/')
            val path = rest.substringAfter('/', missingDelimiterValue = "").trimEnd('/')
            if (rest.isBlank() || host.isBlank() || host.contains(' ')) {
                return null
            }
            if (!schemeAllowed(scheme, host)) {
                return null
            }
            return "$scheme://$host" + (if (path.isEmpty()) "" else "/$path")
        }

        private fun schemeAllowed(
            scheme: String,
            host: String,
        ): Boolean {
            if (scheme == "https") return true
            if (scheme != "http") return false
            return host == "localhost" || host.startsWith("10.") ||
                host.startsWith("192.168.") || host.endsWith(".local")
        }
    }

/** Validación de credenciales (email + password 8–72). */
class ValidateAuthUseCase
    @Inject
    constructor() {
        fun email(raw: String): String? {
            val v = raw.trim()
            if (v.isEmpty()) return "Introduce tu email."
            if (!v.contains('@') || !v.substringAfter('@').contains('.')) return "Ese email no parece válido."
            return null
        }

        fun password(raw: String): String? {
            if (raw.length < MIN_PASSWORD_LENGTH) return "Mínimo $MIN_PASSWORD_LENGTH caracteres."
            if (raw.length > MAX_PASSWORD_LENGTH) return "Máximo $MAX_PASSWORD_LENGTH caracteres."
            return null
        }

        fun name(raw: String): String? {
            if (raw.length > MAX_NAME_LENGTH) return "Máximo $MAX_NAME_LENGTH caracteres."
            return null
        }

        private companion object {
            const val MIN_PASSWORD_LENGTH = 8
            const val MAX_PASSWORD_LENGTH = 72
            const val MAX_NAME_LENGTH = 80
        }
    }
