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
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto

/**
 * Snapshot de emisoras personalizadas para lectura offline (FR-011). La
 * verdad es el servidor. Las personalizadas no tienen favicon propio:
 * la UI muestra siempre el emblema local, así que no se cachea imagen.
 */
@Entity(tableName = "custom_stations_cache")
data class CachedCustomStation(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val cachedAt: Long,
)

@Dao
abstract class CustomStationsCacheDao {
    @Query("SELECT * FROM custom_stations_cache ORDER BY rowid")
    abstract suspend fun loadAll(): List<CachedCustomStation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(items: List<CachedCustomStation>)

    @Query("DELETE FROM custom_stations_cache WHERE id = :id")
    abstract suspend fun deleteById(id: String)

    @Query("DELETE FROM custom_stations_cache")
    abstract suspend fun clear()

    /** Sustitución atómica tras un `GET` correcto. */
    @Transaction
    open suspend fun replaceAll(items: List<CachedCustomStation>) {
        clear()
        upsertAll(items)
    }
}

/** Migración v3 → v4: crea `custom_stations_cache` (caché de lectura). */
val MIGRATION_3_4: Migration =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `custom_stations_cache` (" +
                    "`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`streamUrl` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
        }
    }

/** Mapea emisoras personalizadas del servidor a caché. */
fun List<StationDto>.toCustomCached(now: Long): List<CachedCustomStation> =
    map { station ->
        CachedCustomStation(
            id = station.id,
            name = station.name,
            streamUrl = station.url,
            cachedAt = now,
        )
    }

/** Reconstruye emisoras personalizadas desde la caché (offline). */
fun List<CachedCustomStation>.toCustomStations(): List<StationDto> =
    map {
        StationDto(
            id = it.id,
            name = it.name,
            url = it.streamUrl,
            isCustom = true,
        )
    }

