package com.example.tolocharadio.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Snapshot de emisora para lectura offline cuando RadioBrowser
 * devuelve 503 (spec US-4). `cachedAt` en ms.
 */
@Entity(tableName = "stations_cache")
data class CachedStation(
    @PrimaryKey val id: String,
    val name: String,
    val favicon: String?,
    val country: String?,
    val language: String?,
    val tagsCsv: String,
    val cachedAt: Long,
)

@Dao
interface StationsCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedStation>)

    @Query("SELECT * FROM stations_cache WHERE name LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun search(
        query: String,
        limit: Int,
    ): List<CachedStation>

    @Query("DELETE FROM stations_cache")
    suspend fun clear()
}
