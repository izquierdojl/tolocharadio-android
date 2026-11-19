package com.izquierdojl.tolocharadio.domain.podcasts

/**
 * Retención de descargas (extensión spec 0047, 2026-11-19).
 * `NEVER` desactiva la limpieza automática; el resto son días naturales
 * desde la fecha de finalización de la descarga.
 */
object DownloadRetention {
    /** Sin limpieza automática. */
    const val NEVER = 0

    /** Retención por defecto: 30 días. */
    const val DEFAULT_DAYS = 30

    /** Opciones ofrecidas en Configuración. */
    val OPTIONS = listOf(7, 14, 30, 60, 90, NEVER)

    /** Etiqueta visible de una retención. */
    fun label(days: Int): String = if (days <= NEVER) "Nunca" else "$days días"
}
