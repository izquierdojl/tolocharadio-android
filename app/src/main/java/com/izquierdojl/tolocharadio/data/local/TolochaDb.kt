package com.izquierdojl.tolocharadio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/** BD local: solo caché de lectura (el servidor es la verdad). */
@Database(
    entities = [CachedStation::class, CachedFavorite::class, CachedHistoryEntry::class, CachedCustomStation::class],
    version = 4,
    exportSchema = false,
)
abstract class TolochaDb : RoomDatabase() {
    abstract fun stationsCache(): StationsCacheDao

    abstract fun favoritesCache(): FavoritesCacheDao

    abstract fun historyCache(): HistoryCacheDao

    abstract fun customStationsCache(): CustomStationsCacheDao
}

