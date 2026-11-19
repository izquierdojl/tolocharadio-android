package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.repo.DownloadsRepo
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Limpia las descargas completadas más antiguas que la retención
 * configurada (extensión spec 0047). Se ejecuta al entrar en modo
 * Podcasts, al volver a primer plano y al cambiar la preferencia.
 */
class CleanupDownloadsUseCase
    @Inject
    constructor(
        private val downloads: DownloadsRepo,
        private val prefs: InstancePrefs,
        private val playback: EpisodePlaybackController,
    ) {
        suspend operator fun invoke(keepEpisodeId: Long? = null) {
            val days = prefs.downloadRetentionDays.first()
            val playingId =
                keepEpisodeId
                    ?: (playback.state.value as? EpisodePlaybackController.State.Active)
                        ?.episode
                        ?.id
            downloads.cleanupExpired(days, playingId)
        }
    }
