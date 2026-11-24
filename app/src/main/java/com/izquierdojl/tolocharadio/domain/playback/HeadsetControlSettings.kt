package com.izquierdojl.tolocharadio.domain.playback

/** Milisegundos por segundo. */
private const val MS_PER_SECOND = 1_000L

/**
 * Preferencias globales de los controles de auricular (spec 0060).
 *
 * Los segundos se restringen a [OPTIONS_SECONDS]; cualquier valor ausente o
 * inválido cae a su default. La preferencia es global para la app.
 */
data class HeadsetControlSettings(
    val skipBackSeconds: Int = DEFAULT_SKIP_BACK_SECONDS,
    val skipForwardSeconds: Int = DEFAULT_SKIP_FORWARD_SECONDS,
    val gesturesEnabled: Boolean = true,
) {
    /** Salto atrás en milisegundos. */
    val skipBackMs: Long
        get() = skipBackSeconds * MS_PER_SECOND

    /** Salto adelante en milisegundos. */
    val skipForwardMs: Long
        get() = skipForwardSeconds * MS_PER_SECOND

    companion object {
        /** Default de retroceso (FR-007). */
        const val DEFAULT_SKIP_BACK_SECONDS = 15

        /** Default de avance (FR-007). */
        const val DEFAULT_SKIP_FORWARD_SECONDS = 30

        /** Opciones admitidas en Configuración (FR-008). */
        val OPTIONS_SECONDS: List<Int> = listOf(5, 10, 15, 20, 30, 45, 60)

        /** Preferencia por defecto: 15 s atrás, 30 s adelante y gestos activos. */
        val DEFAULT = HeadsetControlSettings()

        /** Devuelve un valor válido de retroceso o el default. */
        fun sanitizeBackSeconds(value: Int?): Int = sanitize(value, DEFAULT_SKIP_BACK_SECONDS)

        /** Devuelve un valor válido de avance o el default. */
        fun sanitizeForwardSeconds(value: Int?): Int = sanitize(value, DEFAULT_SKIP_FORWARD_SECONDS)

        /** Construye la preferencia desde valores persistidos, saneando cada campo. */
        fun fromPersisted(
            backSeconds: Int?,
            forwardSeconds: Int?,
            gesturesEnabled: Boolean?,
        ): HeadsetControlSettings =
            HeadsetControlSettings(
                skipBackSeconds = sanitizeBackSeconds(backSeconds),
                skipForwardSeconds = sanitizeForwardSeconds(forwardSeconds),
                gesturesEnabled = gesturesEnabled ?: true,
            )

        private fun sanitize(
            value: Int?,
            fallback: Int,
        ): Int = value?.takeIf { it in OPTIONS_SECONDS } ?: fallback
    }
}
