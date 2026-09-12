package com.izquierdojl.tolocharadio.data.remote.dto

import kotlinx.serialization.Serializable

/** Configuración pública de la instancia. */
@Serializable
data class AppConfigDto(
    val appName: String,
)

/** Entrada de `{id,playable,reason}` de `playback/:id/status`. */
@Serializable
data class PlaybackStatusDto(
    val id: String,
    val playable: Boolean,
    val reason: String? = null,
)

/**
 * Respuesta de `POST /auth/login` y `POST /auth/refresh`.
 * El campo `user` del backend se ignora (`ignoreUnknownKeys`); la app
 * no modela perfil.
 */
@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
)
