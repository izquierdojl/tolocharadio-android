package com.izquierdojl.tolocharadio.data.local.podcasts

import org.junit.Assert.assertEquals
import org.junit.Test

/** Mapeo entidad↔registro de descargas (T023, Red; la DAO replica FavoritesCacheDao). */
class EpisodeDownloadMappingTest {
    @Test
    fun `toRecord conserva estado y uri`() {
        val e =
            EpisodeDownloadEntity(
                episodeId = 11L,
                downloadManagerId = 99L,
                status = DownloadStatus.COMPLETE,
                localUri = "content://descargas/11",
                updatedAt = 5L,
            )
        val r = e.toRecord()
        assertEquals(11L, r.episodeId)
        assertEquals(DownloadStatus.COMPLETE, r.status)
        assertEquals("content://descargas/11", r.localUri)
    }

    @Test
    fun `fromRecord redondea el mapeo`() {
        val r = DownloadRecord(7L, 3L, DownloadStatus.RUNNING, null, 1L)
        assertEquals(r, r.toEntity().toRecord())
    }
}
