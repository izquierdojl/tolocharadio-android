package com.izquierdojl.tolocharadio.data.repo

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.local.TolochaDb
import com.izquierdojl.tolocharadio.data.local.podcasts.DownloadRecord
import com.izquierdojl.tolocharadio.data.local.podcasts.DownloadStatus
import com.izquierdojl.tolocharadio.data.local.podcasts.toEntity
import com.izquierdojl.tolocharadio.data.local.podcasts.toRecord
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.di.ApplicationScope
import com.izquierdojl.tolocharadio.domain.podcasts.absoluteUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Intervalo de sondeo del DownloadManager mientras hay descargas activas. */
private const val DOWNLOAD_POLL_MS = 2_000L

/** Sondeos máximos por descarga (30 min); después queda el estado del sistema. */
private const val DOWNLOAD_MAX_POLLS = 900

/** Estado del sistema → estado de dominio (research D5). */
fun mapDownloadStatus(systemStatus: Int): DownloadStatus =
    when (systemStatus) {
        DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETE
        DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
        else -> DownloadStatus.RUNNING
    }

/**
 * Descargas internas en Descargas del sistema (spec 0046, US7).
 * La transferencia la ejecuta el DownloadManager con la URL firmada
 * (sin auth); Room mapea episodio→descarga para la reproducción local.
 */
@Singleton
class DownloadsRepo
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val manager: DownloadManager,
        private val api: PodcastsApi,
        private val db: TolochaDb,
        private val prefs: InstancePrefs,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        private val _records = MutableStateFlow<Map<Long, DownloadRecord>>(emptyMap())
        val records: StateFlow<Map<Long, DownloadRecord>> = _records.asStateFlow()

        /** Sincroniza estados del sistema y recarga el mapeo episodio→descarga. */
        suspend fun refresh() {
            syncRunning()
            _records.value = db.episodeDownloads().loadAll().map { it.toRecord() }.associateBy { it.episodeId }
        }

        /**
         * Emite la URL firmada y encola la transferencia en Descargas.
         * Devuelve el id del sistema para seguimiento.
         */
        suspend fun download(episode: PodcastEpisode): ApiResult<Long> {
            val issued =
                when (val r = safeCall { api.issueDownload(episode.id) }) {
                    is ApiResult.Ok -> r.value
                    is ApiResult.Err -> return r
                }
            val absolute = absoluteUrl(prefs.baseUrl.first(), issued.url)
            val request =
                DownloadManager.Request(Uri.parse(absolute)).apply {
                    // VISIBILITY_VISIBLE (sin notificación): VISIBILITY_*_NOTIFY_*
                    // exige POST_NOTIFICATIONS en API 33+ y reventaba la descarga.
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    setTitle(episode.title)
                }
            val dmId = manager.enqueue(request)
            val record =
                DownloadRecord(
                    episodeId = episode.id,
                    downloadManagerId = dmId,
                    status = DownloadStatus.RUNNING,
                    localUri = null,
                    updatedAt = System.currentTimeMillis(),
                )
            db.episodeDownloads().upsert(record.toEntity())
            refresh()
            monitor(dmId, episode.id)
            return ApiResult.Ok(dmId)
        }

        /**
         * URI local cuando la descarga está completa y el fichero existe;
         * actualiza Room al consultar el estado del sistema. Null en otro caso.
         */
        suspend fun localUriFor(episodeId: Long): String? {
            val stored = db.episodeDownloads().byEpisode(episodeId) ?: return null
            val (status, uri) = queryStatus(stored.downloadManagerId) ?: return null
            return when (status) {
                DownloadStatus.COMPLETE -> {
                    val local = uri ?: return null
                    if (!readable(local)) {
                        db.episodeDownloads().upsert(stored.copy(status = DownloadStatus.FAILED).toRecord().toEntity())
                        refresh()
                        return null
                    }
                    if (stored.status != DownloadStatus.COMPLETE || stored.localUri != local) {
                        val updated = stored.copy(status = DownloadStatus.COMPLETE, localUri = local)
                        db.episodeDownloads().upsert(updated.toRecord().toEntity())
                        refresh()
                    }
                    local
                }
                DownloadStatus.FAILED -> {
                    if (stored.status != DownloadStatus.FAILED) {
                        db.episodeDownloads().upsert(stored.copy(status = DownloadStatus.FAILED).toRecord().toEntity())
                        refresh()
                    }
                    null
                }
                DownloadStatus.RUNNING -> null
            }
        }

        /** Borra el registro (y la transferencia del sistema si sigue activa). */
        suspend fun delete(episodeId: Long) {
            db.episodeDownloads().byEpisode(episodeId)?.let { manager.remove(it.downloadManagerId) }
            db.episodeDownloads().deleteByEpisode(episodeId)
            refresh()
        }

        private fun queryStatus(dmId: Long): Pair<DownloadStatus, String?>? {
            val cursor = manager.query(DownloadManager.Query().setFilterById(dmId)) ?: return null
            return cursor.use {
                if (!it.moveToFirst()) return null
                val column = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (column == -1) return null
                val status = mapDownloadStatus(it.getInt(column))
                val uri =
                    if (status == DownloadStatus.COMPLETE) {
                        manager.getUriForDownloadedFile(dmId)?.toString()
                    } else {
                        null
                    }
                status to uri
            }
        }

        /** Sigue una descarga recién encolada hasta que termina o falla. */
        private fun monitor(
            dmId: Long,
            episodeId: Long,
        ) {
            scope.launch {
                repeat(DOWNLOAD_MAX_POLLS) {
                    delay(DOWNLOAD_POLL_MS)
                    val stored = db.episodeDownloads().byEpisode(episodeId) ?: return@launch
                    val (status, uri) = queryStatus(dmId) ?: return@launch
                    if (status != DownloadStatus.RUNNING) {
                        db.episodeDownloads().upsert(
                            stored.copy(
                                status = status,
                                localUri = uri,
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                        refresh()
                        return@launch
                    }
                }
            }
        }

        /** Actualiza en Room las descargas que terminaron con la app cerrada. */
        private suspend fun syncRunning() {
            val stored = db.episodeDownloads().loadAll()
            stored.filter { it.status == DownloadStatus.RUNNING }.forEach { entity ->
                val result = queryStatus(entity.downloadManagerId) ?: return@forEach
                val (status, uri) = result
                if (status != DownloadStatus.RUNNING) {
                    db.episodeDownloads().upsert(
                        entity.copy(
                            status = status,
                            localUri = uri,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }

        private fun readable(uri: String): Boolean {
            return runCatching {
                context.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.close()
                true
            }.getOrDefault(false)
        }
    }
