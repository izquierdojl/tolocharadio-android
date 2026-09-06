package com.izquierdojl.tolocharadio.data.local.servers

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** DAO para servidores guardados. */
@Dao
interface ServerDao {
    @Query("SELECT * FROM saved_servers ORDER BY isDefault DESC, createdAt DESC")
    fun getAll(): Flow<List<SavedServerEntity>>

    @Query("SELECT * FROM saved_servers WHERE id = :id")
    suspend fun getById(id: String): SavedServerEntity?

    @Query("SELECT * FROM saved_servers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): SavedServerEntity?

    @Query("SELECT * FROM saved_servers WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): SavedServerEntity?

    @Query("SELECT * FROM saved_servers WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): SavedServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: SavedServerEntity)

    @Query("UPDATE saved_servers SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE saved_servers SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: String)

    @Query("UPDATE saved_servers SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE saved_servers SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)

    @Query("DELETE FROM saved_servers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM saved_servers")
    suspend fun count(): Int
}
