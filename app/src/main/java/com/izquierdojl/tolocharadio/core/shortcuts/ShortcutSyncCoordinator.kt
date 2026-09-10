package com.izquierdojl.tolocharadio.core.shortcuts

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.session.AuthState
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.di.ApplicationScope
import com.izquierdojl.tolocharadio.domain.shortcuts.BuildShortcutStationsUseCase
import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutClearer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mantiene el menú del icono sincronizado con el historial (FR-009).
 *
 * Triggers (research.md R5):
 * - cambios en `HistoryRepo.items` con sesión activa → republicar;
 * - login/restauración (`Authenticated`) → refrescar historial y republicar;
 * - app a primer plano ([onForeground]) → refrescar historial (red→caché);
 * - logout (`Unauthenticated`) o [clearNow] → limpiar (FR-010).
 *
 * Las operaciones de publicación/limpieza están serializadas con un [Mutex].
 */
@Singleton
class ShortcutSyncCoordinator
    @Inject
    constructor(
        private val publisher: ShortcutPublisher,
        private val historyRepo: HistoryRepo,
        private val sessionManager: SessionManager,
        private val buildStations: BuildShortcutStationsUseCase,
        @ApplicationScope private val scope: CoroutineScope,
    ) : ShortcutClearer {
        private val mutex = Mutex()

        @Volatile
        private var started = false

        /** Arranca la observación de historial y sesión (idempotente). */
        fun start() {
            if (started) return
            started = true
            scope.launch { observeHistory() }
            scope.launch { observeSession() }
        }

        /** Se invoca cuando la app pasa a primer plano (FR-009, US3.4). */
        fun onForeground() {
            if (sessionManager.authState.value !is AuthState.Authenticated) return
            scope.launch { refreshAndPublish() }
        }

        override suspend fun clear() = clearNow()

        /** Elimina de inmediato los accesos (logout o cambio de instancia). */
        suspend fun clearNow() {
            mutex.withLock { publisher.clear() }
        }

        private suspend fun observeHistory() {
            historyRepo.items.collect { entries ->
                if (sessionManager.authState.value is AuthState.Authenticated) {
                    publish(entries)
                }
            }
        }

        private suspend fun observeSession() {
            sessionManager.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> refreshAndPublish()
                    is AuthState.Unauthenticated -> clearNow()
                    AuthState.Loading -> Unit
                }
            }
        }

        /** Refresca el historial (red; el repo cae a caché) y republica. */
        private suspend fun refreshAndPublish() {
            when (val result = historyRepo.list()) {
                is ApiResult.Ok -> publish(result.value.items)
                is ApiResult.Err -> publish(historyRepo.snapshot())
            }
        }

        private suspend fun publish(entries: List<HistoryEntryDto>) {
            mutex.withLock {
                val slots = publisher.maxSlots()
                if (slots <= 0) return@withLock
                publisher.publish(buildStations(entries, slots))
            }
        }
    }
