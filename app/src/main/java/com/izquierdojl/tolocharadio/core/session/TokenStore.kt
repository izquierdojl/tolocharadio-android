package com.izquierdojl.tolocharadio.core.session

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refresh token en almacenamiento cifrado. El access vive solo en
 * memoria ([SessionManager]). Ninguno se respalda en la nube
 * (ver `backup_rules.xml`).
 *
 * Modelo por servidor (FR-005): el refresh global es el de la
 * sesión activa; cada servidor guarda además su refresh, email y
 * password cifrados como respaldo para re-login silencioso
 * (FR-006b). El id del servidor activo se trackea para saber a
 * quién pertenecen las credenciales globales.
 *
 * FR-010: Si falla el acceso al almacenamiento cifrado, se loguea
 * el error de forma estructurada sin exponer datos sensibles y se
 * retorna null para que la app pide login de nuevo.
 */
@Singleton
class TokenStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        /** Credenciales cifradas de un servidor (respaldo para auto-login). */
        data class ServerCredentials(
            val refresh: String?,
            val email: String?,
            val password: String?,
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

        /** Lee el refresh guardado, o null si no hay sesión o error de cifrado. */
        fun getRefresh(): String? =
            try {
                prefs?.getString(KEY_REFRESH, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read refresh token", e)
                null
            }

        /** Guarda o borra (null) el refresh. Silencioso en error. */
        fun setRefresh(token: String?) {
            try {
                prefs?.edit()?.putString(KEY_REFRESH, token)?.apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write refresh token", e)
            }
        }

        /** Lee las credenciales cifradas de un servidor (solo session core). */
        fun getServerCredentials(serverId: String): ServerCredentials? =
            try {
                val key = "srv:$serverId"
                prefs?.let { p ->
                    ServerCredentials(
                        refresh = p.getString("$key:refresh", null),
                        email = p.getString("$key:email", null),
                        password = p.getString("$key:password", null),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read server credentials", e)
                null
            }

        /** Guarda (o borra con nulls) las credenciales cifradas de un servidor. */
        fun setServerCredentials(
            serverId: String,
            refresh: String?,
            email: String?,
            password: String?,
        ) {
            try {
                val p = prefs ?: return
                val key = "srv:$serverId"
                p.edit()
                    .putString("$key:refresh", refresh)
                    .putString("$key:email", email)
                    .putString("$key:password", password)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write server credentials", e)
            }
        }

        /** Guarda email+password de un servidor conservando su refresh actual. */
        fun saveServerAuth(
            serverId: String,
            email: String,
            password: String,
        ) {
            val existing = getServerCredentials(serverId)
            setServerCredentials(
                serverId,
                refresh = existing?.refresh ?: getRefresh(),
                email = email,
                password = password,
            )
        }

        /** ID del servidor al que pertenecen las credenciales globales. */
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

        /** Borra las credenciales cifradas de un servidor (FR-008). */
        fun deleteServerCredentials(serverId: String) {
            try {
                val p = prefs ?: return
                val key = "srv:$serverId"
                p.edit()
                    .remove("$key:refresh")
                    .remove("$key:email")
                    .remove("$key:password")
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete server credentials", e)
            }
        }

        private companion object {
            private const val TAG = "TokenStore"
            private const val PREFS = "tolocha_tokens"
            private const val KEY_REFRESH = "refresh"
            private const val KEY_ACTIVE_SERVER = "active_server_id"
        }
    }
