package com.izquierdojl.tolocharadio.core.util

/**
 * Metadatos estáticos de la compilación para el diálogo "Acerca de".
 *
 * Se provee desde `BuildConfig` (ver `AppInfoModule`) y se inyecta en
 * `SettingsViewModel`, de modo que los tests puedan simular versiones
 * distintas sin depender de los valores generados en tiempo de build.
 */
data class AppBuildInfo(
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val applicationId: String,
    val buildType: String,
    val repositoryUrl: String,
    val developer: String,
    val license: String,
)
