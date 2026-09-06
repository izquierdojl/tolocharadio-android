package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.remote.api.PlaybackApi
import com.izquierdojl.tolocharadio.data.remote.dto.PlaybackStatusDto
import javax.inject.Inject
import javax.inject.Singleton

/** Precheck de disponibilidad del stream. */
@Singleton
class PlaybackRepo
    @Inject
    constructor(private val api: PlaybackApi) {
        /** `{id,playable,reason}` — llamar antes de arrancar ExoPlayer. */
        suspend fun status(stationId: String): ApiResult<PlaybackStatusDto> = safeCall { api.status(stationId) }
    }
