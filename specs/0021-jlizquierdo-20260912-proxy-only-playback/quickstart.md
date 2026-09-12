# Quickstart: Reproducción unificada por el proxy autenticado

**Feature**: `0021-jlizquierdo-20260912-proxy-only-playback` | **Date**: 2026-09-12

Guía de validación end-to-end. No sustituye a los tests; sirve para comprobar el comportamiento real.

## Prerrequisitos

- Instancia del servicio **actualizada** (con `resolve-playlist-proxy`) accesible por HTTPS.
- Cuenta de la app (toda la reproducción va por el proxy autenticado).
- Emisoras de prueba: una directa (p. ej. `.mp3`), una `.m3u`, una `.pls` y una `.m3u8`.
- Build debug apuntando a esa instancia (`baseUrl` en ajustes o `local.properties`).

## Comandos

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew detekt ktlintCheck lintDebug
./gradlew installDebug
```

## Escenario 1 — Ruta única y historial (US1, SC-001/SC-003)

1. Reproduce la emisora directa y una de cada lista (`.m3u`, `.pls`).
2. **Esperado**: todas suenan; la app no descarga la `station.url` del proveedor (verificable en logs de red/`adb logcat`).
3. **Esperado**: cada reproducción aparece en el historial del usuario (también las de lista).

## Escenario 2 — HLS continuo por proxy (US2, SC-004)

1. Reproduce la emisora `.m3u8` durante ≥ 5 minutos.
2. **Esperado**: emisión continua; las peticiones de manifiesto, variantes y segmentos van a `/playback/:id` y `/playback/:id/hls` con `Authorization: Bearer`.
3. **Esperado**: sin cortes atribuibles a la app y sin token en la URL.

## Escenario 3 — Errores accionables (US3, SC-006)

Provoca listas problemáticas (vacía, solo `http`, contenido no reconocido, host inalcanzable):

1. **Esperado**: mensaje específico en español, botón de reintento, sin cierre ni pantalla negra.
2. **Esperado**: si el preestado de disponibilidad (precheck) devuelve `playable:false`, no se arranca el reproductor.

## Escenario 4 — Preestado de disponibilidad (precheck) bloqueante (FR-005)

1. Con una emisora marcada `playable:false` por el servicio, pulsa reproducir.
2. **Esperado**: no se abre el reproductor; se muestra el motivo con reintento.

## Escenario 5 — No regresión de streams directos (SC-005)

1. Ejecuta la batería existente de reproducción y reproduce una emisora directa.
2. **Esperado**: comportamiento idéntico al actual (proxy + Bearer), 0 regresiones.

## Escenario 6 — Chromecast y reanudación (FR-010)

1. Con una emisora HLS reproduciendo, conecta a Chromecast; después desconecta (reanudación local).
2. **Esperado**: la fuente usada es la misma vía proxy autenticado en ambos casos.

## Criterio de éxito

- Todos los escenarios anteriores OK y `./gradlew detekt ktlintCheck lintDebug` + `testDebugUnitTest` en verde.
- 0 referencias a `PlaylistParser`, `PlaylistFetcher`/`OkHttpPlaylistFetcher`, `PlaylistPlaybackQueue`, `DirectDataSourceFactory`, `PlaybackError` ni `PlaybackSource.Direct*`.
