package com.izquierdojl.tolocharadio.data.local.servers

import android.content.Context
import android.util.Log
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Migra la configuración de `baseUrl` (DataStore) al sistema de
 * servidores guardados y elimina el almacén legacy de credenciales.
 *
 * Se ejecuta una sola vez al arrancar. Idempotente.
 */
@Singleton
class MigrationHelper
    @Inject
    constructor(
        private val dao: ServerDao,
        private val instancePrefs: InstancePrefs,
        @ApplicationContext private val context: Context,
    ) {
        /**
         * Elimina el almacén legacy `tolocha_tokens` (sin
         * `security-crypto`) y, si ya había una instancia configurada
         * y no hay servidores, la migra a un `SavedServerEntity`
         * activo y por defecto.
         */
        suspend fun migrateIfNeeded() {
            // Sin autenticación de usuario no debe quedar ningún token en disco.
            context.deleteSharedPreferences(LEGACY_TOKEN_STORE)

            if (dao.count() > 0) return

            val setupDone = instancePrefs.hasInstance.first()
            if (!setupDone) return

            val baseUrl = instancePrefs.baseUrl.first()
            if (baseUrl.isBlank()) return

            val server =
                SavedServerEntity(
                    url = baseUrl,
                    alias = "Mi servidor",
                    appName = null,
                    isActive = true,
                    isDefault = true,
                )

            dao.insert(server)
            Log.d(TAG, "Migrated baseUrl to SavedServer: $baseUrl")
        }

        private companion object {
            private const val TAG = "MigrationHelper"
            private const val LEGACY_TOKEN_STORE = "tolocha_tokens"
        }
    }
