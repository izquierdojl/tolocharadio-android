package com.izquierdojl.tolocharadio.domain.podcasts

/**
 * Lógica pura de cola de episodios (puerto de `podcastQueue.ts` web).
 * Opera sobre ids para mantenerse testeable sin Android.
 *
 * Reconstruye el orden total conservando el hueco del episodio en
 * curso (oculto de pendientes). Si no hay oculto o no está en la
 * lista, devuelve el orden visible tal cual.
 */
fun mergeQueueOrder(
    allIds: List<Long>,
    visibleOrder: List<Long>,
    hiddenEpisodeId: Long?,
): List<Long> {
    if (hiddenEpisodeId == null) return visibleOrder
    val hiddenIndex = allIds.indexOf(hiddenEpisodeId)
    if (hiddenIndex == -1) return visibleOrder
    val merged = visibleOrder.filterNot { it == hiddenEpisodeId }.toMutableList()
    merged.add(hiddenIndex.coerceAtMost(merged.size), hiddenEpisodeId)
    return merged
}

/** Aplica un orden local quedándose solo con episodios vigentes. */
fun applyQueueOrder(
    items: List<Long>,
    order: List<Long>,
): List<Long> {
    val current = items.toSet()
    return order.filter { it in current }
}

/** Siguiente episodio tras terminar [endedEpisodeId], o null si no queda otro. */
fun nextQueueEpisodeId(
    ids: List<Long>,
    endedEpisodeId: Long,
): Long? = ids.firstOrNull { it != endedEpisodeId }
