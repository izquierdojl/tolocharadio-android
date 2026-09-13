package com.izquierdojl.tolocharadio.di

import com.izquierdojl.tolocharadio.BuildConfig
import com.izquierdojl.tolocharadio.core.util.AppBuildInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provee los metadatos reales del build para el diálogo "Acerca de". */
@Module
@InstallIn(SingletonComponent::class)
object AppInfoModule {
    private const val APP_NAME = "Tolocha Radio"
    private const val REPOSITORY_URL = "https://github.com/izquierdojl/tolocharadio-android"
    private const val DEVELOPER = "izquierdojl"
    private const val LICENSE = "MIT"

    @Provides
    @Singleton
    fun provideAppBuildInfo(): AppBuildInfo =
        AppBuildInfo(
            appName = APP_NAME,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            applicationId = BuildConfig.APPLICATION_ID,
            buildType = if (BuildConfig.DEBUG) "Depuración" else "Publicación",
            repositoryUrl = REPOSITORY_URL,
            developer = DEVELOPER,
            license = LICENSE,
        )
}
