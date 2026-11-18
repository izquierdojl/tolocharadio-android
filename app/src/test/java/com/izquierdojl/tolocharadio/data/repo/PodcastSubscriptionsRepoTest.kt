package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.api.BulkSubscribeBody
import com.izquierdojl.tolocharadio.data.remote.api.PodcastSearchPage
import com.izquierdojl.tolocharadio.data.remote.api.PodcastSubscriptionList
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.BulkSubscriptionResult
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class PodcastSubscriptionsRepoTest {
    private val api: PodcastsApi = mockk()
    private val repo = PodcastSubscriptionsRepo(api)

    private val show = PodcastSummary(7L, "https://a.example/f.xml", "Tolocha", isSubscribed = true)

    @Test
    fun `search devuelve resultados`() =
        runTest {
            val r1 = PodcastSearchResult("A", null, "https://a.example/f.xml")
            coEvery { api.search("tol", any()) } returns
                Response.success(PodcastSearchPage(listOf(r1)))
            val r = repo.search("tol")
            assertTrue(r is ApiResult.Ok)
            assertEquals("https://a.example/f.xml", (r as ApiResult.Ok).value.single().feedUrl)
        }

    @Test
    fun `search en blanco no llama a la API`() =
        runTest {
            val r = repo.search("   ")
            assertTrue((r as ApiResult.Err).error is DomainError.Validation)
        }

    @Test
    fun `resolve valida la url antes de red`() =
        runTest {
            val r = repo.resolveFeed("ftp://x")
            assertTrue((r as ApiResult.Err).error is DomainError.Validation)
        }

    @Test
    fun `unsubscribe 404 se reconcilia como exito`() =
        runTest {
            coEvery { api.unsubscribe(7L) } returns
                Response.error(404, """{"error":{"code":"NOT_FOUND","message":"x","status":404}}""".toResponseBody())
            val r = repo.unsubscribe(7L)
            assertTrue(r is ApiResult.Ok)
            assertTrue(7L !in repo.subscribedIds.value)
        }

    @Test
    fun `list publica suscripciones y ids`() =
        runTest {
            coEvery { api.subscriptions() } returns Response.success(PodcastSubscriptionList(listOf(show)))
            val r = repo.list()
            assertTrue(r is ApiResult.Ok)
            assertEquals(setOf(7L), repo.subscribedIds.value)
            assertEquals(listOf(show), repo.subscriptions.value)
        }

    @Test
    fun `bulkImport delega feedUrls y devuelve informe`() =
        runTest {
            val bulk = BulkSubscriptionResult(imported = 2, skipped = 1)
            coEvery { api.bulkSubscribe(BulkSubscribeBody(listOf("https://a.example/f.xml"))) } returns
                Response.success(bulk)
            val r = repo.bulkImport(listOf("https://a.example/f.xml"))
            assertTrue(r is ApiResult.Ok)
            assertEquals(2, (r as ApiResult.Ok).value.imported)
        }

    @Test
    fun `exportOpml devuelve el texto`() =
        runTest {
            coEvery { api.exportOpml() } returns Response.success("<opml></opml>".toResponseBody())
            val r = repo.exportOpml()
            assertTrue(r is ApiResult.Ok)
            assertEquals("<opml></opml>", (r as ApiResult.Ok).value)
        }
}
