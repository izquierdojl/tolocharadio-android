package com.example.tolocharadio.data.repo

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.safeCall
import com.example.tolocharadio.core.session.SessionManager
import com.example.tolocharadio.data.remote.api.PatchMeBody
import com.example.tolocharadio.data.remote.api.PatchPasswordBody
import com.example.tolocharadio.data.remote.api.UserApi
import com.example.tolocharadio.data.remote.dto.ThemeDto
import com.example.tolocharadio.data.remote.dto.UserDto
import javax.inject.Inject
import javax.inject.Singleton

/** Perfil propio. */
@Singleton
class UserRepo
    @Inject
    constructor(
        private val api: UserApi,
        private val session: SessionManager,
    ) {
        /** Perfil; en éxito sincroniza el usuario de la sesión. */
        suspend fun me(): ApiResult<UserDto> =
            when (val r = safeCall { api.me() }) {
                is ApiResult.Ok -> {
                    session.setUser(r.value.user)
                    ApiResult.Ok(r.value.user)
                }
                is ApiResult.Err -> r
            }

        /** Actualiza nombre y/o tema. */
        suspend fun patchMe(
            name: String?,
            theme: ThemeDto?,
        ): ApiResult<UserDto> =
            when (val r = safeCall { api.patchMe(PatchMeBody(name, theme)) }) {
                is ApiResult.Ok -> {
                    session.setUser(r.value.user)
                    ApiResult.Ok(r.value.user)
                }
                is ApiResult.Err -> r
            }

        /**
         * Cambia la contraseña. El servidor revoca refresh previos: quien
         * llame MUST forzar re-login tras éxito.
         */
        suspend fun patchPassword(
            current: String,
            new: String,
        ): ApiResult<Unit> =
            when (val r = safeCall { api.patchPassword(PatchPasswordBody(current, new)) }) {
                is ApiResult.Ok -> ApiResult.Ok(Unit)
                is ApiResult.Err -> r
            }
    }
