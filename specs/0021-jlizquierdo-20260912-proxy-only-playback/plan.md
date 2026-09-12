# Implementation Plan: Reproducción unificada por el proxy autenticado

**Branch**: `0021-jlizquierdo-20260912-proxy-only-playback` | **Date**: 2026-09-12 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0021-jlizquierdo-20260912-proxy-only-playback/spec.md`

## Summary

El backend ya resuelve listas y HLS en el proxy autenticado `GET /playback/:id` (subrecursos HLS firmados en `GET /playback/:id/hls`, historial server-side, errores tipados y `status` para listas). Este cambio **revierte la excepción de la spec 0019** y devuelve la app a una única vía de reproducción: **todo por el proxy autenticado**.

Concretamente: se elimina la resolución de listas en cliente (fetch sin credenciales, parser M3U/PLS, candidatos y cola de fallback) y las fuentes directas; `ResolvePlaybackSourceUseCase` pasa a ser una decisión pura que solo marca si la emisora es HLS por la extensión de su enlace; `StationMediaItemFactory` apunta siempre al proxy y elige `HlsMediaSource` (Bearer + `mimeType` HLS) o `ProgressiveMediaSource` (Bearer); el preestado de disponibilidad pasa a ser **bloqueante para todo tipo de emisora**; y los motivos de error del servicio se traducen a mensajes accionables. Con ello desaparece la deuda del issue #4 (historial server-side de emisoras de lista restaurado) y la Constitución II se cumple sin excepción.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JDK 17 (bytecode target 17; CI Temurin 17)

**Primary Dependencies**: Media3 1.4.1 (ExoPlayer, MediaSession, `datasource-okhttp`, `cast` y **`exoplayer-hls`**, que se mantiene), OkHttp 4.12.0, Hilt, Coroutines/Flow, Compose BOM; Retrofit/`PlaybackRepo` para `playback/:id/status`. Se eliminan consumidores de `PlaylistFetcher`/`OkHttpPlaylistFetcher`.

**Storage**: N/A. No hay persistencia nueva. Se simplifica `ActiveStationHolder` (la fuente ya no se resuelve por red, por lo que `resolvedSource` deja de ser un dato que cachear).

**Testing**: JUnit4 4.13.2, MockK, Turbine, `kotlinx-coroutines-test`; sin Robolectric. CI: `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug`.

**Target Platform**: Android `minSdk 26`, `targetSdk`/`compileSdk` 37; `usesCleartextTraffic="false"`.

**Project Type**: mobile-app (un único módulo Gradle `app`, capas `core/`, `data/`, `domain/`, `feature/`).

**Performance Goals**: decisión de fuente O(1) y sin red; preestado de disponibilidad (precheck) + arranque percibido ≤ 5 s; HLS continuo (SC-004); sin trabajo de red en el hilo principal.

**Constraints**: reproducción SOLO por el proxy autenticado (FR-001/FR-002); Bearer en todas las peticiones, incluidas variantes y segmentos HLS (FR-004); HTTPS-only (FR-011); instancia backend actualizada (FR-013); sin cambios de esquema Room; sin dependencias nuevas.

**Scale/Scope**: cambio de reducción neta de código: se **eliminan** ~6 clases de producción y ~4 de test (parser, fetcher, cola, datasource directo, errores cliente, formato M3U/PLS) y se **modifican** ~7 archivos (resolver, fábrica de `MediaItem`, `PlayerViewModel`, `ActiveStationHolder`, `CastPlayerManager`, `PlayerModule`, strings de error).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento | Evidencia |
|-----------|--------------|-----------|
| I. MVVM + Clean por capas | PASS | La decisión de fuente es una función pura en `domain/playback`; la red de listas desaparece del cliente (la asume el servicio); orquestación en `PlayerViewModel`; Hilt se simplifica; un solo módulo `app` |
| II. Kotlin-First, Compose M3 y Media3 y proxy con Bearer | **PASS (sin excepción)** | Todas las emisoras se reproducen por `GET /playback/:id` con `Authorization: Bearer`; HLS usa `HlsMediaSource` sobre el manifiesto reescrito por el servicio. Se retira la excepción de la 0019 → Complexity Tracking vacío |
| III. Calidad Test-First (NON-NEGOTIABLE) | PASS | Red-Green: se actualizan primero los tests de resolución, fábrica de `MediaItem`, `PlayerViewModel` (preestado de disponibilidad (precheck) bloqueante, sin fallback) y se eliminan los tests de código retirado; sin Robolectric |
| IV. Streaming robusto y manejo de errores | PASS | Los motivos tipados del servicio (`PLAYLIST_EMPTY`, `PLAYLIST_INSECURE_ONLY`, `PLAYLIST_MALFORMED`, `PLAYLIST_UNREACHABLE`, `STREAM_UNAVAILABLE`) se traducen a mensajes accionables en español con reintento; sin `try/catch` genéricos; KDoc en APIs de `domain` |
| V. Simplicidad modular (YAGNI) | PASS | Se elimina código muerto (parser, fetcher, cola, datasource sin auth, errores cliente); no se añaden dependencias (se mantiene `exoplayer-hls`, ya justificada y necesaria) |

**Post-Phase 1 re-check**: PASS sin violaciones. El diseño no añade módulos Gradle ni dependencias; reduce superficie y restaura el historial server-side.

## Project Structure

### Documentation (this feature)

```text
specs/0021-jlizquierdo-20260912-proxy-only-playback/
├── plan.md              # Este archivo (/speckit.plan)
├── research.md          # Fase 0 (/speckit.plan)
├── data-model.md        # Fase 1 (/speckit.plan)
├── quickstart.md        # Fase 1 (/speckit.plan)
├── contracts/
│   └── proxy-playback.md  # Contrato de reproducción unificada por proxy
├── checklists/
│   └── requirements.md  # Calidad de spec (ya existente)
└── tasks.md             # Fase 2 (/speckit.tasks — NO creado aquí)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── domain/playback/
│   ├── PlaybackSource.kt                    # ~ Proxied(stationId, hls: Boolean) — única fuente
│   ├── HlsStation.kt                        # NUEVO: helper puro isHls(url) (sustituye a PlaylistFormat)
│   ├── ResolvePlaybackSourceUseCase.kt      # ~ decisión pura, sin red ni candidatos → PlaybackSource
│   ├── PlaylistFormat.kt                    # ✗ ELIMINAR (solo se conserva la detección HLS vía HlsStation)
│   ├── PlaylistParser.kt                    # ✗ ELIMINAR (resuelve el servicio)
│   ├── PlaylistFetcher.kt                   # ✗ ELIMINAR (PlaylistFetcher/PlaylistContent/PlaylistFetchException)
│   └── PlaybackError.kt                     # ✗ ELIMINAR (los errores llegan del servicio)
├── data/remote/playlist/OkHttpPlaylistFetcher.kt  # ✗ ELIMINAR (fetch de listas ya no es del cliente)
├── feature/player/
│   ├── StationMediaItemFactory.kt           # ~ URI siempre proxy; HLS vs progresivo según source.hls
│   ├── DirectDataSourceFactory.kt           # ✗ ELIMINAR (no hay fuentes directas)
│   ├── PlaylistPlaybackQueue.kt             # ✗ ELIMINAR (fallback lo hace el servicio)
│   ├── PlayerViewModel.kt                   # ~ preestado de disponibilidad (precheck) bloqueante para todos; sin cola/fallback; mapea errores
│   ├── ActiveStationHolder.kt               # ~ retira resolvedSource/updateResolvedSource
│   └── AuthDataSourceFactory.kt             # = sin cambios; Bearer en todas las peticiones (incl. /hls)
├── cast/CastPlayerManager.kt                # ~ resumeLocalPlayback calcula la fuente proxy desde station
├── di/PlayerModule.kt                       # ~ retira bindings de PlaylistFetcher y DirectDataSourceFactory
└── domain/playback/PlaybackStatusReason.kt  # NUEVO: mapeo puro de reason codes → mensajes en español
```

```text
app/src/test/java/com/izquierdojl/tolocharadio/
├── domain/playback/HlsStationTest.kt             # NUEVO (sustituye a PlaylistFormatTest)
├── domain/playback/ResolvePlaybackSourceUseCaseTest.kt  # ~ decisión pura (proxy / proxy+HLS)
├── domain/playback/PlaybackStatusReasonTest.kt   # NUEVO (mapeo de errores del servicio)
├── domain/playback/PlaylistFormatTest.kt         # ✗ ELIMINAR
├── domain/playback/PlaylistParserTest.kt         # ✗ ELIMINAR
├── feature/player/PlaylistPlaybackQueueTest.kt   # ✗ ELIMINAR
├── feature/player/StationMediaItemFactoryTest.kt # ~ URI proxy, mime HLS y HlsMediaSource
└── feature/player/PlayerViewModelTest.kt         # ~ preestado de disponibilidad (precheck) bloqueante para listas, sin fallback
```

**Structure Decision**: se mantiene el único módulo `app` y las capas actuales. La única lógica de cliente que queda es pura y testable en JVM (`HlsStation.isHls`, `ResolvePlaybackSourceUseCase`, `PlaybackStatusReason`). La resolución de listas se delega íntegramente en el servicio, por lo que desaparecen la capa `data/remote/playlist` y las fuentes directas.

## Complexity Tracking

> Sin violaciones: la excepción a la Constitución II que justificaba la spec 0019 queda **retirada** por este cambio.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |

**Deuda saldada (gobernanza)**: [issue #4](https://github.com/izquierdojl/tolocharadio-android/issues/4) — "Resolver listas m3u/m3u8/pls en el proxy para restaurar historial". El backend ya lo resolvió (`resolve-playlist-proxy`); este cambio retira la excepción en el cliente y restaura el historial server-side. Actualizar la Complexity Tracking de la spec 0019 y cerrar el issue al implementar.
