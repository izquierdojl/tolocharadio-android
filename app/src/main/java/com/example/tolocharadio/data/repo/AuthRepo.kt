package com.example.tolocharadio.data.repo

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.safeCall
import com.example.tolocharadio.core.session.SessionManager
import com.example.tolocharadio.data.remote.api.AuthApi
import com.example.tolocharadio.data.remote.api.ForgotBody
import com.example.tolocharadio.data.remote.api.LoginBody
import com.example.tolocharadio.data.remote.api.RefreshBody
import com.example.tolocharadio.data.remote.api.RegisterBody
import com.example.tolocharadio.data.remote.api.ResetBody
import com.example.tolocharadio.data.remote.dto.UserDto
import javax.inject.Inject
import javax.inject.Singleton

/** Auth JWT: login, registro, refresh, logout y recuperación. */
@Singleton
class AuthRepo
    @Inject
    constructor(
        private val api: AuthApi,
        private val session: SessionManager,
    ) {
        /** Login; en éxito deja la sesión lista. */
        suspend fun login(
            email: String,
            password: String,
        ): ApiResult<UserDto> =
            when (val r = safeCall { api.login(LoginBody(email.trim(), password)) }) {
                is ApiResult.Ok -> {
                    session.setAuthenticated(r.value.user, r.value.accessToken, r.value.refreshToken)
                    ApiResult.Ok(r.value.user)
                }
                is ApiResult.Err -> r
            }

        /** Registro (solo si la instancia lo permite); deja sesión lista. */
        suspend fun register(
            email: String,
            password: String,
            name: String?,
        ): ApiResult<UserDto> =
            when (val r = safeCall { api.register(RegisterBody(email.trim(), password, name?.ifBlank { null })) }) {
                is ApiResult.Ok -> {
                    session.setAuthenticated(r.value.user, r.value.accessToken, r.value.refreshToken)
                    ApiResult.Ok(r.value.user)
                }
                is ApiResult.Err -> r
            }

        /** Cierra sesión en servidor y limpia local aunque falle la red. */
        suspend fun logout() {
            runCatching { api.logout() }
            session.logout()
        }

        /** Pide reset; mensaje neutro siempre (anti-enumeración). */
        suspend fun forgot(email: String): ApiResult<Unit> =
            when (val r = safeCall { api.forgotPassword(ForgotBody(email.trim())) }) {
                is ApiResult.Ok -> ApiResult.Ok(Unit)
                is ApiResult.Err -> r
            }

        /** Completa el reset con el token de un solo uso. */
        suspend fun reset(
            token: String,
            newPassword: String,
        ): ApiResult<Unit> =
            when (val r = safeCall { api.resetPassword(ResetBody(token, newPassword)) }) {
                is ApiResult.Ok -> ApiResult.Ok(Unit)
                is ApiResult.Err -> r
            }

        /** Renueva tokens con el refresh guardado. */
        suspend fun refreshNow(refresh: String): ApiResult<UserDto> =
            when (val r = safeCall { api.refresh(RefreshBody(refresh)) }) {
                is ApiResult.Ok -> {
                    session.setAuthenticated(r.value.user, r.value.accessToken, r.value.refreshToken)
                    ApiResult.Ok(r.value.user)
                }
                is ApiResult.Err -> r
            }
    }
