package com.izquierdojl.tolocharadio.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto

/**
 * Snapshot de historial para lectura offline (FR-010). La verdad es el
 * servidor; una entrada por emisora (deduplicada), `playedAt` conserva
 * la reproducción más reciente. `cachedAt` en ms.
 */
@Entity(tableName = "history_cache")
data class CachedHistoryEntry(
    @PrimaryKey val id: String,
    val name: String,
    val favicon: String?,
    val country: String?,
    val language: String?,
    val tagsCsv: String,
    val playedAt: Long,
    val cachedAt: Long,
)

@Dao
abstract class HistoryCacheDao {
    @Query("SELECT * FROM history_cache ORDER BY playedAt DESC")
    abstract suspend fun loadOrdered(): List<CachedHistoryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(items: List<CachedHistoryEntry>)

    @Query("DELETE FROM history_cache WHERE id = :id")
    abstract suspend fun deleteById(id: String)

    @Query("DELETE FROM history_cache")
    abstract suspend fun clear()

    /** Sustitución atómica tras un `GET` correcto. */
    @Transaction
    open suspend fun replaceAll(items: List<CachedHistoryEntry>) {
        clear()
        upsertAll(items)
    }
}

/** Migración v2 → v3: crea `history_cache` (caché de lectura). */
val MIGRATION_2_3: Migration =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `history_cache` (" +
                    "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `favicon` TEXT, " +
                    "`country` TEXT, `language` TEXT, `tagsCsv` TEXT NOT NULL, " +
                    "`playedAt` INTEGER NOT NULL, `cachedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
        }
    }

/** Mapea entradas de historial del servidor a caché (deduplicadas). */
fun List<HistoryEntryDto>.toCached(now: Long): List<CachedHistoryEntry> =
    map { entry ->
        CachedHistoryEntry(
            id = entry.station.id,
            name = entry.station.name,
            favicon = entry.station.favicon,
            country = entry.station.country,
            language = entry.station.language,
            tagsCsv = entry.station.tags.joinToString(","),
            playedAt = entry.playedAt,
            cachedAt = now,
        )
    }

/** Reconstruye entradas de historial desde la caché (offline). */
fun List<CachedHistoryEntry>.toHistoryEntries(): List<HistoryEntryDto> =
    map {
        HistoryEntryDto(
            station =
                StationDto(
                    id = it.id,
                    name = it.name,
                    favicon = it.favicon,
                    country = it.country,
                    language = it.language,
                    tags = it.tagsCsv.split(",").filter(String::isNotBlank),
                ),
            playedAt = it.playedAt,
        )
    }
