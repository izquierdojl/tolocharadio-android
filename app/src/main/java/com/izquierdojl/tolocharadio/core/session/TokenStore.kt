package com.izquierdojl.tolocharadio.core.session

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credenciales por servidor en almacenamiento cifrado: **email,
 * contraseña y refresh token**. El access token vive solo en memoria
 * ([SessionManager]). Nada de esto se respalda en la nube
 * (ver `backup_rules.xml`).
 *
 * FR-001/SC-005: las credenciales NUNCA se registran en logs.
 * Si el almacén cifrado falla, se loguea el error sin datos sensibles
 * y se devuelve null/estado seguro.
 */
@Singleton
class TokenStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        /** Credenciales cifradas de un servidor. */
        data class ServerCredentials(
            val email: String?,
            val password: String?,
            val refresh: String?,
        )

        // security-crypto 1.1.0 depreca MasterKey.Builder sin ofrecer
        // alternativa no deprecada para este caso; supresión acotada.
        @Suppress("DEPRECATION")
        private val prefs =
            runCatching {
                EncryptedSharedPreferences.create(
                    context,
                    PREFS,
                    MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }.getOrElse { e ->
                Log.e(TAG, "Failed to create EncryptedSharedPreferences", e)
                null
            }

        /** Lee las credenciales cifradas de un servidor. */
        fun getCredentials(serverId: String): ServerCredentials? =
            try {
                prefs?.let { p ->
                    ServerCredentials(
                        email = p.getString(key(serverId, KEY_EMAIL), null),
                        password = p.getString(key(serverId, KEY_PASSWORD), null),
                        refresh = p.getString(key(serverId, KEY_REFRESH), null),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read server credentials", e)
                null
            }

        /** true si el servidor tiene email y contraseña guardados (FR-010). */
        fun hasCredentials(serverId: String): Boolean {
            val creds = getCredentials(serverId) ?: return false
            return !creds.email.isNullOrBlank() && !creds.password.isNullOrBlank()
        }

        /** Guarda las credenciales cifradas de un servidor. */
        fun setCredentials(
            serverId: String,
            email: String?,
            password: String?,
            refresh: String?,
        ) {
            try {
                val p = prefs ?: return
                p.edit()
                    .putString(key(serverId, KEY_EMAIL), email)
                    .putString(key(serverId, KEY_PASSWORD), password)
                    .putString(key(serverId, KEY_REFRESH), refresh)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write server credentials", e)
            }
        }

        /** Rota solo el refresh token del servidor (tras refresh/login). */
        fun updateRefresh(
            serverId: String,
            refresh: String?,
        ) {
            try {
                prefs?.edit()?.putString(key(serverId, KEY_REFRESH), refresh)?.apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update refresh token", e)
            }
        }

        /** Borra las credenciales cifradas de un servidor. */
        fun deleteCredentials(serverId: String) {
            try {
                prefs
                    ?.edit()
                    ?.remove(key(serverId, KEY_EMAIL))
                    ?.remove(key(serverId, KEY_PASSWORD))
                    ?.remove(key(serverId, KEY_REFRESH))
                    ?.apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete server credentials", e)
            }
        }

        /** ID del servidor activo (a quién pertenece la sesión en memoria). */
        fun getActiveServerId(): String? =
            try {
                prefs?.getString(KEY_ACTIVE_SERVER, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read active server id", e)
                null
            }

        fun setActiveServerId(serverId: String?) {
            try {
                prefs?.edit()?.putString(KEY_ACTIVE_SERVER, serverId)?.apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write active server id", e)
            }
        }

        private fun key(
            serverId: String,
            field: String,
        ): String = "srv:$serverId:$field"

        private companion object {
            private const val TAG = "TokenStore"
            private const val PREFS = "tolocha_tokens"
            private const val KEY_ACTIVE_SERVER = "active_server_id"
            private const val KEY_EMAIL = "email"
            private const val KEY_PASSWORD = "password"
            private const val KEY_REFRESH = "refresh"
        }
    }
