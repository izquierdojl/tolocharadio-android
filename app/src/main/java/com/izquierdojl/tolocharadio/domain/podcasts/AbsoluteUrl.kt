package com.izquierdojl.tolocharadio.domain.podcasts

/**
 * El backend firma rutas relativas (`/api/v1/...`): la web las
 * resuelve contra el origen y el cliente móvil debe absolutizarlas
 * con la URL del servidor activo antes de usarlas (player,
 * DownloadManager y Cast exigen URLs absolutas http/https).
 */
fun absoluteUrl(
    baseUrl: String,
    url: String,
): String {
    val value = url.trim()
    if (value.startsWith("http://") || value.startsWith("https://")) return value
    return baseUrl.trimEnd('/') + "/" + value.trimStart('/')
}
