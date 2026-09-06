package com.izquierdojl.tolocharadio.core.util

/**
 * Normaliza URLs de instancias para comparación y almacenamiento.
 * - Quita barra final
 * - Lowercasing de scheme y host
 * - Preserva puerto explícito y path
 * - Añade https:// si no tiene scheme
 *
 * Parseo puro JVM (sin `android.net.Uri`) para ser testeable en
 * tests unitarios sin Robolectric.
 */
object UrlNormalizer {
    private val URL_REGEX = Regex("^(?i)(https?)://([^/?#]+)([^#]*)$")

    fun normalize(url: String): String {
        val trimmed = url.trim()
        val withScheme =
            if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true)
            ) {
                "https://$trimmed"
            } else {
                trimmed
            }
        val match = URL_REGEX.find(withScheme) ?: return withScheme
        val scheme = match.groupValues[1].lowercase()
        val authority = match.groupValues[2].lowercase()
        val path = match.groupValues[3].substringBefore('?').removeSuffix("/")
        return "$scheme://$authority$path"
    }
}
