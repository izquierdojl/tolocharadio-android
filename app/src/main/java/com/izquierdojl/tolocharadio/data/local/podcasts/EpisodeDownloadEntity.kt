package com.izquierdojl.tolocharadio.data.local.podcasts

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter

/** Estado de la transferencia gestionada por el DownloadManager del sistema. */
enum class DownloadStatus {
    RUNNING,
    COMPLETE,
    FAILED,
}

/**
 * Mapeo episodio→descarga (research D5). La reproducción prefiere el
 * `localUri` cuando el estado es COMPLETE y el fichero existe.
 * `updatedAt` en ms.
 */
@Entity(tableName = "episode_downloads")
data class EpisodeDownloadEntity(
    @PrimaryKey val episodeId: Long,
    val downloadManagerId: Long,
    val status: DownloadStatus,
    val localUri: String?,
    val updatedAt: Long,
)

/** Registro de dominio espejo de la entidad. */
data class DownloadRecord(
    val episodeId: Long,
    val downloadManagerId: Long,
    val status: DownloadStatus,
    val localUri: String?,
    val updatedAt: Long,
)

/** Entidad → registro de dominio. */
fun EpisodeDownloadEntity.toRecord(): DownloadRecord =
    DownloadRecord(
        episodeId = episodeId,
        downloadManagerId = downloadManagerId,
        status = status,
        localUri = localUri,
        updatedAt = updatedAt,
    )

/** Registro → entidad Room. */
fun DownloadRecord.toEntity(): EpisodeDownloadEntity =
    EpisodeDownloadEntity(
        episodeId = episodeId,
        downloadManagerId = downloadManagerId,
        status = status,
        localUri = localUri,
        updatedAt = updatedAt,
    )

@Dao
abstract class EpisodeDownloadDao {
    @Query("SELECT * FROM episode_downloads WHERE episodeId = :episodeId")
    abstract suspend fun byEpisode(episodeId: Long): EpisodeDownloadEntity?

    @Query("SELECT * FROM episode_downloads")
    abstract suspend fun loadAll(): List<EpisodeDownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(item: EpisodeDownloadEntity)

    @Query("DELETE FROM episode_downloads WHERE episodeId = :episodeId")
    abstract suspend fun deleteByEpisode(episodeId: Long)

    @Query("DELETE FROM episode_downloads")
    abstract suspend fun clear()
}

/** Room persiste el estado como nombre (legible en depuración). */
class DownloadStatusConverter {
    @TypeConverter
    fun toName(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(name: String): DownloadStatus {
        val status = runCatching { DownloadStatus.valueOf(name) }.getOrNull()
        return status ?: DownloadStatus.FAILED
    }
}
