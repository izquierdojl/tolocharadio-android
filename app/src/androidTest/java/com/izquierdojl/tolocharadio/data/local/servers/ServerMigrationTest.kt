package com.izquierdojl.tolocharadio.data.local.servers

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.izquierdojl.tolocharadio.data.local.MIGRATION_6_7
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migración Room 6→7 (constitución: migraciones probadas): conserva las
 * filas de `saved_servers` y elimina `userEmail`.
 */
@RunWith(AndroidJUnit4::class)
class ServerMigrationTest {
    @Test
    fun migration_6_7_conserva_filas_y_elimina_userEmail() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "server_migration_test.db"
        context.deleteDatabase(dbName)

        val config =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(6) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(CREATE_V6)
                            db.execSQL("CREATE UNIQUE INDEX index_saved_servers_url_alias ON saved_servers (url, alias)")
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        db.execSQL(
            "INSERT INTO saved_servers (id,url,alias,appName,userEmail,isActive,isDefault,createdAt) " +
                "VALUES ('1','https://s','srv','App','a@b.c',1,1,10)",
        )

        MIGRATION_6_7.migrate(db)

        val columns = columnNames(db)
        assertFalse(columns.contains("userEmail"))
        assertTrue(columns.contains("isActive"))

        db.query("SELECT id, url, alias FROM saved_servers").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.getString(cursor.getColumnIndexOrThrow("id")) == "1")
        }

        helper.close()
        context.deleteDatabase(dbName)
    }

    private fun columnNames(db: SupportSQLiteDatabase): List<String> {
        val names = mutableListOf<String>()
        db.query("PRAGMA table_info(saved_servers)").use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                names += cursor.getString(nameIdx)
            }
        }
        return names
    }

    private companion object {
        val CREATE_V6 =
            """
            CREATE TABLE saved_servers (
                id TEXT NOT NULL PRIMARY KEY,
                url TEXT NOT NULL,
                alias TEXT NOT NULL,
                appName TEXT,
                userEmail TEXT,
                isActive INTEGER NOT NULL DEFAULT 0,
                isDefault INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
    }
}
