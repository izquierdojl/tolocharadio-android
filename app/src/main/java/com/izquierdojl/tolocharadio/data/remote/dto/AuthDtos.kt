package com.izquierdojl.tolocharadio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Preferencia de tema del perfil (`light|dark`). */
@Serializable
enum class ThemeDto {
    @SerialName("light")
    LIGHT,

    @SerialName("dark")
    DARK,
}

/** Usuario de `GET /users/me` y `AuthResponse`. */
@Serializable
data class UserDto(
    val id: Int,
    val email: String,
    val name: String? = null,
    val theme: ThemeDto = ThemeDto.DARK,
    val createdAt: Long = 0,
)

/** Respuesta de register/login/refresh. Las cookies `Set-Cookie` se ignoran. */
@Serializable
data class AuthResponseDto(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
)

/** Configuración pública de la instancia. */
@Serializable
data class AppConfigDto(
    val appName: String,
    val registrationEnabled: Boolean,
)

/** Entrada de `{id,playable,reason}` de `playback/:id/status`. */
@Serializable
data class PlaybackStatusDto(
    val id: String,
    val playable: Boolean,
    val reason: String? = null,
)
