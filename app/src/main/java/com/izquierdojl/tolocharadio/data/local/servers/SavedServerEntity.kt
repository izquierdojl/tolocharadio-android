package com.izquierdojl.tolocharadio.data.local.servers

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Instancia TolochaRadio guardada por el usuario.
 * Cada entrada representa una URL + alias independiente.
 * Se permiten múltiples entradas con la misma URL (alias diferentes).
 *
 * `isActive`: servidor en uso en la sesión actual.
 * `isDefault`: servidor al que se conecta al arrancar la app (FR-005).
 */
@Entity(
    tableName = "saved_servers",
    indices = [Index(value = ["url", "alias"], unique = true)],
)
data class SavedServerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val url: String,
    val alias: String,
    val appName: String? = null,
    val userEmail: String? = null,
    val isActive: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
