package com.izquierdojl.tolocharadio.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.izquierdojl.tolocharadio.core.session.SessionManager
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Datasource de Media3 hacia el proxy `GET /playback/:id` con
 * `Authorization: Bearer` (FR-004). Media3 aplica la cabecera a cada
 * petición, incluidos manifiestos, variantes y segmentos HLS.
 */
@OptIn(UnstableApi::class)
class PlayerDataSourceFactory
    @Inject
    constructor(
        private val session: SessionManager,
        private val okHttp: OkHttpClient,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            val delegate = OkHttpDataSource.Factory(okHttp).createDataSource()
            session.accessTokenNow()?.let { token ->
                delegate.setRequestProperty("Authorization", "Bearer $token")
            }
            // UA identificable como pide RadioBrowser/TolochaRadio.
            delegate.setRequestProperty("User-Agent", "TolochaRadio-Android")
            return delegate
        }
    }
