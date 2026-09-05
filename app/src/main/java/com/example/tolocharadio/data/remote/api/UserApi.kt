package com.example.tolocharadio.data.remote.api

import com.example.tolocharadio.data.remote.dto.ThemeDto
import com.example.tolocharadio.data.remote.dto.UserDto
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

@Serializable
data class UserEnvelope(val user: UserDto)

@Serializable
data class PatchMeBody(val name: String? = null, val theme: ThemeDto? = null)

@Serializable
data class PatchPasswordBody(val currentPassword: String, val newPassword: String)

/** Perfil propio (Bearer). */
interface UserApi {
    @GET("users/me")
    suspend fun me(): Response<UserEnvelope>

    @PATCH("users/me")
    suspend fun patchMe(
        @Body body: PatchMeBody,
    ): Response<UserEnvelope>

    @PATCH("users/me/password")
    suspend fun patchPassword(
        @Body body: PatchPasswordBody,
    ): Response<OkResult>
}
