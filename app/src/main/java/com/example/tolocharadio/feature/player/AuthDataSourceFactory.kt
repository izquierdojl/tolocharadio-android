package com.example.tolocharadio.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.example.tolocharadio.core.session.SessionManager
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Datasource de Media3 que inyecta `Authorization: Bearer` en el proxy
 * `GET /playback/:id`. El token jamás va en la URL (FR-007).
 */
@OptIn(UnstableApi::class)
class AuthDataSourceFactory
    @Inject
    constructor(
        private val session: SessionManager,
        private val okHttp: OkHttpClient,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            val factory = OkHttpDataSource.Factory(okHttp)
            val delegate = factory.createDataSource()
            val token = session.accessTokenNow()
            if (token != null) {
                delegate.setRequestProperty("Authorization", "Bearer $token")
            }
            // UA identificable como pide RadioBrowser/TolochaRadio.
            delegate.setRequestProperty("User-Agent", "TolochaRadio-Android")
            return delegate
        }

        /** Datasource clásico para streams que no requieren auth. */
        fun fallback(): DataSource.Factory = DefaultHttpDataSource.Factory().setUserAgent("TolochaRadio-Android")
    }
