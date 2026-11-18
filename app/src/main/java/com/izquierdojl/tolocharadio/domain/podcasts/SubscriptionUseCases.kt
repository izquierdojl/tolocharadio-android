package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.BulkSubscriptionResult
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.PodcastSubscriptionsRepo
import javax.inject.Inject

/** Suscripción por programa o feed (US2). */
class SubscribePodcastUseCase
    @Inject
    constructor(private val repo: PodcastSubscriptionsRepo) {
        suspend operator fun invoke(podcastId: Long): ApiResult<PodcastSummary> = repo.subscribe(podcastId)

        suspend fun byFeedUrl(feedUrl: String): ApiResult<PodcastSummary> = repo.subscribeFeed(feedUrl)
    }

/** Baja de suscripción, idempotente (US2). */
class UnsubscribePodcastUseCase
    @Inject
    constructor(private val repo: PodcastSubscriptionsRepo) {
        suspend operator fun invoke(podcastId: Long): ApiResult<Unit> = repo.unsubscribe(podcastId)
    }

/** Importación OPML: parsea, valida y envía en lote (US2, FR-010). */
class ImportOpmlUseCase
    @Inject
    constructor(private val repo: PodcastSubscriptionsRepo) {
        suspend operator fun invoke(xml: String): ApiResult<BulkSubscriptionResult> {
            val urls = parseOpmlFeedUrls(xml).filter { isValidFeedUrl(it) }
            if (urls.isEmpty()) return ApiResult.Err(DomainError.Validation(emptyList()))
            return repo.bulkImport(urls)
        }
    }
