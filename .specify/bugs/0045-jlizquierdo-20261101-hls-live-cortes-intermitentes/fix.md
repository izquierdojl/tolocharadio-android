# Bug Fix: Cortes intermitentes en emisoras HLS por timeout de lectura

- **Slug**: 0045-jlizquierdo-20261101-hls-live-cortes-intermitentes
- **Fixed**: 2026-11-01
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

El cliente HTTP de reproducción usaba los timeouts por defecto de OkHttp, con
un `readTimeout` de 10 s. El flujo continuo del backend publica un segmento cada
~10–12 s, así que ExoPlayer cerraba la conexión en cada hueco, la app lo
interpretaba como un corte de red y reconectaba. Ahora el reproductor usa un
cliente propio sin timeout de lectura, manteniendo el `connectTimeout` acotado y
la reconexión intacta.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackHttpClient.kt` | added | Cliente OkHttp exclusivo de reproducción: `readTimeout(0)`, `connectTimeout(15 s)` y reintento de conexión. |
| `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt` | modified | `playerDataSource()` inyecta `PlaybackHttpClient.create()` en lugar de `OkHttpClient()`; se retira el import sin uso. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackHttpClientTest.kt` | added test | Fija la ausencia de timeout de lectura, el timeout de conexión y el reintento. |

## Diff Highlights (optional)

```kotlin
// PlaybackHttpClient.kt
object PlaybackHttpClient {
    const val CONNECT_TIMEOUT_SECONDS: Long = 15L
    const val READ_TIMEOUT_MILLIS: Long = 0L

    fun create(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
}
```

```kotlin
// PlayerModule.kt
fun playerDataSource(session: SessionManager): PlayerDataSourceFactory {
    return PlayerDataSourceFactory(session, PlaybackHttpClient.create())
}
```

## Tests Added or Updated

- `PlaybackHttpClientTest.no aplica timeout de lectura al flujo en directo` —
  impide volver al `readTimeout` finito que cortaba cada segmento.
- `PlaybackHttpClientTest.acota el tiempo de conexion` — conserva un límite
  razonable para el handshake con el proxy.
- `PlaybackHttpClientTest.reintenta conexiones fallidas` — mantiene la
  tolerancia a fallos transitorios.

## Local Verification

- Commands run:
  - `./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.feature.player.PlaybackHttpClientTest"` → BUILD SUCCESSFUL.
  - `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL.
  - `./gradlew detekt ktlintCheck lintDebug` → BUILD SUCCESSFUL.
  - `./gradlew assembleDebug` → BUILD SUCCESSFUL.
- Manual checks: medición del puente HLS real de Onda Cero contra el backend
  v0.29.1; los segmentos llegan con huecos de 11,1–11,8 s, por encima del
  `readTimeout` anterior de 10 s. No se pudo probar en dispositivo porque el
  servidor configurado no está disponible en este entorno.

## Deviations from Assessment

- No existía `assessment.md` para este fallo (los 16 bugs previos tenían otros
  slugs). Se creó el assessment `0045-...` como paso previo obligatorio del
  flujo y se aplicó después la remediación propuesta, sin desviaciones.
- No se creó rama nueva: el repositorio estaba en `main` y el flujo no requiere
  commit. Si se quiere trazabilidad, la rama debería ser
  `0045-jlizquierdo-20261101-hls-live-cortes-intermitentes`.

## Follow-ups

- Desplegar el backend en **v0.29.1 o superior**. El commit `a0a2549` elimina
  las etiquetas ID3 de cada segmento y amplía el warmup a dos segmentos; sin
  esa versión pueden quedar micro-cortes de decodificación aunque el cliente ya
  no cierre la conexión.
- Verificar el proxy inverso (Apache) para que no aplique buffering ni un
  timeout de respuesta inferior a ~30 s sobre `/api/v1/playback/*/live`.
- Prueba manual en dispositivo: reproducir Onda Cero durante 10 minutos con
  pantalla apagada y comprobar que no hay reconexiones en el log.
- Considerar un test instrumentado que mantenga una conexión con huecos de
  12 s y valide que ExoPlayer no entra en `STATE_IDLE`.
