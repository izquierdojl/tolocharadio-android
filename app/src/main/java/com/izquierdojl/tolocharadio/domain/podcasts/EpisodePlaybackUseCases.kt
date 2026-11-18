package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.repo.DownloadsRepo
import com.izquierdojl.tolocharadio.data.repo.ProgressRepo
import com.izquierdojl.tolocharadio.data.repo.QueueRepo
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Fuente efectiva de un episodio: archivo local o URL firmada (T040). */
data class EpisodePlaybackSource(
    val uri: String,
    val local: Boolean,
    val expiresAt: Long,
)

/** Margen de reemisión anticipada de la URL firmada (segundos). */
const val PLAYBACK_REFRESH_MARGIN_SEC = 300L

/**
 * Resuelve cómo reproducir: archivo local completo si existe
 * (ahorro de datos, FR-024) o URL firmada del servidor (research D3).
 */
class ResolveEpisodePlaybackUseCase
    @Inject
    constructor(
        private val api: PodcastsApi,
        private val downloads: DownloadsRepo,
        private val prefs: InstancePrefs,
    ) {
        suspend operator fun invoke(episode: PodcastEpisode): ApiResult<EpisodePlaybackSource> {
            downloads.localUriFor(episode.id)?.let { uri ->
                return ApiResult.Ok(EpisodePlaybackSource(uri, local = true, expiresAt = Long.MAX_VALUE))
            }
            return when (val r = safeCall { api.issuePlayback(episode.id) }) {
                is ApiResult.Ok -> {
                    val absolute = absoluteUrl(prefs.baseUrl.first(), r.value.url)
                    ApiResult.Ok(
                        EpisodePlaybackSource(absolute, local = false, expiresAt = r.value.expiresAt),
                    )
                }
                is ApiResult.Err -> r
            }
        }

        /** La URL firmada debe reemitirse cuando vence dentro del margen. */
        fun needsRefresh(
            source: EpisodePlaybackSource,
            nowSec: Long,
            marginSec: Long = PLAYBACK_REFRESH_MARGIN_SEC,
        ): Boolean = !source.local && source.expiresAt - nowSec < marginSec
    }

/** Siguiente episodio tras terminar uno (auto-avance, FR-016). */
class NextEpisodeUseCase
    @Inject
    constructor(private val queue: QueueRepo) {
        suspend operator fun invoke(endedEpisodeId: Long): Long? {
            val items = queue.queue.value.items
            return queue.nextAfter(items, endedEpisodeId)
        }
    }

/** Descarta de "continuar escuchando" sin completar (FR-016). */
class DismissEpisodeUseCase
    @Inject
    constructor(private val progress: ProgressRepo) {
        suspend operator fun invoke(episodeId: Long) = progress.dismiss(episodeId)
    }
