package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.AuthResponseDto
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class RegisterBody(val email: String, val password: String, val name: String? = null)

@Serializable
data class LoginBody(val email: String, val password: String)

@Serializable
data class RefreshBody(val refreshToken: String? = null)

@Serializable
data class ForgotBody(val email: String)

@Serializable
data class ResetBody(val token: String, val newPassword: String)

@Serializable
data class ForgotResult(val resetToken: String? = null)

@Serializable
data class OkResult(val ok: Boolean = true)

/** Auth JWT con refresh rotatorio (Android envía el refresh en body). */
interface AuthApi {
    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterBody,
    ): Response<AuthResponseDto>

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginBody,
    ): Response<AuthResponseDto>

    @POST("auth/refresh")
    suspend fun refresh(
        @Body body: RefreshBody = RefreshBody(),
    ): Response<AuthResponseDto>

    @POST("auth/logout")
    suspend fun logout(
        @Body body: RefreshBody = RefreshBody(),
    ): Response<OkResult>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body body: ForgotBody,
    ): Response<ForgotResult>

    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Body body: ResetBody,
    ): Response<OkResult>
}

