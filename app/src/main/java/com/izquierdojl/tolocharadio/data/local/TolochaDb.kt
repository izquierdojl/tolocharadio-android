package com.izquierdojl.tolocharadio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.local.servers.ServerDao

/** BD local: solo caché de lectura (el servidor es la verdad). */
@Database(
    entities = [
        CachedStation::class,
        CachedFavorite::class,
        CachedHistoryEntry::class,
        CachedCustomStation::class,
        SavedServerEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class TolochaDb : RoomDatabase() {
    abstract fun stationsCache(): StationsCacheDao

    abstract fun favoritesCache(): FavoritesCacheDao

    abstract fun historyCache(): HistoryCacheDao

    abstract fun customStationsCache(): CustomStationsCacheDao

    abstract fun servers(): ServerDao
}

/** Migración v5 → v6: isActive (servidor activo) y userEmail (FR-005). */
val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE saved_servers ADD COLUMN userEmail TEXT")
            db.execSQL("ALTER TABLE saved_servers ADD COLUMN isActive INTEGER NOT NULL DEFAULT 0")
        }
    }

/** Migración v4 → v5: añade tabla saved_servers. */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS saved_servers (
                    id TEXT NOT NULL PRIMARY KEY,
                    url TEXT NOT NULL,
                    alias TEXT NOT NULL,
                    appName TEXT,
                    isDefault INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_saved_servers_url_alias ON saved_servers (url, alias)",
            )
        }
    }
