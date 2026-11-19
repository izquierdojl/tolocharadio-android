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
import io.mockk.verify
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
            every { cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR) } returns -1
            every { cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES) } returns -1
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
            every { cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR) } returns -1
            every { cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES) } returns -1
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

    @Test
    fun `downloadProgress calcula la fraccion y tolera total desconocido`() {
        assertEquals(0.25f, DownloadProgress(11L, 50L, 200L).fraction)
        assertEquals(null, DownloadProgress(11L, 50L, null).fraction)
        assertEquals(null, DownloadProgress(11L, 50L, 0L).fraction)
    }

    @Test
    fun `download no duplica una transferencia running`() =
        runTest {
            val cursor: Cursor = mockk()
            every { cursor.moveToFirst() } returns true
            every { cursor.getColumnIndex(DownloadManager.COLUMN_STATUS) } returns 0
            every { cursor.getInt(0) } returns DownloadManager.STATUS_RUNNING
            every { cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR) } returns 1
            every { cursor.getLong(1) } returns 10L
            every { cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES) } returns 2
            every { cursor.getLong(2) } returns 100L
            every { cursor.close() } just runs
            every { manager.query(any()) } returns cursor
            coEvery { dao.loadAll() } returns
                listOf(EpisodeDownloadEntity(11L, 42L, DownloadStatus.RUNNING, null, 0))

            repo.refresh()
            val r = repo.download(episode)

            assertTrue(r is ApiResult.Ok)
            assertEquals(42L, (r as ApiResult.Ok).value)
            assertEquals(10L, repo.progress.value[11L]?.bytesDownloaded)
            coVerify(exactly = 0) { api.issueDownload(11L) }
        }

    @Test
    fun `cleanupExpired borra solo las descargas antiguas`() =
        runTest {
            val now = System.currentTimeMillis()
            val day = 24L * 60L * 60L * 1_000L
            val old =
                EpisodeDownloadEntity(11L, 42L, DownloadStatus.COMPLETE, "content://d/11", now - 31 * day)
            val recent =
                EpisodeDownloadEntity(12L, 43L, DownloadStatus.COMPLETE, "content://d/12", now)
            coEvery { dao.loadAll() } returns listOf(old, recent)
            every { manager.remove(42L) } returns 1
            coEvery { dao.deleteByEpisode(11L) } just runs

            repo.cleanupExpired(30)

            coVerify { dao.deleteByEpisode(11L) }
            coVerify(exactly = 0) { dao.deleteByEpisode(12L) }
            verify { manager.remove(42L) }
        }

    @Test
    fun `cleanupExpired respeta el episodio en curso y Nunca`() =
        runTest {
            val now = System.currentTimeMillis()
            val old =
                EpisodeDownloadEntity(
                    11L,
                    42L,
                    DownloadStatus.COMPLETE,
                    "content://d/11",
                    now - 31L * 24L * 60L * 60L * 1_000L,
                )
            coEvery { dao.loadAll() } returns listOf(old)

            repo.cleanupExpired(30, keepEpisodeId = 11L)
            repo.cleanupExpired(0)

            coVerify(exactly = 0) { dao.deleteByEpisode(11L) }
            verify(exactly = 0) { manager.remove(42L) }
        }
}
