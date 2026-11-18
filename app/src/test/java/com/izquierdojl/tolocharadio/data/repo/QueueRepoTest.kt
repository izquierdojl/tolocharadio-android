package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.api.PodcastQueueWrapper
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.AddToQueueBody
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastQueue
import com.izquierdojl.tolocharadio.data.remote.dto.ReorderQueueBody
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Cuerpo de error 409 para el conflicto de revisión de cola. */
private const val REVISION_CONFLICT_JSON =
    """{"error":{"code":"QUEUE_REVISION_MISMATCH","message":"obsoleta","status":409}}"""

class QueueRepoTest {
    private val api: PodcastsApi = mockk()
    private val repo = QueueRepo(api)

    private fun ep(id: Long) = PodcastEpisode(id, 7L, "E$id")

    private fun queueOf(
        revision: Long,
        vararg ids: Long,
    ) = PodcastQueue(revision = revision, limit = 200, items = ids.map { ep(it) })

    @Test
    fun `load publica revision e items`() =
        runTest {
            coEvery { api.queue() } returns Response.success(PodcastQueueWrapper(queueOf(4, 1, 2)))
            val r = repo.load()
            assertTrue(r is ApiResult.Ok)
            assertEquals(4L, repo.queue.value.revision)
            assertEquals(listOf(1L, 2L), repo.queue.value.items.map { it.id })
        }

    @Test
    fun `add delega posicion`() =
        runTest {
            coEvery { api.addToQueue(AddToQueueBody(9, "next")) } returns
                Response.success(PodcastQueueWrapper(queueOf(5, 9)))
            val r = repo.add(9, QueuePosition.NEXT)
            assertTrue(r is ApiResult.Ok)
            assertEquals(5L, (r as ApiResult.Ok).value.revision)
        }

    @Test
    fun `reorder envia revision base y orden total`() =
        runTest {
            coEvery { api.queue() } returns Response.success(PodcastQueueWrapper(queueOf(4, 1, 2, 3)))
            coEvery { api.reorderQueue(ReorderQueueBody(4, listOf(3L, 2L, 1L))) } returns
                Response.success(PodcastQueueWrapper(queueOf(5, 3, 2, 1)))
            repo.load()
            val r = repo.reorder(listOf(3L, 1L), hiddenEpisodeId = 2L)
            assertTrue(r is ApiResult.Ok)
            assertEquals(5L, (r as ApiResult.Ok).value.revision)
        }

    @Test
    fun `409 recarga y devuelve conflicto`() =
        runTest {
            coEvery { api.queue() } returns Response.success(PodcastQueueWrapper(queueOf(4, 1, 2)))
            coEvery { api.reorderQueue(any()) } returns
                Response.error(409, REVISION_CONFLICT_JSON.toResponseBody())
            repo.load()
            val r = repo.reorder(listOf(2L, 1L), hiddenEpisodeId = null)
            assertTrue(r is ApiResult.Err)
            assertTrue(
                (r as ApiResult.Err).error is com.izquierdojl.tolocharadio.core.network.DomainError.Conflict,
            )
        }

    @Test
    fun `nextAfter devuelve el siguiente distinto`() {
        assertEquals(2L, repo.nextAfter(listOf(ep(1), ep(2)), 1L))
    }
}
