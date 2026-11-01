# Bug Assessment: Cortes intermitentes en emisoras HLS por timeout de lectura

- **Slug**: 0045-jlizquierdo-20261101-hls-live-cortes-intermitentes
- **Created**: 2026-11-01
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> Tras probar la última especificación, las emisoras que se reproducen por m3u8
> (por ejemplo `https://atres-live.ondacero.es/live/ondacero/bitrate_1.m3u8`)
> van erráticas, con saltos de reproducción, cortes intermitentes, etc. Este
> comportamiento se resolvió en la web/api
> https://www.github.com/izquierdojl/tolocharadio. Explora y propone o procede
> con una corrección al error.

## Symptom

Las emisoras cuyo origen es HLS y que el backend sirve como flujo continuo
(`GET /playback/{id}/live`) suenan con micro-cortes y saltos cada pocos
segundos. El usuario no interactúa: el audio se interrumpe, la app reconecta y
se pierde parte del directo. Las emisoras de stream directo no presentan el
problema porque el origen envía bytes de forma continua.

## Reproduction

1. Tener un servidor TolochaRadio ≥ 0.29.0 (con `GET /playback/:id/live`).
2. Configurar la app contra ese servidor e iniciar sesión.
3. Reproducir una emisora HLS, por ejemplo Onda Cero.
4. Escuchar durante 2–3 minutos: se aprecian cortes y saltos recurrentes,
   aproximadamente cada 10 s, aunque la red sea estable.

## Evidence

El puente HLS del backend entrega **un segmento cada ~10 s**, no un flujo
continuo de bytes. Medición real contra Onda Cero con
`streamHlsLive` del repo web (v0.29.1):

```text
chunk=1 first=true  ct=audio/aac bytes=163192 gapMs=2322
chunk=2 first=false ct=audio/aac bytes=162962 gapMs=257
chunk=3 first=false ct=audio/aac bytes=159243 gapMs=11128
chunk=4 first=false ct=audio/aac bytes=159902 gapMs=11571
chunk=5 first=false ct=audio/aac bytes=159578 gapMs=11571
chunk=6 first=false ct=audio/aac bytes=159701 gapMs=6145
chunk=7 first=false ct=audio/aac bytes=159745 gapMs=11776
```

El hueco entre segmentos (11–12 s) supera el `readTimeout` por defecto de
`OkHttpClient` (10 s). Cada vez que se supera, el socket lanza
`SocketTimeoutException`; Media3 lo clasifica como
`ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT` y `PlaybackRecoveryManager` lo trata
como un corte de red: cierra la conexión, reconecta y el backend vuelve a
arrancar con dos segmentos de warmup. El resultado audible es un salto.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt:56-60` —
  `playerDataSource()` construye `OkHttpClient()` con los valores por defecto
  (readTimeout = 10 s).
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerDataSourceFactory.kt:23-30`
  — usa ese cliente para cada petición del flujo continuo y del HLS legacy.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt:87-118`
  — ante el timeout inicia una reconexión completa.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactory.kt:151-166`
  — el flujo continuo se reproduce con `ProgressiveMediaSource`, que mantiene
  una única conexión HTTP abierta durante toda la escucha.
- Backend `apps/api/src/services/hlsBridge.ts` (repo web, v0.29.1) — publica
  cada segmento cuando el origen lo publica (~10 s); no es un flujo de bytes
  constante.

## Root Cause Hypothesis

Confianza: **high**.

El cliente HTTP de reproducción no está configurado para un flujo en directo
con publicaciones periódicas. OkHttp aplica `readTimeout = 10 s` por defecto y
el backend puede pasar 11–12 s sin escribir bytes entre segmentos. El timeout
no es un fallo de red real, pero la app lo interpreta como tal y reconecta,
introduciendo cortes y saltos que el usuario percibe como reproducción
errática.

Agrava el síntoma que el backend anterior a 0.29.1 no eliminaba las etiquetas
ID3 de cada segmento (corregido en `a0a2549`), lo que añadía micro-cortes de
decodificación. La app debe ser robusta frente al ritmo del puente
independientemente de la versión del servidor.

## Proposed Remediation

**Preferred**: crear un `OkHttpClient` específico para reproducción con
`readTimeout(0)` (sin timeout de lectura) y `connectTimeout(15 s)`, y usarlo en
`PlayerDataSourceFactory`. Un stream en directo no debe cerrarse por
inactividad: la detección real de estancamiento ya la hace el watchdog de 15 s
de `PlaybackRecoveryManager`, que reconecta si la posición no avanza.

**Alternatives** (optional):
- Subir el `readTimeout` a 30 s: mitiga el síntoma, pero sigue siendo un valor
  arbitrario y puede cortar si el origen tarda más en publicar.
- Cambiar el ritmo del backend: no procede; el puente ya refleja la
  periodicidad del origen y funciona en la web.
- Mantener el cliente por defecto y filtrar los timeouts de lectura en la
  recuperación: oculta el problema y deja la conexión rota.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackHttpClient.kt` (nuevo)
- `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackHttpClientTest.kt` (nuevo)

**Tests to add or update**:
- Test unitario: el cliente de reproducción tiene `readTimeoutMillis == 0`.
- Test unitario: conserva un `connectTimeout` acotado (15 s).
- Verificación manual: reproducir Onda Cero durante al menos 10 minutos y
  comprobar que no hay cortes ni reconexiones.

**Risks & Considerations**:
- Sin `readTimeout`, una conexión realmente muerta podría quedarse abierta. La
  cubre el watchdog de 15 s y el cierre explícito al pausar/detener.
- El cliente es exclusivo del reproductor: no afecta a Retrofit ni a las
  llamadas de la API, que mantienen sus timeouts.
- Requiere servidor ≥ 0.29.1 para eliminar los micro-cortes por ID3; sin esa
  versión el cliente deja de cortar por inactividad, pero puede quedar algún
  glitch de decodificación.
- No cambia la autenticación, la ruta ni el contrato del backend.

## Open Questions

- Ninguna bloqueante. El fallo se reproduce con cualquier emisora HLS cuyos
  segmentos tengan una duración cercana o superior a 10 s, que es el caso de
  Onda Cero y de la mayoría de emisoras StreamTheWorld.
