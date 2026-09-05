package com.example.tolocharadio.core.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refresh token en almacenamiento cifrado. El access vive solo en
 * memoria ([SessionManager]). Ninguno se respalda en la nube
 * (ver `backup_rules.xml`).
 */
@Singleton
class TokenStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        // security-crypto 1.1.0 depreca MasterKey.Builder sin ofrecer
        // alternativa no deprecada para este caso; supresión acotada.
        @Suppress("DEPRECATION")
        private val prefs =
            EncryptedSharedPreferences.create(
                context,
                PREFS,
                MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

        /** Lee el refresh guardado, o null si no hay sesión. */
        fun getRefresh(): String? = prefs.getString(KEY_REFRESH, null)

        /** Guarda o borra (null) el refresh. */
        fun setRefresh(token: String?) {
            prefs.edit().putString(KEY_REFRESH, token).apply()
        }

        private companion object {
            const val PREFS = "tolocha_tokens"
            const val KEY_REFRESH = "refresh"
        }
    }
