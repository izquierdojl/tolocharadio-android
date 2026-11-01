package com.izquierdojl.tolocharadio.feature.player

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP exclusivo del flujo de reproducción (bug 0045).
 *
 * El backend publica un segmento cada ~10 s, así que la conexión pasa
 * intervalos de inactividad superiores al `readTimeout` por defecto de OkHttp
 * (10 s). Un timeout finito corta el flujo en cada hueco y la app lo interpreta
 * como un corte de red: reconecta y se pierde audio. Los streams en directo no
 * deben tener timeout de lectura; el watchdog de [PlaybackRecoveryManager] ya
 * detecta la ausencia real de progreso y decide reconectar.
 *
 * El cliente es exclusivo del reproductor: Retrofit y el resto de la API
 * conservan sus propios timeouts.
 */
object PlaybackHttpClient {
    /** Margen para establecer la conexión con el proxy. */
    const val CONNECT_TIMEOUT_SECONDS: Long = 15L

    /** Sin timeout de lectura: el puente puede estar ~12 s sin escribir bytes. */
    const val READ_TIMEOUT_MILLIS: Long = 0L

    /** Construye el cliente con los timeouts adecuados para audio en directo. */
    fun create(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
}
