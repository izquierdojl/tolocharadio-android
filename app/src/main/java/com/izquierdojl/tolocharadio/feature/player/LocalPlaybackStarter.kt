package com.izquierdojl.tolocharadio.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.domain.playback.ResolvePlaybackSourceUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arranque de reproducción local reutilizado por el ViewModel, la recuperación
 * y el cambio de perfil (spec 0042, R2/R4).
 *
 * `recordHistory = false` en recuperación y cambio de perfil para no duplicar
 * la entrada de historial (FR-019).
 */
@Singleton
class LocalPlaybackStarter
    @Inject
    constructor(
        private val engine: PlaybackEngine,
        private val mediaItemFactory: StationMediaItemFactory,
        private val resolveSource: ResolvePlaybackSourceUseCase,
        private val prefs: InstancePrefs,
        private val history: HistoryRepo,
    ) {
        @OptIn(UnstableApi::class)
        suspend fun start(
            station: StationDto,
            recordHistory: Boolean = true,
            playWhenReady: Boolean = true,
        ) {
            val source = resolveSource(station)
            val baseUrl = prefs.baseUrl.first()
            val item = mediaItemFactory.create(station, source, baseUrl)
            engine.player.setMediaSource(mediaItemFactory.createMediaSource(item, source))
            engine.player.prepare()
            engine.player.playWhenReady = playWhenReady
            if (recordHistory) {
                history.recordLocalPlay(station)
            }
        }
    }
