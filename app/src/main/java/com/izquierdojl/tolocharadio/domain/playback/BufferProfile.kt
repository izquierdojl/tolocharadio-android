package com.izquierdojl.tolocharadio.domain.playback

/** Perfiles de buffer ofrecidos al usuario (spec 0042, FR-010). */
enum class BufferProfile {
    DATA_SAVER,
    BALANCED,
    MAX_STABILITY,
    ;

    companion object {
        /** Perfil por defecto en una instalación nueva. */
        val DEFAULT: BufferProfile = BALANCED

        /** Convierte un valor persistido, con fallback seguro. */
        fun fromPersisted(value: String?): BufferProfile =
            value?.let { name ->
                entries.firstOrNull { it.name == name }
            } ?: DEFAULT
    }
}
