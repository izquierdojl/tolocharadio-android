package com.izquierdojl.tolocharadio.domain

import javax.inject.Inject

/** Normaliza la base del servidor (FR-004). Rechaza `http://` salvo local. */
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
