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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/** Intervalo de sondeo del DownloadManager mientras hay descargas activas. */
private const val DOWNLOAD_POLL_MS = 1_000L

/** Sondeos máximos por descarga (30 min); después queda el estado del sistema. */
private const val DOWNLOAD_MAX_POLLS = 1_800

/** Milisegundos de un día para la retención. */
private const val DAY_MS = 24L * 60L * 60L * 1_000L

/** Progreso efímero de una descarga (no persiste; el sistema es el dueño). */
data class DownloadProgress(
    val episodeId: Long,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
) {
    /** Fracción 0..1, o null si el tamaño total no se conoce. */
    val fraction: Float?
        get() =
            totalBytes
                ?.takeIf { it > 0 }
                ?.let { total ->
                    (bytesDownloaded.toDouble() / total).toFloat().coerceIn(0f, 1f)
                }
}

/** Consulta interna del estado del sistema. */
private data class DownloadQuery(
    val status: DownloadStatus,
    val uri: String?,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
)

/** Estado del sistema → estado de dominio (research D5). */
fun mapDownloadStatus(systemStatus: Int): DownloadStatus =
    when (systemStatus) {
        DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETE
        DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
        else -> DownloadStatus.RUNNING
    }

/**
 * Descargas internas en Descargas del sistema (spec 0046, US7) con
 * progreso efímero para la UI (spec 0047, US6).
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

        private val _progress = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
        val progress: StateFlow<Map<Long, DownloadProgress>> = _progress.asStateFlow()

        private val monitoring: MutableSet<Long> = Collections.synchronizedSet(mutableSetOf())

        /** Sincroniza estados del sistema y relanza el monitor de las activas. */
        suspend fun refresh() {
            syncRunning()
            _records.value = db.episodeDownloads().loadAll().map { it.toRecord() }.associateBy { it.episodeId }
        }

        /**
         * Emite la URL firmada y encola la transferencia en Descargas.
         * Devuelve el id del sistema para seguimiento.
         */
        suspend fun download(episode: PodcastEpisode): ApiResult<Long> {
            val existing = _records.value[episode.id]
            if (existing?.status == DownloadStatus.RUNNING) {
                val query = queryStatus(existing.downloadManagerId)
                if (query?.status == DownloadStatus.RUNNING) {
                    return ApiResult.Ok(existing.downloadManagerId)
                }
            }
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
            _progress.update { it + (episode.id to DownloadProgress(episode.id, 0L, null)) }
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
            val query = queryStatus(stored.downloadManagerId) ?: return null
            return when (query.status) {
                DownloadStatus.COMPLETE -> {
                    val local = query.uri ?: return null
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
            db.episodeDownloads().byEpisode(episodeId)?.let {
                manager.remove(it.downloadManagerId)
                monitoring.remove(it.downloadManagerId)
            }
            db.episodeDownloads().deleteByEpisode(episodeId)
            _progress.update { it - episodeId }
            refresh()
        }

        /**
         * Borra las descargas completadas más antiguas que [retentionDays]
         * (0 = nunca), conservando el episodio en curso si se indica.
         */
        suspend fun cleanupExpired(
            retentionDays: Int,
            keepEpisodeId: Long? = null,
        ) {
            if (retentionDays <= 0) return
            val cutoff = System.currentTimeMillis() - retentionDays * DAY_MS
            val expired =
                db.episodeDownloads().loadAll().filter {
                    it.status == DownloadStatus.COMPLETE &&
                        it.updatedAt < cutoff &&
                        it.episodeId != keepEpisodeId
                }
            if (expired.isEmpty()) return
            expired.forEach {
                manager.remove(it.downloadManagerId)
                db.episodeDownloads().deleteByEpisode(it.episodeId)
                _progress.update { progress -> progress - it.episodeId }
            }
            refresh()
        }

        private fun queryStatus(dmId: Long): DownloadQuery? {
            val cursor = manager.query(DownloadManager.Query().setFilterById(dmId)) ?: return null
            return cursor.use {
                if (!it.moveToFirst()) return null
                val statusColumn = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusColumn == -1) return null
                val status = mapDownloadStatus(it.getInt(statusColumn))
                val bytes = it.longColumn(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR).coerceAtLeast(0L)
                val total = it.longColumn(DownloadManager.COLUMN_TOTAL_SIZE_BYTES).takeIf { value -> value > 0L }
                val uri =
                    if (status == DownloadStatus.COMPLETE) {
                        manager.getUriForDownloadedFile(dmId)?.toString()
                    } else {
                        null
                    }
                DownloadQuery(status, uri, bytes, total)
            }
        }

        /** Sigue una descarga hasta que termina o falla, publicando el progreso. */
        private fun monitor(
            dmId: Long,
            episodeId: Long,
        ) {
            if (!monitoring.add(dmId)) return
            scope.launch {
                try {
                    repeat(DOWNLOAD_MAX_POLLS) {
                        delay(DOWNLOAD_POLL_MS)
                        val stored = db.episodeDownloads().byEpisode(episodeId) ?: return@launch
                        val query = queryStatus(dmId) ?: return@launch
                        if (query.status == DownloadStatus.RUNNING) {
                            _progress.update {
                                it + (episodeId to DownloadProgress(episodeId, query.bytesDownloaded, query.totalBytes))
                            }
                        } else {
                            _progress.update { it - episodeId }
                            db.episodeDownloads().upsert(
                                stored.copy(
                                    status = query.status,
                                    localUri = query.uri,
                                    updatedAt = System.currentTimeMillis(),
                                ),
                            )
                            refresh()
                            return@launch
                        }
                    }
                } finally {
                    monitoring.remove(dmId)
                }
            }
        }

        /** Reanuda el monitor de descargas que seguían activas al cerrar la app. */
        private suspend fun syncRunning() {
            val stored = db.episodeDownloads().loadAll()
            stored.filter { it.status == DownloadStatus.RUNNING }.forEach { entity ->
                val query = queryStatus(entity.downloadManagerId) ?: return@forEach
                if (query.status != DownloadStatus.RUNNING) {
                    db.episodeDownloads().upsert(
                        entity.copy(
                            status = query.status,
                            localUri = query.uri,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                } else {
                    _progress.update {
                        it +
                            (
                                entity.episodeId to
                                    DownloadProgress(entity.episodeId, query.bytesDownloaded, query.totalBytes)
                            )
                    }
                    monitor(entity.downloadManagerId, entity.episodeId)
                }
            }
        }

        private fun android.database.Cursor.longColumn(name: String): Long {
            val index = getColumnIndex(name)
            return if (index == -1) 0L else getLong(index)
        }

        private fun readable(uri: String): Boolean {
            return runCatching {
                context.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.close()
                true
            }.getOrDefault(false)
        }
    }
