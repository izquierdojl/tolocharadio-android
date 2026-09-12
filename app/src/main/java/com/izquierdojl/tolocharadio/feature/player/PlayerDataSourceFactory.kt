package com.izquierdojl.tolocharadio.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Datasource de Media3 hacia el proxy `GET /playback/:id`.
 * La app no usa autenticación de usuario: no añade ninguna credencial.
 */
@OptIn(UnstableApi::class)
class PlayerDataSourceFactory
    @Inject
    constructor(
        private val okHttp: OkHttpClient,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            val delegate = OkHttpDataSource.Factory(okHttp).createDataSource()
            // UA identificable como pide RadioBrowser/TolochaRadio.
            delegate.setRequestProperty("User-Agent", "TolochaRadio-Android")
            return delegate
        }

        /** Datasource clásico por si se necesita un fallback HTTP. */
        fun fallback(): DataSource.Factory = DefaultHttpDataSource.Factory().setUserAgent("TolochaRadio-Android")
    }
