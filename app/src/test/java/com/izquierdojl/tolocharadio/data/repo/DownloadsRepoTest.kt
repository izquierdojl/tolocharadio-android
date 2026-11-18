package com.izquierdojl.tolocharadio.data.repo

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.local.TolochaDb
import com.izquierdojl.tolocharadio.data.local.podcasts.DownloadStatus
import com.izquierdojl.tolocharadio.data.local.podcasts.EpisodeDownloadDao
import com.izquierdojl.tolocharadio.data.local.podcasts.EpisodeDownloadEntity
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.IssuedToken
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Descargas: encolar, preferencia local y mapeo de estados DM (T050, Red→Green). */
class DownloadsRepoTest {
    private val context: Context = mockk(relaxed = true)
    private val manager: DownloadManager = mockk()
    private val dao: EpisodeDownloadDao = mockk(relaxed = true)
    private val db: TolochaDb = mockk { every { episodeDownloads() } returns dao }
    private val api: PodcastsApi = mockk()
    private val prefs: InstancePrefs =
        mockk {
            every { baseUrl } returns kotlinx.coroutines.flow.MutableStateFlow("https://srv.example")
        }
    private val repo =
        DownloadsRepo(
            context,
            manager,
            api,
            db,
            prefs,
            // Sin monitor real: el sondeo podría despertar tras terminar el test.
            CoroutineScope(Job().apply { cancel() }),
        )

    private val episode = PodcastEpisode(11L, 7L, "Episodio")

    @Test
    fun `download emite firmada y encola en el sistema`() =
        runTest {
            coEvery { api.issueDownload(11L) } returns
                Response.success(IssuedToken("https://cdn.example/e.mp3?sig=x", 9))
            every { manager.enqueue(any()) } returns 42L
            val r = repo.download(episode)
            assertTrue(r is ApiResult.Ok)
            assertEquals(42L, (r as ApiResult.Ok).value)
            coVerify { dao.upsert(match { it.episodeId == 11L && it.downloadManagerId == 42L }) }
        }

    @Test
    fun `localUri prefiere el fichero completo`() =
        runTest {
            val cursor: Cursor = mockk()
            every { cursor.moveToFirst() } returns true
            every { cursor.getColumnIndex(DownloadManager.COLUMN_STATUS) } returns 0
            every { cursor.getInt(0) } returns DownloadManager.STATUS_SUCCESSFUL
            every { cursor.close() } just runs
            every { manager.query(any()) } returns cursor
            val fakeUri = mockk<Uri>()
            every { fakeUri.toString() } returns "content://descargas/11"
            every { manager.getUriForDownloadedFile(42L) } returns fakeUri
            coEvery { dao.byEpisode(11L) } returns
                EpisodeDownloadEntity(11L, 42L, DownloadStatus.RUNNING, null, 0)
            val uri = repo.localUriFor(11L)
            assertEquals("content://descargas/11", uri)
        }

    @Test
    fun `url firmada relativa no revienta el enqueue`() =
        runTest {
            coEvery { api.issueDownload(12L) } returns
                Response.success(IssuedToken("/api/v1/podcasts/episodes/12/download?sig=x", 9))
            every { manager.enqueue(any()) } returns 43L
            val r = repo.download(PodcastEpisode(12L, 7L, "E12"))
            assertTrue(r is ApiResult.Ok)
            assertEquals(43L, (r as ApiResult.Ok).value)
        }

    @Test
    fun `refresh sincroniza descargas terminadas con la app cerrada`() =
        runTest {
            val cursor: Cursor = mockk()
            every { cursor.moveToFirst() } returns true
            every { cursor.getColumnIndex(DownloadManager.COLUMN_STATUS) } returns 0
            every { cursor.getInt(0) } returns DownloadManager.STATUS_SUCCESSFUL
            every { cursor.close() } just runs
            every { manager.query(any()) } returns cursor
            val fakeUri = mockk<Uri>()
            every { fakeUri.toString() } returns "content://descargas/11"
            every { manager.getUriForDownloadedFile(42L) } returns fakeUri
            val running = EpisodeDownloadEntity(11L, 42L, DownloadStatus.RUNNING, null, 0)
            val complete =
                running.copy(status = DownloadStatus.COMPLETE, localUri = "content://descargas/11")
            coEvery { dao.loadAll() } returnsMany listOf(listOf(running), listOf(complete))
            repo.refresh()
            assertEquals(DownloadStatus.COMPLETE, repo.records.value[11L]?.status)
            coVerify {
                dao.upsert(match { it.episodeId == 11L && it.status == DownloadStatus.COMPLETE })
            }
        }

    @Test
    fun `mapDownloadStatus traduce estados del sistema`() {
        assertEquals(DownloadStatus.COMPLETE, mapDownloadStatus(DownloadManager.STATUS_SUCCESSFUL))
        assertEquals(DownloadStatus.FAILED, mapDownloadStatus(DownloadManager.STATUS_FAILED))
        assertEquals(DownloadStatus.RUNNING, mapDownloadStatus(DownloadManager.STATUS_RUNNING))
    }
}
