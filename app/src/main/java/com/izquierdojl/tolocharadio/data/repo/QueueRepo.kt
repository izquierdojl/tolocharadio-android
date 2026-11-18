package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.AddToQueueBody
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastQueue
import com.izquierdojl.tolocharadio.data.remote.dto.ReorderQueueBody
import com.izquierdojl.tolocharadio.domain.podcasts.mergeQueueOrder
import com.izquierdojl.tolocharadio.domain.podcasts.nextQueueEpisodeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Posición de inserción en cola (contrato `end|next`). */
enum class QueuePosition(val apiValue: String) {
    NEXT("next"),
    END("end"),
}

/** Estado observado de la cola (FR-019–FR-022). */
data class QueueState(
    val revision: Long = 0,
    val items: List<PodcastEpisode> = emptyList(),
    val limit: Int = 200,
)

/**
 * Cola sincronizada por revisión optimista (research D6).
 * `409` recarga la versión del servidor y devuelve el conflicto
 * para avisar y reintentar (FR-022).
 */
@Singleton
class QueueRepo
    @Inject
    constructor(private val api: PodcastsApi) {
        private val _queue = MutableStateFlow(QueueState())
        val queue: StateFlow<QueueState> = _queue.asStateFlow()

        suspend fun load(): ApiResult<QueueState> {
            return when (val r = safeCall { api.queue() }) {
                is ApiResult.Ok -> {
                    publish(r.value.queue)
                    ApiResult.Ok(_queue.value)
                }
                is ApiResult.Err -> r
            }
        }

        suspend fun count(): ApiResult<Pair<Int, Int>> {
            return when (val r = safeCall { api.queueCount() }) {
                is ApiResult.Ok -> ApiResult.Ok(r.value.count to r.value.limit)
                is ApiResult.Err -> r
            }
        }

        /** Añade al final o a continuación (el servidor mueve si ya estaba). */
        suspend fun add(
            episodeId: Long,
            position: QueuePosition,
        ): ApiResult<QueueState> {
            return when (val r = safeCall { api.addToQueue(AddToQueueBody(episodeId, position.apiValue)) }) {
                is ApiResult.Ok -> {
                    publish(r.value.queue)
                    ApiResult.Ok(_queue.value)
                }
                is ApiResult.Err -> r
            }
        }

        suspend fun remove(episodeId: Long): ApiResult<QueueState> {
            return when (val r = safeCall { api.removeFromQueue(episodeId) }) {
                is ApiResult.Ok -> {
                    publish(r.value.queue)
                    ApiResult.Ok(_queue.value)
                }
                is ApiResult.Err -> r
            }
        }

        suspend fun clear(keepEpisodeId: Long? = null): ApiResult<QueueState> {
            return when (val r = safeCall { api.clearQueue(keepEpisodeId) }) {
                is ApiResult.Ok -> {
                    publish(r.value.queue)
                    ApiResult.Ok(_queue.value)
                }
                is ApiResult.Err -> r
            }
        }

        /**
         * Reordena con la revisión base del último GET, conservando el
         * hueco del episodio en curso (research D6).
         */
        suspend fun reorder(
            visibleOrder: List<Long>,
            hiddenEpisodeId: Long?,
        ): ApiResult<QueueState> {
            val current = _queue.value
            val full = mergeQueueOrder(current.items.map { it.id }, visibleOrder, hiddenEpisodeId)
            return when (val r = safeCall { api.reorderQueue(ReorderQueueBody(current.revision, full)) }) {
                is ApiResult.Ok -> {
                    publish(r.value.queue)
                    ApiResult.Ok(_queue.value)
                }
                is ApiResult.Err -> {
                    if (r.error is DomainError.Conflict) load()
                    r
                }
            }
        }

        /** Siguiente episodio tras terminar uno (auto-avance, FR-016). */
        fun nextAfter(
            items: List<PodcastEpisode>,
            endedEpisodeId: Long,
        ): Long? = nextQueueEpisodeId(items.map { it.id }, endedEpisodeId)

        private fun publish(queue: PodcastQueue) {
            _queue.value =
                QueueState(
                    revision = queue.revision,
                    items = queue.items.distinctBy { it.id },
                    limit = queue.limit,
                )
        }
    }
