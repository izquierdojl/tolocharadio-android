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
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto

/**
 * Snapshot de favorita para lectura offline (FR-010). La verdad es el
 * servidor; `sortIndex` conserva el orden personalizado. `cachedAt` en ms.
 */
@Entity(tableName = "favorites_cache")
data class CachedFavorite(
    @PrimaryKey val id: String,
    val name: String,
    val favicon: String?,
    val country: String?,
    val language: String?,
    val tagsCsv: String,
    val addedAt: Long,
    val sortIndex: Int,
    val cachedAt: Long,
)

@Dao
abstract class FavoritesCacheDao {
    @Query("SELECT * FROM favorites_cache ORDER BY sortIndex ASC")
    abstract suspend fun loadOrdered(): List<CachedFavorite>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(items: List<CachedFavorite>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(item: CachedFavorite)

    @Query("DELETE FROM favorites_cache WHERE id = :id")
    abstract suspend fun deleteById(id: String)

    @Query("SELECT COALESCE(MAX(sortIndex), -1) + 1 FROM favorites_cache")
    abstract suspend fun nextSortIndex(): Int

    @Query("DELETE FROM favorites_cache")
    abstract suspend fun clear()

    /** Sustitución atómica tras un `GET` correcto. */
    @Transaction
    open suspend fun replaceAll(items: List<CachedFavorite>) {
        clear()
        upsertAll(items)
    }
}

/** Migración v1 → v2: crea `favorites_cache` (caché de lectura, sin datos previos). */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `favorites_cache` (" +
                    "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `favicon` TEXT, " +
                    "`country` TEXT, `language` TEXT, `tagsCsv` TEXT NOT NULL, " +
                    "`addedAt` INTEGER NOT NULL, `sortIndex` INTEGER NOT NULL, " +
                    "`cachedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

/** Mapea favoritas del servidor a caché con `sortIndex` continuo (VR-05). */
fun List<FavoriteDto>.toCached(now: Long): List<CachedFavorite> =
    mapIndexed { index, fav ->
        CachedFavorite(
            id = fav.station.id,
            name = fav.station.name,
            favicon = fav.station.favicon,
            country = fav.station.country,
            language = fav.station.language,
            tagsCsv = fav.station.tags.joinToString(","),
            addedAt = fav.addedAt,
            sortIndex = index,
            cachedAt = now,
        )
    }

/** Reconstruye favoritas desde la caché (offline). */
fun List<CachedFavorite>.toFavorites(): List<FavoriteDto> =
    map {
        FavoriteDto(
            station =
                StationDto(
                    id = it.id,
                    name = it.name,
                    favicon = it.favicon,
                    country = it.country,
                    language = it.language,
                    tags = it.tagsCsv.split(",").filter(String::isNotBlank),
                ),
            addedAt = it.addedAt,
        )
    }

