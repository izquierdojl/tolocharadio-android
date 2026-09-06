package com.izquierdojl.tolocharadio.core.session

import com.izquierdojl.tolocharadio.domain.auth.RestoreSessionUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Restaura la sesión al arrancar la app. Delega a [RestoreSessionUseCase].
 *
 * Debe invocarse en MainActivity.onCreate() antes de setContent.
 */
@Singleton
class SessionRestorer
    @Inject
    constructor(
        private val restoreSession: RestoreSessionUseCase,
        private val session: SessionManager,
    ) {
        /**
         * Intenta restaurar la sesión. Idempotente — solo actúa si
         * el estado actual es [AuthState.Loading].
         */
        suspend fun restore() {
            if (session.authState.value !is AuthState.Loading) return
            restoreSession()
        }
    }
