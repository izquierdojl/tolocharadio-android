package com.izquierdojl.tolocharadio.feature.podcasts.subscriptions

import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import java.text.Collator
import java.util.Locale

/**
 * Órdenes de suscripciones (paridad con `subscriptionSort.ts` de la web).
 * El orden por defecto es el de episodios pendientes más recientes.
 */
enum class SubscriptionSort(val label: String) {
    RECENT_EPISODES("Episodios recientes"),
    NAME_ASC("Nombre (A → Z)"),
    NAME_DESC("Nombre (Z → A)"),
    RECENT("Últimas suscritas"),
    MANUAL("Orden manual"),
}

private val spanishCollator: Collator =
    Collator.getInstance(Locale.forLanguageTag("es-ES")).apply {
        strength = Collator.PRIMARY
    }

/** Compara títulos ignorando mayúsculas y acentos (como la web). */
private fun compareTitles(
    a: PodcastSummary,
    b: PodcastSummary,
): Int = spanishCollator.compare(a.title, b.title)

/**
 * Devuelve una copia ordenada según [order]. `RECENT` conserva el orden
 * del servidor (fecha de suscripción); `RECENT_EPISODES` coloca al final
 * los programas sin episodios pendientes y desempata por nombre.
 */
fun sortSubscriptions(
    items: List<PodcastSummary>,
    order: SubscriptionSort,
    manualOrder: List<Long> = emptyList(),
): List<PodcastSummary> =
    when (order) {
        SubscriptionSort.NAME_ASC ->
            items.sortedWith { a, b -> compareTitles(a, b) }
        SubscriptionSort.NAME_DESC ->
            items.sortedWith { a, b -> compareTitles(b, a) }
        SubscriptionSort.RECENT_EPISODES ->
            items.sortedWith(
                compareBy<PodcastSummary> { it.latestPendingEpisodeAt == null }
                    .thenByDescending { it.latestPendingEpisodeAt ?: Long.MIN_VALUE }
                    .thenComparator(::compareTitles),
            )
        SubscriptionSort.RECENT -> items
        SubscriptionSort.MANUAL -> {
            val position = manualOrder.withIndex().associate { it.value to it.index }
            items.sortedBy { position[it.id] ?: Int.MAX_VALUE }
        }
    }
