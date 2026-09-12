# Implementation Plan: Reproducción exclusiva y soporte de listas m3u/m3u8/pls

**Branch**: `0019-jlizquierdo-20260911-audio-focus-playlists` | **Date**: 2026-09-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0019-jlizquierdo-20260911-audio-focus-playlists/spec.md`

**Note**: `setup-plan.ps1` se ejecutó correctamente sorteando el bug conocido de `Get-Python3Command` (stub `python3` de Microsoft Store) con un shim temporal `python3.cmd → python` antepuesto al PATH de la invocación (ver research.md R0). El template se copió de forma automática y este documento lo completa.

## Summary

Dos mejoras independientes sobre la reproducción de audio:

1. **Reproducción exclusiva (foco de audio)**: el `ExoPlayer` local pasa a gestionar el foco con Media3 (`USAGE_MEDIA` + `CONTENT_TYPE_SPEECH`, `handleAudioFocus = true`, `handleAudioBecomingNoisy = true`). Con eso: otra app que reproduce pausa TolochaRadio (FR-001); al reproducir TolochaRadio se solicita `AUDIOFOCUS_GAIN` y las demás apps se detienen (FR-002); una interrupción transitoria pausa y reanuda al volver el foco salvo pausa manual previa (FR-003); una pérdida permanente pausa sin auto-reanudar (FR-004); se aplica igual en background/notificación/Bluetooth (FR-005) y al desconectar auriculares.
2. **Emisoras servidas como listas**: `domain/playback/` clasifica la URL de la emisora (`.m3u8` HLS; `.m3u`/`.pls` listas de texto; resto = stream directo), resuelve en cliente las listas de texto (fetch sin credenciales, parseo, resolución de URLs relativas, filtro HTTPS, candidatos ordenados con fallback acotado) y reproduce la fuente resultante con `HlsMediaSource` (nueva dependencia `media3-exoplayer-hls`) o `ProgressiveMediaSource`. La creación de `MediaItem`/`MediaSource` se centraliza en una única fábrica usada por reproducción local, Cast y reanudación post-Cast.

Excepción justificada (Complexity Tracking): las emisoras de lista se reproducen **directas por HTTPS**, sin pasar por el proxy `/playback/:id`, porque el proxy entrega bytes sin reescribir manifiestos HLS (rompería segmentos relativos) ni convierte `.m3u`/`.pls` en audio; resolverlo en servidor exige un cambio en el repo backend, fuera de alcance de esta spec. La consecuencia (historial server-side de esas emisoras) se registra como deuda con seguimiento.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JDK 17 (bytecode target 17; CI Temurin 17)

**Primary Dependencies**: Media3 1.4.1 (ExoPlayer + MediaSession + datasource-okhttp + cast) **+ NUEVA `androidx.media3:media3-exoplayer-hls:1.4.1`**, OkHttp 4.12.0, Hilt 2.60.1, Coroutines/Flow 1.8.1, Compose BOM 2025.01.00; Retrofit/PlaybackRepo existentes para precheck `playback/:id/status`

**Storage**: N/A — no hay persistencia nueva. Se amplía en memoria `ActiveStationHolder` con la última `PlaybackSource` resuelta para reanudar tras Cast sin repetir red

**Testing**: JUnit4 4.13.2, MockK 1.13.12, Turbine 1.1.0, `kotlinx-coroutines-test`; sin Robolectric (no está en el stack). CI: `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug`

**Target Platform**: Android `minSdk 26`, `targetSdk`/`compileSdk` 37; `usesCleartextTraffic="false"` (FR-010)

**Project Type**: mobile-app (un único módulo Gradle `app`, capas `core/`, `data/`, `domain/`, `feature/`)

**Performance Goals**: pausa por pérdida de foco ≤ 2 s (SC-002); resolución de lista (fetch + parse) en ≤ 3 s en red normal; arranque de reproducción percibido ≤ 5 s; sin trabajo de red en el hilo principal

**Constraints**: HTTPS-only para cualquier URL reproducida (FR-010); el token Bearer nunca se envía a hosts de terceros (fetch de listas y reproducción directa van sin `Authorization`); Media3 como único motor; sin cambios de esquema Room; una sola dependencia nueva justificada; los streams no-lista mantienen el proxy autenticado sin cambios

**Scale/Scope**: ~6 clases nuevas de producción (3 puras de dominio, 1 fetcher de datos, 1 fábrica de MediaItem, 1 cola de fallback) + 1 config de audio + refactor acotado de 3 sitios de creación de `MediaItem`; ~5 archivos de test nuevos + 1 ampliado

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento | Evidencia |
|-----------|--------------|-----------|
| I. MVVM + Clean por capas | PASS | Clasificación/parseo/candidatos en `domain/playback` (Kotlin puro, sin Android); fetch de red en `data/remote/playlist`; orquestación en `PlayerViewModel` y `CastPlayerManager`; Hilt para todo; un solo módulo `app` |
| II. Kotlin-First, Compose M3 y Media3 | PASS con excepción justificada | Foco gestionado por Media3 (FR-005 de la constitución); HLS con Media3 (`media3-exoplayer-hls`, misma versión del BOM manual); sin XML/Views nuevos. Excepción: las emisoras de lista no usan el proxy por incompatibilidad técnica del proxy con HLS/`.m3u`/`.pls` → ver Complexity Tracking |
| III. Calidad Test-First (NON-NEGOTIABLE) | PASS | Red-Green: tests JUnit de parser/clasificador/resolver/cola/attrs antes de la implementación; `PlayerViewModelTest` ampliado para fuente de lista, fallback y error; sin Robolectric |
| IV. Streaming robusto y manejo de errores | PASS | `ResolutionResult`/`PlaybackError` tipados con mensajes en español (FR-009/FR-013); fallback acotado (sin bucles); sin `try/catch` genéricos; KDoc en APIs de `domain`/`data` |
| V. Simplicidad modular (YAGNI) | PASS | Una sola dependencia nueva, oficial y requerida para HLS; sin multi-módulo, sin WorkManager, sin persistencia nueva; la fábrica única de `MediaItem` responde a 3 consumidores reales (local, Cast, reanudación) |

**Post-Phase 1 re-check**: PASS con la misma excepción justificada. El diseño (parser y resolver puros, fetcher sin auth, config de audio aislada, cola acotada) no añade más violaciones ni módulos Gradle.

## Project Structure

### Documentation (this feature)

```text
specs/0019-jlizquierdo-20260911-audio-focus-playlists/
├── plan.md              # Este archivo (/speckit.plan)
├── research.md          # Fase 0 (/speckit.plan)
├── data-model.md        # Fase 1 (/speckit.plan)
├── quickstart.md        # Fase 1 (/speckit.plan)
├── contracts/
│   ├── audio-focus.md           # Comportamiento de foco (FR-001..005)
│   └── playlist-resolution.md   # Detección, parseo y resolución (FR-006..013)
├── checklists/
│   └── requirements.md  # Calidad de spec (ya existente)
└── tasks.md             # Fase 2 (/speckit.tasks — NO creado aquí)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── di/PlayerModule.kt                          # ~ ExoPlayer con AudioAttributes(USAGE_MEDIA/SPEECH, handleAudioFocus) + handleAudioBecomingNoisy; provee datasource sin auth para URLs directas
├── domain/playback/                            # NUEVO (Kotlin puro, testeable en JVM)
│   ├── PlaybackSource.kt                       # sellado: Proxied | DirectProgressive(url) | DirectHls(url)
│   ├── PlaybackError.kt                        # sellado/enum: NETWORK | NO_ENTRIES | INSECURE_ONLY | MALFORMED
│   ├── PlaylistFormat.kt                       # detección por extensión (m3u8/hls, m3u, pls) con java.net.URI
│   ├── PlaylistParser.kt                       # parser puro m3u/pls + resolución de relativas + filtro https + dedupe
│   ├── PlaylistFetcher.kt                      # interfaz: suspend fetch(url) → PlaylistContent(texto, finalUrl)
│   └── ResolvePlaybackSourceUseCase.kt         # detecta formato, fetch+parse de listas, candidatos ordenados (máx. 5)
├── data/remote/playlist/OkHttpPlaylistFetcher.kt # NUEVO: GET sin Authorization, timeouts, redirects, body texto
├── feature/player/
│   ├── PlayerAudioConfig.kt                    # NUEVO: AudioAttributes + flags (función pura testeable)
│   ├── DirectDataSourceFactory.kt              # NUEVO: DataSource sin Authorization para URLs directas
│   ├── StationMediaItemFactory.kt              # NUEVO: único punto MediaItem/MediaSource (proxy | directo | HLS) + mime
│   ├── PlaylistPlaybackQueue.kt                # NUEVO: candidatos + índice, advance/exhausted (puro)
│   └── PlayerViewModel.kt                      # ~ resuelve fuente, reproduce, fallback acotado en onPlayerError, guarda fuente en holder
├── feature/player/ActiveStationHolder.kt       # ~ campo resolvedSource para reanudación post-Cast sin nueva red
└── cast/CastPlayerManager.kt                   # ~ connectToStation(station, source) y resumeLocalPlayback usan la fuente resuelta
```

```text
app/src/test/java/com/izquierdojl/tolocharadio/
├── domain/playback/PlaylistFormatTest.kt            # NUEVO
├── domain/playback/PlaylistParserTest.kt            # NUEVO
├── domain/playback/ResolvePlaybackSourceUseCaseTest.kt  # NUEVO (fetcher fake)
├── feature/player/PlayerAudioConfigTest.kt          # NUEVO
├── feature/player/PlaylistPlaybackQueueTest.kt      # NUEVO
└── feature/player/PlayerViewModelTest.kt            # ampliado (fuente de lista, fallback, error)
```

**Structure Decision**: se mantiene el único módulo `app` y la organización por capas. La lógica de decisión (formato, parseo, candidatos, cola de fallback, atributos de audio) vive en `domain/playback` o en funciones puras de `feature/player` para poder testearse en JVM sin Robolectric; el acceso a red queda en `data/remote/playlist`; la integración con Media3 y Hilt en `feature/player`, `cast` y `di/PlayerModule`. No se extrae módulo Gradle nuevo (constitución I y V).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Reproducción directa (HTTPS) de emisoras de lista, sin proxy `/playback/:id` (constitución II) | El proxy entrega los bytes del recurso tal cual: un manifiesto HLS con segmentos relativos se resolvería contra `/api/v1/playback/...` y fallaría, y un `.m3u`/`.pls` no es audio reproducible. Resolver listas server-side exige cambios en el repo backend (fuera de esta spec) y bloquearía la feature | "Mantener todo por proxy" ya es el comportamiento actual y es exactamente lo que no reproduce estos formatos; "añadir endpoint de resolución al backend" requiere spec/PR en otro repositorio y no es implementable aquí |
| Sin registro de historial server-side para emisoras de lista | El historial se registra en el servidor al consumir el proxy; la reproducción directa no lo atraviesa | Añadir un POST de historial al contrato `/api/v1` es un cambio de backend fuera de alcance; se registra como deuda con issue y revisión ≤ 2 sprints (gobernanza) |

**Deuda registrada (gobernanza)**: [issue #4](https://github.com/izquierdojl/tolocharadio-android/issues/4) — "Resolver listas m3u/m3u8/pls en el proxy para restaurar historial"; revisión **2026-10-09** (máx. 2 sprints). Si no se resuelve, reevaluar y extender con justificación.
