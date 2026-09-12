package com.izquierdojl.tolocharadio.domain.servers

/** Modelo de dominio para un servidor guardado (sin credenciales). */
data class SavedServer(
    val id: String,
    val url: String,
    val alias: String,
    val appName: String?,
    val isActive: Boolean,
    val isDefault: Boolean,
    val createdAt: Long,
)
