package com.izquierdojl.tolocharadio.domain.podcasts

/**
 * Política del potenciador de voz (FR-027/FR-029):
 * solo se aplica con preferencia activa, episodio local activo y sin Cast.
 */
object VoiceBoostPolicy {
    fun isEffective(
        preferenceEnabled: Boolean,
        episodeActive: Boolean,
        castConnected: Boolean,
    ): Boolean = preferenceEnabled && episodeActive && !castConnected
}
