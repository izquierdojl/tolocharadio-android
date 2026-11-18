package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.remote.api.BulkSubscribeBody
import com.izquierdojl.tolocharadio.data.remote.api.PodcastResolveResult
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.BulkSubscriptionResult
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.remote.dto.SubscribePodcastBody
import com.izquierdojl.tolocharadio.domain.podcasts.isValidFeedUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Suscripciones de podcast (spec 0046, US2). La verdad es el servidor;
 * los flujos exponen suscripciones e ids para el marcado coherente.
 */
@Singleton
class PodcastSubscriptionsRepo
    @Inject
    constructor(private val api: PodcastsApi) {
        private val _subscriptions = MutableStateFlow<List<PodcastSummary>>(emptyList())
        val subscriptions: StateFlow<List<PodcastSummary>> = _subscriptions.asStateFlow()

        private val _subscribedIds = MutableStateFlow<Set<Long>>(emptySet())
        val subscribedIds: StateFlow<Set<Long>> = _subscribedIds.asStateFlow()

        private val _subscribedFeedUrls = MutableStateFlow<Set<String>>(emptySet())
        val subscribedFeedUrls: StateFlow<Set<String>> = _subscribedFeedUrls.asStateFlow()

        /** Búsqueda en el directorio (`q` no vacío). */
        suspend fun search(
            query: String,
            limit: Int = 20,
        ): ApiResult<List<PodcastSearchResult>> {
            if (query.isBlank()) return ApiResult.Err(DomainError.Validation(emptyList()))
            return when (val r = safeCall { api.search(query.trim(), limit) }) {
                is ApiResult.Ok -> ApiResult.Ok(r.value.items)
                is ApiResult.Err -> r
            }
        }

        /** Alta manual: valida la URL antes de resolver el feed. */
        suspend fun resolveFeed(feedUrl: String): ApiResult<PodcastResolveResult> {
            if (!isValidFeedUrl(feedUrl)) return ApiResult.Err(DomainError.Validation(emptyList()))
            return safeCall { api.resolve(SubscribePodcastBody(feedUrl = feedUrl.trim())) }
        }

        /** Lista propia y publica flujos. */
        suspend fun list(): ApiResult<List<PodcastSummary>> {
            return when (val r = safeCall { api.subscriptions() }) {
                is ApiResult.Ok -> {
                    publish(r.value.items)
                    ApiResult.Ok(r.value.items)
                }
                is ApiResult.Err -> r
            }
        }

        /** Suscripción por programa (actualiza flujos y recarga la lista). */
        suspend fun subscribe(podcastId: Long): ApiResult<PodcastSummary> {
            return when (val r = safeCall { api.subscribe(SubscribePodcastBody(podcastId = podcastId)) }) {
                is ApiResult.Ok -> {
                    list()
                    ApiResult.Ok(r.value.podcast)
                }
                is ApiResult.Err -> r
            }
        }

        /** Suscripción por feed (results de directorio y altas manuales). */
        suspend fun subscribeFeed(feedUrl: String): ApiResult<PodcastSummary> {
            if (!isValidFeedUrl(feedUrl)) return ApiResult.Err(DomainError.Validation(emptyList()))
            return when (val r = safeCall { api.subscribe(SubscribePodcastBody(feedUrl = feedUrl.trim())) }) {
                is ApiResult.Ok -> {
                    list()
                    ApiResult.Ok(r.value.podcast)
                }
                is ApiResult.Err -> r
            }
        }

        /**
         * Baja idempotente: 404 se reconcilia como éxito (ya no está
         * suscrito) y el id sale de los flujos igualmente.
         */
        suspend fun unsubscribe(podcastId: Long): ApiResult<Unit> {
            return when (val r = safeCall { api.unsubscribe(podcastId) }) {
                is ApiResult.Ok -> {
                    drop(podcastId)
                    ApiResult.Ok(Unit)
                }
                is ApiResult.Err -> {
                    if (r.error is DomainError.NotFound) {
                        drop(podcastId)
                        ApiResult.Ok(Unit)
                    } else {
                        r
                    }
                }
            }
        }

        /** Importación en lote (b bulk del XML ya parseado). */
        suspend fun bulkImport(feedUrls: List<String>): ApiResult<BulkSubscriptionResult> {
            if (feedUrls.isEmpty()) return ApiResult.Err(DomainError.Validation(emptyList()))
            return when (val r = safeCall { api.bulkSubscribe(BulkSubscribeBody(feedUrls)) }) {
                is ApiResult.Ok -> {
                    list()
                    r
                }
                is ApiResult.Err -> r
            }
        }

        /** Exportación OPML 2.0 como texto. */
        suspend fun exportOpml(): ApiResult<String> {
            return when (val r = safeCall { api.exportOpml() }) {
                is ApiResult.Ok -> ApiResult.Ok(r.value.string())
                is ApiResult.Err -> r
            }
        }

        private fun publish(items: List<PodcastSummary>) {
            _subscriptions.value = items
            _subscribedIds.value = items.map { it.id }.toSet()
            _subscribedFeedUrls.value = items.map { it.feedUrl }.toSet()
        }

        private fun drop(podcastId: Long) {
            _subscribedIds.value = _subscribedIds.value - podcastId
            _subscriptions.value = _subscriptions.value.filterNot { it.id == podcastId }
            _subscribedFeedUrls.value = _subscriptions.value.map { it.feedUrl }.toSet()
        }
    }
