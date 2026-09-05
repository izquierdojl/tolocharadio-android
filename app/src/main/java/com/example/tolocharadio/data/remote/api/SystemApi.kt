package com.example.tolocharadio.data.remote.api

import com.example.tolocharadio.data.remote.dto.AppConfigDto
import retrofit2.Response
import retrofit2.http.GET

/** `System`: healthcheck y configuración pública. */
interface SystemApi {
    @GET("health")
    suspend fun health(): Response<Unit>

    @GET("config")
    suspend fun config(): Response<AppConfigDto>
}
