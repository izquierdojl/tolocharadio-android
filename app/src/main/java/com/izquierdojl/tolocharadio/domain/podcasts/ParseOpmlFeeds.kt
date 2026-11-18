package com.izquierdojl.tolocharadio.domain.podcasts

/**
 * OPML 2.0 del lado cliente (puerto de `parseOpmlFeedUrls` web).
 * Extrae `xmlUrl` únicas, recortadas y en orden de aparición.
 */
fun parseOpmlFeedUrls(xml: String): List<String> {
    val pattern = Regex("""<outline\b[^>]*\bxmlUrl\s*=\s*(["'])(.*?)\1""")
    val seen = LinkedHashSet<String>()
    for (match in pattern.findAll(xml)) {
        val url = match.groupValues[2].trim()
        if (url.isNotEmpty()) seen.add(url)
    }
    return seen.toList()
}

/** Una dirección de feed válida es http/https no vacía. */
fun isValidFeedUrl(url: String): Boolean {
    val value = url.trim()
    if (value.isEmpty()) return false
    return value.startsWith("http://") || value.startsWith("https://")
}
