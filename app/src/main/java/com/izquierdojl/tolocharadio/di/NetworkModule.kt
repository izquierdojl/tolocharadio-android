package com.izquierdojl.tolocharadio.di

import com.izquierdojl.tolocharadio.BuildConfig
import com.izquierdojl.tolocharadio.core.network.RetrofitFactory
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.api.AuthApi
import com.izquierdojl.tolocharadio.data.remote.api.CustomStationsApi
import com.izquierdojl.tolocharadio.data.remote.api.FavoritesApi
import com.izquierdojl.tolocharadio.data.remote.api.HistoryApi
import com.izquierdojl.tolocharadio.data.remote.api.PlaybackApi
import com.izquierdojl.tolocharadio.data.remote.api.StationsApi
import com.izquierdojl.tolocharadio.data.remote.api.SystemApi
import com.izquierdojl.tolocharadio.data.remote.api.UserApi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Red contra la instancia activa. La base se lee una vez al crear el
 * grafo; al cambiar de instancia la app se reinicia (logout+limpieza).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun retrofit(
        prefs: InstancePrefs,
        session: SessionManager,
        tokens: TokenStore,
        authApi: Lazy<AuthApi>,
    ): Retrofit {
        val base = runBlocking { prefs.baseUrl.first() }
        return RetrofitFactory.create(
            base.ifBlank { BuildConfig.TOLOCHA_BASE_URL },
            session,
            tokens,
            authApi,
            BuildConfig.DEBUG,
        )
    }

    @Provides
    @Singleton
    fun system(api: Retrofit): SystemApi = api.create(SystemApi::class.java)

    @Provides
    @Singleton
    fun auth(api: Retrofit): AuthApi = api.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun users(api: Retrofit): UserApi = api.create(UserApi::class.java)

    @Provides
    @Singleton
    fun stations(api: Retrofit): StationsApi = api.create(StationsApi::class.java)

    @Provides
    @Singleton
    fun favorites(api: Retrofit): FavoritesApi = api.create(FavoritesApi::class.java)

    @Provides
    @Singleton
    fun history(api: Retrofit): HistoryApi = api.create(HistoryApi::class.java)

    @Provides
    @Singleton
    fun customStations(api: Retrofit): CustomStationsApi = api.create(CustomStationsApi::class.java)

    @Provides
    @Singleton
    fun playback(api: Retrofit): PlaybackApi = api.create(PlaybackApi::class.java)
}

