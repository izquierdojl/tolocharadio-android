package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.remote.api.PodcastProgressValue
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeProgress
import com.izquierdojl.tolocharadio.data.remote.dto.ReportEpisodeProgressBody
import javax.inject.Inject
import javax.inject.Singleton

/** Mapeo del progreso del contrato al DTO de dominio. */
fun PodcastProgressValue.toDto(): PodcastEpisodeProgress =
    PodcastEpisodeProgress(
        positionSec = positionSec,
        durationSec = durationSec,
        completed = completed,
        completedAt = completedAt,
        updatedAt = updatedAt,
    )

/** Origen del informe de progreso (contrato del servidor). */
private const val PROGRESS_SOURCE = "android"

/** Milisegundos por segundo. */
private const val MS_PER_SEC = 1_000L

/**
 * Progreso de escucha por episodio (spec 0046, US3).
 * Reporte idempotente `{sessionId,sequence,positionSec}` (PUT),
 * keepalive al fondo/cierre (POST) y descarte (FR-016).
 */
@Singleton
class ProgressRepo
    @Inject
    constructor(private val api: PodcastsApi) {
        /** Progreso guardado; sin reporte previo es null (404 reconciliado). */
        suspend fun load(episodeId: Long): ApiResult<PodcastEpisodeProgress?> {
            return when (val r = safeCall { api.progress(episodeId) }) {
                is ApiResult.Ok -> ApiResult.Ok(r.value.progress.toDto())
                is ApiResult.Err ->
                    if (r.error is DomainError.NotFound) {
                        ApiResult.Ok(null)
                    } else {
                        r
                    }
            }
        }

        /** Reporte periódico y en pausa/búsqueda/cierre (research D4). */
        suspend fun report(
            episodeId: Long,
            sessionId: String,
            sequence: Long,
            positionSec: Long,
            durationSec: Long? = null,
            listenedMs: Long = positionSec * MS_PER_SEC,
            completed: Boolean? = null,
        ): ApiResult<PodcastEpisodeProgress> {
            val body =
                ReportEpisodeProgressBody(
                    sessionId = sessionId,
                    sequence = sequence,
                    positionSec = positionSec,
                    listenedMs = listenedMs,
                    durationSec = durationSec,
                    completed = completed,
                    source = PROGRESS_SOURCE,
                )
            return when (val r = safeCall { api.reportProgress(episodeId, body) }) {
                is ApiResult.Ok -> ApiResult.Ok(r.value.progress.toDto())
                is ApiResult.Err -> r
            }
        }

        /** Keepalive de fondo/cierre (equivalente al `sendBeacon` web). */
        suspend fun keepalive(
            episodeId: Long,
            sessionId: String,
            sequence: Long,
            positionSec: Long,
            durationSec: Long? = null,
            listenedMs: Long = positionSec * MS_PER_SEC,
        ): ApiResult<Unit> {
            val body =
                ReportEpisodeProgressBody(
                    sessionId = sessionId,
                    sequence = sequence,
                    positionSec = positionSec,
                    listenedMs = listenedMs,
                    durationSec = durationSec,
                    source = PROGRESS_SOURCE,
                )
            return when (val r = safeCall { api.keepaliveProgress(episodeId, body) }) {
                is ApiResult.Ok -> ApiResult.Ok(Unit)
                is ApiResult.Err -> r
            }
        }

        /** Descarta de "continuar escuchando" sin marcar completado (FR-016). */
        suspend fun dismiss(episodeId: Long): ApiResult<Boolean> {
            return when (val r = safeCall { api.dismiss(episodeId) }) {
                is ApiResult.Ok -> ApiResult.Ok(r.value.dismissed)
                is ApiResult.Err -> r
            }
        }
    }
