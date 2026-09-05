package com.example.tolocharadio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/** BD local: solo caché de lectura (el servidor es la verdad). */
@Database(entities = [CachedStation::class], version = 1, exportSchema = false)
abstract class TolochaDb : RoomDatabase() {
    abstract fun stationsCache(): StationsCacheDao
}
