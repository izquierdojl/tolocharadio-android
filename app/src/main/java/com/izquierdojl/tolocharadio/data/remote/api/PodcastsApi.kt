package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.AddToQueueBody
import com.izquierdojl.tolocharadio.data.remote.dto.BulkSubscriptionResult
import com.izquierdojl.tolocharadio.data.remote.dto.IssuedToken
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastCategoryEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodePage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeSearchPage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastHabitEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastHome
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastQueue
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastRecentEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastStatsSummaryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTopEpisodeDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTopPodcastDto
import com.izquierdojl.tolocharadio.data.remote.dto.ReorderQueueBody
import com.izquierdojl.tolocharadio.data.remote.dto.ReportEpisodeProgressBody
import com.izquierdojl.tolocharadio.data.remote.dto.SubscribePodcastBody
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class PodcastWrapper(val podcast: PodcastSummary)

@Serializable
data class PodcastResolveResult(
    val podcast: PodcastSummary,
    val episodes: PodcastEpisodePage,
)

@Serializable
data class PodcastQueueWrapper(val queue: PodcastQueue)

@Serializable
data class PodcastQueueCount(val count: Int = 0, val limit: Int = 200)

@Serializable
data class PodcastEpisodeWrapper(val episode: PodcastEpisode)

@Serializable
data class PodcastFavoriteList(val items: List<PodcastEpisode> = emptyList())

@Serializable
data class PodcastSearchPage(val items: List<PodcastSearchResult> = emptyList())

@Serializable
data class PodcastProgressWrapper(val progress: PodcastProgressValue)

@Serializable
data class PodcastProgressValue(
    val positionSec: Long = 0,
    val durationSec: Long? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val updatedAt: Long = 0,
)

@Serializable
data class PodcastDismissResult(val dismissed: Boolean = false)

@Serializable
data class PodcastTopPodcasts(val items: List<PodcastTopPodcastDto> = emptyList())

@Serializable
data class PodcastTopEpisodes(val items: List<PodcastTopEpisodeDto> = emptyList())

@Serializable
data class PodcastHabits(val items: List<PodcastHabitEntryDto> = emptyList())

@Serializable
data class PodcastCategories(val items: List<PodcastCategoryEntryDto> = emptyList())

@Serializable
data class PodcastRecents(val items: List<PodcastRecentEntryDto> = emptyList())

/**
 * Gestión completa de podcast (paridad `apps/web/src/lib/api.ts`).
 * Todo exige sesión salvo que se indique; errores al dominio vía `safeCall`.
 */
@Suppress("TooManyFunctions")
interface PodcastsApi {
    @GET("podcasts/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("limit") limit: Int = 20,
    ): Response<PodcastSearchPage>

    @GET("podcasts/episodes/search")
    suspend fun searchEpisodes(
        @Query("q") q: String,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20,
    ): Response<PodcastEpisodeSearchPage>

    @POST("podcasts/resolve")
    suspend fun resolve(
        @Body body: SubscribePodcastBody,
    ): Response<PodcastResolveResult>

    @GET("podcasts/subscriptions")
    suspend fun subscriptions(): Response<PodcastSubscriptionList>

    @POST("podcasts/subscriptions")
    suspend fun subscribe(
        @Body body: SubscribePodcastBody,
    ): Response<PodcastWrapper>

    @POST("podcasts/subscriptions/bulk")
    suspend fun bulkSubscribe(
        @Body body: BulkSubscribeBody,
    ): Response<BulkSubscriptionResult>

    @GET("podcasts/subscriptions/opml")
    suspend fun exportOpml(): Response<ResponseBody>

    @DELETE("podcasts/subscriptions/{podcastId}")
    suspend fun unsubscribe(
        @Path("podcastId") podcastId: Long,
    ): Response<OkResult>

    @GET("podcasts/home")
    suspend fun home(): Response<PodcastHome>

    @GET("podcasts/{podcastId}")
    suspend fun detail(
        @Path("podcastId") podcastId: Long,
    ): Response<PodcastWrapper>

