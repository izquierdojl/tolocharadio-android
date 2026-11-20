package com.izquierdojl.tolocharadio.feature.stats.blocks

/** Elemento genérico de una tarjeta de resumen. */
data class StatsSummaryItem(
    val label: String,
    val value: String,
    val hint: String? = null,
)
