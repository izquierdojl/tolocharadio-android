package com.izquierdojl.tolocharadio.data.local

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migración v1 → v2 sin esquemas exportados (`exportSchema=false`):
 * crea la BD v1 a mano, migra con Room y verifica que
 * `stations_cache` se conserva y `favorites_cache` nace vacía.
 * Requiere dispositivo/emulador (instrumented).
 */
@RunWith(AndroidJUnit4::class)
class FavoritesMigrationTest {
    @Test
    fun migrate1To2_creaFavoritesCacheConservandoStations() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "mig-fav-manual"
        context.deleteDatabase(name)

        val v1 =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(context)
                    .name(name)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(1) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    "CREATE TABLE stations_cache (id TEXT NOT NULL, name TEXT NOT NULL, " +
                                        "favicon TEXT, country TEXT, language TEXT, tagsCsv TEXT NOT NULL, " +
                                        "cachedAt INTEGER NOT NULL, PRIMARY KEY(id))",
                                )
                                db.execSQL(
                                    "INSERT INTO stations_cache VALUES ('u1','Tolocha',null,null,null,'',0)",
                                )
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        },
                    ).build(),
            )
        v1.writableDatabase.close()

        val db =
            Room.databaseBuilder(context, TolochaDb::class.java, name)
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                )
                .build()
        val stations = runBlocking { db.stationsCache().search("", 10) }
        assertEquals("u1", stations.single().id)
        assertTrue(runBlocking { db.favoritesCache().loadOrdered() }.isEmpty())
        db.close()
        context.deleteDatabase(name)
    }
}
