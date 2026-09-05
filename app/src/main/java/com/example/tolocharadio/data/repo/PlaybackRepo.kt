package com.example.tolocharadio.data.repo

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.safeCall
import com.example.tolocharadio.data.remote.api.PlaybackApi
import com.example.tolocharadio.data.remote.dto.PlaybackStatusDto
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