    @GET("podcasts/{podcastId}/episodes")
    suspend fun episodes(
        @Path("podcastId") podcastId: Long,
        @Query("sort") sort: String? = null,
        @Query("filter") filter: String? = null,
        @Query("q") q: String? = null,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 24,
    ): Response<PodcastEpisodePage>

    @GET("podcasts/queue")
    suspend fun queue(): Response<PodcastQueueWrapper>

    @GET("podcasts/queue/count")
    suspend fun queueCount(): Response<PodcastQueueCount>

    @POST("podcasts/queue")
    suspend fun addToQueue(
        @Body body: AddToQueueBody,
    ): Response<PodcastQueueWrapper>

    @PUT("podcasts/queue/order")
    suspend fun reorderQueue(
        @Body body: ReorderQueueBody,
    ): Response<PodcastQueueWrapper>

    @DELETE("podcasts/queue")
    suspend fun clearQueue(
        @Query("keepEpisodeId") keepEpisodeId: Long? = null,
    ): Response<PodcastQueueWrapper>

    @DELETE("podcasts/queue/{episodeId}")
    suspend fun removeFromQueue(
        @Path("episodeId") episodeId: Long,
    ): Response<PodcastQueueWrapper>

    @GET("podcasts/favorites")
    suspend fun favorites(): Response<PodcastFavoriteList>

    @POST("podcasts/favorites")
    suspend fun addFavorite(
        @Body body: EpisodeIdBody,
    ): Response<PodcastEpisodeWrapper>

    @DELETE("podcasts/favorites/{episodeId}")
    suspend fun removeFavorite(
        @Path("episodeId") episodeId: Long,
    ): Response<OkResult>

    @GET("podcasts/episodes/{episodeId}/progress")
    suspend fun progress(
        @Path("episodeId") episodeId: Long,
    ): Response<PodcastProgressWrapper>

    @PUT("podcasts/episodes/{episodeId}/progress")
    suspend fun reportProgress(
        @Path("episodeId") episodeId: Long,
        @Body body: ReportEpisodeProgressBody,
    ): Response<PodcastProgressWrapper>

    @POST("podcasts/episodes/{episodeId}/progress")
    suspend fun keepaliveProgress(
        @Path("episodeId") episodeId: Long,
        @Body body: ReportEpisodeProgressBody,
    ): Response<PodcastProgressWrapper>

    @POST("podcasts/episodes/{episodeId}/dismiss")
    suspend fun dismiss(
        @Path("episodeId") episodeId: Long,
    ): Response<PodcastDismissResult>

    @POST("podcasts/episodes/{episodeId}/playback")
    suspend fun issuePlayback(
        @Path("episodeId") episodeId: Long,
    ): Response<IssuedToken>

    @POST("podcasts/episodes/{episodeId}/download")
    suspend fun issueDownload(
        @Path("episodeId") episodeId: Long,
    ): Response<IssuedToken>

    @GET("podcasts/stats/summary")
    suspend fun statsSummary(): Response<PodcastStatsSummaryDto>

    @GET("podcasts/stats/top-podcasts")
    suspend fun topPodcasts(
        @Query("limit") limit: Int? = null,
    ): Response<PodcastTopPodcasts>

    @GET("podcasts/stats/top-episodes")
    suspend fun topEpisodes(
        @Query("limit") limit: Int? = null,
    ): Response<PodcastTopEpisodes>

    @GET("podcasts/stats/timeline")
    suspend fun timeline(
        @Query("granularity") granularity: String? = null,
    ): Response<PodcastTimelineDto>

    @GET("podcasts/stats/habits")
    suspend fun habits(): Response<PodcastHabits>

    @GET("podcasts/stats/categories")
    suspend fun categories(
        @Query("limit") limit: Int? = null,
    ): Response<PodcastCategories>

    @GET("podcasts/stats/recent")
    suspend fun recent(
        @Query("limit") limit: Int? = null,
    ): Response<PodcastRecents>
}

@Serializable
data class PodcastSubscriptionList(val items: List<PodcastSummary> = emptyList())

@Serializable
data class BulkSubscribeBody(val feedUrls: List<String>)

@Serializable
data class EpisodeIdBody(val episodeId: Long)
