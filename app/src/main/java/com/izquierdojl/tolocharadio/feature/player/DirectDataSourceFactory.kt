package com.izquierdojl.tolocharadio.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Datasource para URLs directas (reproducción de emisoras de lista, spec 0019).
 *
 * A propósito **no** añade `Authorization`: el token de la cuenta nunca debe
 * viajar a hosts de terceros (FR-010 y constitución II). A diferencia de
 * [AuthDataSourceFactory], que inyecta Bearer para el proxy propio.
 */
@OptIn(UnstableApi::class)
@Singleton
class DirectDataSourceFactory
    @Inject
    constructor() : DataSource.Factory {
        override fun createDataSource(): DataSource =
            DefaultHttpDataSource.Factory()
                .setUserAgent("TolochaRadio-Android")
                .createDataSource()
    }
