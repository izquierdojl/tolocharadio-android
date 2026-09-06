package com.izquierdojl.tolocharadio.data.local.servers

import android.util.Log
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Migra la configuración actual de baseUrl (DataStore) al
 * nuevo sistema de servidores guardados.
 *
 * Se ejecuta una sola vez al detectar que no hay servidores
 * guardados pero sí una baseUrl en DataStore.
 */
@Singleton
class MigrationHelper
    @Inject
    constructor(
        private val dao: ServerDao,
        private val instancePrefs: InstancePrefs,
        private val tokenStore: TokenStore,
    ) {
        /**
         * Migra la baseUrl existente a un SavedServer.
         * Idempotente — solo migra si no hay servidores guardados.
         */
        suspend fun migrateIfNeeded() {
            val serverCount = dao.count()
            if (serverCount > 0) return

            // Obtener baseUrl actual
            var baseUrl = ""
            instancePrefs.baseUrl.collect { url ->
                baseUrl = url
            }

            if (baseUrl.isBlank()) return

            // Crear servidor con la URL existente: activo y por defecto
            // (FR-005 — es el único servidor migrado)
            val server =
                SavedServerEntity(
                    url = baseUrl,
                    alias = "Mi servidor",
                    appName = "Mi servidor",
                    isActive = true,
                    isDefault = true,
                )

            dao.insert(server)
            tokenStore.setActiveServerId(server.id)
            Log.d(TAG, "Migrated baseUrl to SavedServer: $baseUrl")
        }

        private companion object {
            private const val TAG = "MigrationHelper"
        }
    }
