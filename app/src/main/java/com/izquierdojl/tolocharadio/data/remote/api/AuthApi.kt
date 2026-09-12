package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.AuthResponseDto
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class LoginBody(val email: String, val password: String)

@Serializable
data class RefreshBody(val refreshToken: String? = null)

/**
 * Auth por servidor: solo login y refresh. La app no usa registro,
 * recuperación de contraseña, logout ni `/users/me`.
 */
interface AuthApi {
    @POST("auth/login")
    suspend fun login(
        @Body body: LoginBody,
    ): Response<AuthResponseDto>

    @POST("auth/refresh")
    suspend fun refresh(
        @Body body: RefreshBody = RefreshBody(),
    ): Response<AuthResponseDto>
}
