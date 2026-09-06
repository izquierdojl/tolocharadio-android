package com.example.tolocharadio.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.tolocharadio.data.local.MIGRATION_1_2
import com.example.tolocharadio.data.local.MIGRATION_2_3
import com.example.tolocharadio.data.local.TolochaDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.prefs by preferencesDataStore("tolocha_prefs")

/** Room + DataStore. */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun prefs(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.prefs

    @Provides
    @Singleton
    fun db(
        @ApplicationContext context: Context,
    ): TolochaDb =
        Room.databaseBuilder(context, TolochaDb::class.java, "tolocha.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
}
