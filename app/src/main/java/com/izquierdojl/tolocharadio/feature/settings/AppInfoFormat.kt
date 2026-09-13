package com.izquierdojl.tolocharadio.feature.settings

/**
 * Formatea la información de la aplicación como texto plano para el
 * portapapeles (FR-005). Omite el servidor activo si no existe (FR-007)
 * y no incluye credenciales, tokens ni datos personales (FR-009).
 */
fun AppInfo.toClipboardText(): String =
    buildString {
        appendLine(appName)
        appendLine("Versión: $version")
        appendLine("Número de compilación: $versionCode")
        appendLine("Identificador: $applicationId")
        appendLine("Tipo de build: $buildType")
        appendLine("Tema: $theme")
        appendLine("Pantalla de arranque: $startScreen")
        activeServerAlias
            ?.takeIf { it.isNotBlank() }
            ?.let { appendLine("Servidor activo: $it") }
        appendLine("Desarrollador: $developer")
        appendLine("Licencia: $license")
        append("Repositorio: $repositoryUrl")
    }
