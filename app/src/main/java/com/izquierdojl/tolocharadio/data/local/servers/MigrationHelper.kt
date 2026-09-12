package com.izquierdojl.tolocharadio.data.local.servers

import android.util.Log
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Migra la configuración de `baseUrl` (DataStore) al sistema de
 * servidores guardados. Se ejecuta una sola vez al arrancar.
 *
 * Los servidores migrados no tienen credenciales: el gate de arranque
 * los lleva a la pantalla unificada para completarlas (FR-010).
 */
@Singleton
class MigrationHelper
    @Inject
    constructor(
        private val dao: ServerDao,
        private val instancePrefs: InstancePrefs,
    ) {
        suspend fun migrateIfNeeded() {
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
            Log.d(TAG, "Migrated baseUrl to SavedServer")
        }

        private companion object {
            private const val TAG = "MigrationHelper"
        }
    }
