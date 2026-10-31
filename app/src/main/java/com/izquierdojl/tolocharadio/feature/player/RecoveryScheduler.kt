package com.izquierdojl.tolocharadio.feature.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scope de ejecución del manager de recuperación (spec 0042).
 *
 * En producción usa el hilo principal y mantiene el watchdog activo. Los tests
 * pueden inyectar un `TestScope` y desactivar el watchdog para que
 * `advanceUntilIdle` no encuentre un bucle infinito.
 */
@Singleton
class RecoveryScheduler
    @Inject
    constructor() {
        var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            private set

        var watchdogEnabled: Boolean = true
            private set

        internal constructor(
            scope: CoroutineScope,
            watchdogEnabled: Boolean,
        ) : this() {
            this.scope = scope
            this.watchdogEnabled = watchdogEnabled
        }

        /** Cancela el scope (tests o cierre del proceso). */
        fun cancel() {
            scope.cancel()
        }
    }
