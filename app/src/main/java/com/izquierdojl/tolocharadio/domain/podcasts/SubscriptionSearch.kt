package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import java.text.Normalizer
import java.util.Locale

private val COMBINING_MARKS = "\\p{M}+".toRegex()

/**
 * Normaliza texto para búsquedas por coincidencia parcial: minúsculas y
 * sin marcas diacríticas. Así «camion» coincide con «El camión fantasma».
 */
fun normalizeSearchText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)

/**
 * Filtra suscripciones por título con coincidencia parcial, sin distinguir
 * mayúsculas ni tildes y conservando el orden recibido. Un término vacío o
 * compuesto solo por espacios devuelve la lista completa.
 */
fun filterSubscriptionsByTitle(
    items: List<PodcastSummary>,
    term: String,
): List<PodcastSummary> {
    val needle = normalizeSearchText(term.trim())
    if (needle.isEmpty()) return items.toList()
    return items.filter { normalizeSearchText(it.title).contains(needle) }
}
