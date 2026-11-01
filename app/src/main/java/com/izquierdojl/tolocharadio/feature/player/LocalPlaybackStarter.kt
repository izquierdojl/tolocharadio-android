package com.izquierdojl.tolocharadio.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
import com.izquierdojl.tolocharadio.domain.playback.ResolvePlaybackSourceUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arranque de reproducción local reutilizado por el ViewModel, la recuperación
 * y el cambio de perfil (spec 0042/0044).
 *
 * `recordHistory = false` en fallback/recovery para no duplicar la entrada de
 * historial. La ruta `CONTINUOUS` usa `/live`; `LEGACY` conserva HLS/progresivo.
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
        private val sessionState: PlaybackSessionState,
    ) {
        /** Arranque normal con la ruta y el marcador actuales de la sesión. */
        @OptIn(UnstableApi::class)
        suspend fun start(
            station: StationDto,
            recordHistory: Boolean = true,
            playWhenReady: Boolean = true,
        ) {
            start(
                station = station,
                route = sessionState.route,
                sequence = sessionState.nextSequence(),
                recordHistory = recordHistory,
                playWhenReady = playWhenReady,
            )
        }

        /** Arranque explícito por ruta, usado por fallback/recovery. */
        @OptIn(UnstableApi::class)
        suspend fun start(
            station: StationDto,
            route: PlaybackRoute,
            sequence: Long? = null,
            recordHistory: Boolean = false,
            playWhenReady: Boolean = true,
        ) {
            val source = resolveSource(station)
            val baseUrl = prefs.baseUrl.first()
            val effectiveSequence = sequence ?: sessionState.nextSequence()
            val item =
                when (route) {
                    PlaybackRoute.CONTINUOUS ->
                        mediaItemFactory.createLive(station, baseUrl, effectiveSequence)
                    PlaybackRoute.LEGACY ->
                        mediaItemFactory.createLegacy(station, source, baseUrl, effectiveSequence)
                }
            engine.player.setMediaSource(mediaItemFactory.createMediaSource(item, source, route))
            engine.player.prepare()
            engine.player.playWhenReady = playWhenReady
            if (recordHistory) {
                history.recordLocalPlay(station)
            }
        }
    }
