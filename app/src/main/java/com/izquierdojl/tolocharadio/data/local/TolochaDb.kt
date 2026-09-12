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
    version = 7,
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

/**
 * Migración v6 → v7: elimina `userEmail`. La app ya no guarda
 * credenciales de usuario (FR-009); se conservan las filas y los índices.
 */
val MIGRATION_6_7 =
    object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS saved_servers_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    url TEXT NOT NULL,
                    alias TEXT NOT NULL,
                    appName TEXT,
                    isActive INTEGER NOT NULL DEFAULT 0,
                    isDefault INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO saved_servers_new (id, url, alias, appName, isActive, isDefault, createdAt)
                SELECT id, url, alias, appName, isActive, isDefault, createdAt FROM saved_servers
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE saved_servers")
            db.execSQL("ALTER TABLE saved_servers_new RENAME TO saved_servers")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_saved_servers_url_alias ON saved_servers (url, alias)",
            )
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
