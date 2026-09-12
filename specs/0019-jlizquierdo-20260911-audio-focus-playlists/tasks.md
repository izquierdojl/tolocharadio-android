---
description: "Task list for Reproducción exclusiva y soporte de listas m3u/m3u8/pls"
---

# Tasks: Reproducción exclusiva y soporte de listas m3u/m3u8/pls

**Input**: Design documents from `/specs/0019-jlizquierdo-20260911-audio-focus-playlists/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: INCLUIDOS Y OBLIGATORIOS. La constitución (Principio III, NON-NEGOTIABLE) exige tests unitarios y Red-Green: escribir el test que falla antes de la implementación. No se añade Robolectric; la lógica pura se extrae para poder testearse en JVM y la plataforma queda en wrappers delgados.

**Organization**: tareas agrupadas por user story para implementación y prueba independientes (US1 foco de audio P1; US2 listas P2).

**Nota de entorno**: `setup-tasks.ps1` se ejecutó con el shim temporal `python3.cmd` (bug `Get-Python3Command` de `common.ps1:322`; ver `research.md` R0). Este archivo se generó desde `.specify/templates/tasks-template.md`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: US1/US2 según `spec.md`
- Rutas exactas de archivo en cada tarea

## Path Conventions

- Módulo único `app` (Android/Kotlin). Código: `app/src/main/java/com/izquierdojl/tolocharadio/`
- Tests: `app/src/test/java/com/izquierdojl/tolocharadio/`
- Base package: `com.izquierdojl.tolocharadio`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: preparar la dependencia HLS y los paquetes nuevos

- [X] T001 [P] Añadir `media3-exoplayer-hls` al catálogo `gradle/libs.versions.toml` (alias `media3-exoplayer-hls` con `version.ref = "media3"`) y a `app/build.gradle.kts` (`implementation(libs.media3.exoplayer.hls)`) — única dependencia nueva, justificada en `plan.md` Complexity Tracking
- [X] T002 [P] Crear los directorios `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/`, `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/playlist/` y `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: tipos base y datasource directo compartidos por la feature

**⚠️ CRITICAL**: US2 no puede empezar hasta completar esta fase. US1 (foco) es independiente de esta fase (`PlayerModule.exoPlayer` vs tipos de lista) y puede ejecutarse tras Setup.

- [X] T003 [P] Crear `PlaybackSource` sellado (`Proxied(stationId)` / `DirectProgressive(url)` / `DirectHls(url)`, KDoc de invariante HTTPS) en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackSource.kt`
- [X] T004 [P] Crear `PlaybackError` sellado (`NETWORK` / `NO_ENTRIES` / `INSECURE_ONLY` / `MALFORMED`) con `userMessage()` en español en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackError.kt`
- [X] T005 [P] Crear `DirectDataSourceFactory` (`DataSource.Factory`, `DefaultHttpDataSource.Factory` con `User-Agent: TolochaRadio-Android`, **sin** `Authorization`) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/DirectDataSourceFactory.kt`
- [X] T006 Añadir el provider Hilt de `DirectDataSourceFactory` en `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt`

**Checkpoint**: tipos base y datasource directo listos; US2 puede empezar

---

## Phase 3: User Story 1 - Reproducción exclusiva entre aplicaciones (Priority: P1) 🎯 MVP

**Goal**: el player local gestiona el foco de audio: pausa cuando otra app reproduce, reclama `AUDIOFOCUS_GAIN` al reproducir, reanuda tras interrupción transitoria salvo pausa manual, pausa en pérdida permanente y al desconectar auriculares; igual en background, notificación y Bluetooth.

**Independent Test**: reproducir con TolochaRadio y arrancar VLC/Pocket Casts → la radio se pausa en ≤2 s sin mezcla; con VLC sonando, pulsar play en TolochaRadio → VLC se detiene; interrupción simulada (llamada) → reanuda sola salvo pausa manual. Ver `quickstart.md` Escenario 1.

### Tests for User Story 1 ⚠️ (escribir primero, deben FALLAR)

- [X] T007 [P] [US1] Escribir `PlayerAudioConfigTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerAudioConfigTest.kt` (verifica `usage == C.USAGE_MEDIA`, `contentType == C.AUDIO_CONTENT_TYPE_SPEECH`, `handleAudioFocus == true` y `handleAudioBecomingNoisy == true`)

### Implementation for User Story 1

- [X] T008 [US1] Crear `PlayerAudioConfig` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerAudioConfig.kt` (construye `androidx.media3.common.AudioAttributes` con `USAGE_MEDIA` + `CONTENT_TYPE_SPEECH`; expone la aplicación a `ExoPlayer.Builder`: `setAudioAttributes(attrs, true)` y `setHandleAudioBecomingNoisy(true)`) — hacer pasar T007
- [X] T009 [US1] Modificar `exoPlayer()` en `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt` para construir el `ExoPlayer` con `PlayerAudioConfig` (foco propio; los mensajes FR-001..FR-004 los gestiona el `AudioFocusManager` de Media3)
- [X] T010 [US1] Validar manualmente el Escenario 1 de `quickstart.md` en dispositivo/emulador: otra app inicia/pausa, llamada entrante con y sin pausa manual, background/notificación, desconexión de auriculares, modo silencio/No molestar y una sesión con interrupciones de foco repetidas (FR-001..FR-005, SC-002..SC-004, SC-007) — verificado manualmente en emulador por el usuario (2026-09-11)

**Checkpoint**: foco de audio exclusivo funcionando — MVP demostrable

---

## Phase 4: User Story 2 - Reproducir emisoras con enlace de lista (.m3u8 / .m3u / .pls) (Priority: P2)

**Goal**: detectar el formato por `station.url`, resolver listas de texto en cliente (fetch sin credenciales, parseo, relativas, filtro HTTPS, candidatos ordenados con fallback acotado) y reproducir con `HlsMediaSource`/`ProgressiveMediaSource`; funciona igual para catálogo y emisoras personalizadas, con errores tipados y sin regresión en streams directos (proxy intacto).

**Independent Test**: reproducir emisoras de prueba `.m3u8`, `.m3u` y `.pls` HTTPS → suenan; lista inválida → error tipado con reintento sin crash; una emisora directa sigue sonando por proxy. Ver `quickstart.md` Escenarios 2-4.

### Tests for User Story 2 ⚠️ (escribir primero, deben FALLAR)

- [X] T011 [P] [US2] Escribir `PlaylistFormatTest` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/PlaylistFormatTest.kt` (`.m3u8`/`.m3u`/`.pls` case-insensitive, con query, sin path, URL inválida → `null`, desconocido → `null`)
- [X] T012 [P] [US2] Escribir `PlaylistParserTest` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/PlaylistParserTest.kt` (M3U con `#EXTM3U`/`#EXTINF`/comentarios, relativas resueltas contra `finalUrl`, `http` descartado, dedupe, vacío → `NO_ENTRIES`; PLS `FileN` desordenado, `TitleN` ignorado, texto no reconocible → `MALFORMED`)
- [X] T013 [P] [US2] Escribir `ResolvePlaybackSourceUseCaseTest` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/ResolvePlaybackSourceUseCaseTest.kt` con `PlaylistFetcher` fake (stream directo → `Proxied`, `.m3u8` → `Single(DirectHls)`, `.m3u`/.pls → `Candidates`, fetch KO → `NETWORK`, solo http → `INSECURE_ONLY`, máx. 5 y dedupe)
- [X] T014 [P] [US2] Escribir `PlaylistPlaybackQueueTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaylistPlaybackQueueTest.kt` (`current`, `advance` hasta agotar, `exhausted`, lista de 1 elemento)
- [X] T015 [P] [US2] Escribir `StationMediaItemFactoryTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactoryTest.kt` (URI proxy para `Proxied`, URI directa y `mimeType == MimeTypes.APPLICATION_M3U8` para `DirectHls`, sin mime para `DirectProgressive`, metadata conservada; usar emisoras con `favicon = null`)
- [X] T016 [P] [US2] Ampliar `PlayerViewModelTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt`: fuente directa/HLS sin bloqueo por precheck, fallback acotado de candidatos en `onPlayerError`, agotamiento → `PlayerState.Error`, camino proxy intacto (mantener tests existentes en verde)

### Implementation for User Story 2

- [X] T017 [P] [US2] Implementar `PlaylistFormat.detect(url)` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaylistFormat.kt` (path sin query con `java.net.URI`, `null` ante URL inválida) — hacer pasar T011
- [X] T018 [P] [US2] Implementar `PlaylistParser` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaylistParser.kt` (gramáticas M3U/PLS de `contracts/playlist-resolution.md`, `URI.resolve`, filtro https, dedupe, máx. 5) — hacer pasar T012
- [X] T019 [P] [US2] Crear la interfaz `PlaylistFetcher` y `PlaylistContent(text, finalUrl)` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaylistFetcher.kt` (contrato de fetch en `contracts/playlist-resolution.md` §2)
- [X] T020 [US2] Implementar `ResolutionResult` + `ResolvePlaybackSourceUseCase` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/ResolvePlaybackSourceUseCase.kt` (clasifica, resuelve listas, mapea a `PlaybackError`) — hacer pasar T013; depende de T017, T018, T019
- [X] T021 [P] [US2] Implementar `PlaylistPlaybackQueue` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaylistPlaybackQueue.kt` — hacer pasar T014
- [X] T022 [P] [US2] Implementar `StationMediaItemFactory` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactory.kt` (`create(station, source, baseUrl)` con metadata + mime HLS; `createMediaSource(item, source)` con `HlsMediaSource`/`ProgressiveMediaSource`) — hacer pasar T015
- [X] T023 [US2] Implementar `OkHttpPlaylistFetcher` en `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/playlist/OkHttpPlaylistFetcher.kt` (OkHttp **sin** `Authorization`, redirects, timeout 10 s, cuerpo limitado ~1 MB, `User-Agent` propio); depende de T019
- [X] T024 [US2] Registrar `PlaylistFetcher → OkHttpPlaylistFetcher` en `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt`; depende de T019 y T023
- [X] T025 [P] [US2] Ampliar `ActiveStationHolder` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/ActiveStationHolder.kt` con `resolvedSource: PlaybackSource?` (set/clear junto a estación y estado)
- [X] T026 [US2] Integrar la resolución en `PlayerViewModel` (`play()`/`startStream()`/`onPlayerError`/`retry()`/`stop()`) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt`: precheck bloqueante solo para `Proxied`, uso de `StationMediaItemFactory`, cola de fallback (máx. 2 saltos) y persistencia en el holder — hacer pasar T016; depende de T020, T021, T022, T025
- [X] T027 [US2] Actualizar `CastPlayerManager` en `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`: `connectToStation(station, source)` con la fuente resuelta y `resumeLocalPlayback` usando `StationMediaItemFactory` + `holder.resolvedSource` (sin fetch de lista); mantener su foco manual — depende de T022, T025, T026
- [X] T028 [US2] Validar manualmente los Escenarios 2, 3 y 4 de `quickstart.md` (`.m3u8`, `.m3u`, `.pls`, emisora personalizada y errores) en dispositivo/emulador (FR-006..FR-010, SC-001, SC-006) — verificado manualmente en emulador por el usuario (2026-09-11)

**Checkpoint**: emisoras de lista reproducen y los errores son accionables; UI/notificación/controles sin cambios

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: gates de calidad, documentación y validación integral

- [X] T029 Ejecutar `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` y corregir cualquier error o warning nuevo (obligatorio para el merge)
- [X] T030 [P] Añadir KDoc a las APIs públicas nuevas de `domain/playback/` y `data/remote/playlist/` (contrato, errores y dispatcher) según constitución IV/V
- [X] T031 Validar el Escenario 5 de `quickstart.md` (no regresión de streams directos, historial, Cast con HLS/lista y reanudación post-Cast) con build de debug (FR-011, FR-012, SC-005) — verificado manualmente en emulador por el usuario (2026-09-11)
- [X] T032 Documentar en la PR: dependencia `media3-exoplayer-hls` justificada (tamaño/alternativa) y excepción de reproducción directa de listas (Complexity Tracking) enlazando el issue de deuda #4 (revisión 2026-10-09, máx. 2 sprints) — documentado en la PR (2026-09-11)

**Estado de gates (2026-09-11)**: `testDebugUnitTest` (335 tests) y `assembleDebug` en verde. `detekt`, `ktlintCheck` y `lintDebug` siguen fallando **solo por deuda preexistente** en archivos ajenos a esta feature; verificado contra `HEAD` limpio: detekt 29→29 (0 nuevos), lint 13→11 (0 nuevos), ktlint main/test con los archivos de esta spec limpios. T010, T028 y T031 requieren dispositivo/emulador; T032 requiere PR.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: depende de Setup; bloquea US2. US1 solo depende de Setup
- **US1 (Phase 3)**: depende de Setup; es el MVP e independiente de US2 (toca `PlayerAudioConfig.kt` y `PlayerModule.exoPlayer`; US2 toca tipos de lista y `PlayerViewModel`)
- **US2 (Phase 4)**: depende de Foundational; puede ejecutarse en paralelo con US1 coordinando las ediciones distintas de `di/PlayerModule.kt` (T006 de Foundational, T009 de US1, T024 de US2 se aplican en secuencia sobre el mismo archivo)
- **Polish (Phase 5)**: depende de que US1 y US2 estén completas

### User Story Dependencies

- **US1**: sin dependencias de otras historias
- **US2**: sin dependencias de US1; requiere Foundational (T003–T006)

### Within Each User Story

- Tests PRIMERO y en rojo antes de implementar (constitución III)
- Modelos puros antes de casos de uso; caso de uso antes de ViewModel; ViewModel antes de Cast
- Cada historia termina con su validación manual de `quickstart.md`
- No commitear sin que lo pida el usuario (el hook `speckit.git.commit` es opcional)

### Parallel Opportunities

- Setup: T001 y T002 en paralelo
- Foundational: T003, T004 y T005 en paralelo; T006 tras T005
- US1: T007 en solitario (test), después T008 → T009 → T010
- US2 tests: T011–T016 en paralelo (archivos distintos)
- US2 implementación: T017, T018, T019, T021, T022 y T025 en paralelo; T020 tras T017–T019; T023 tras T019; T024 tras T019/T023; T026 tras T020–T022/T025; T027 tras T026
- Entre historias: US1 y US2 en paralelo por dos personas (solo coinciden en `di/PlayerModule.kt`, coordinar T009 vs T024)
- Polish: T030 en paralelo con T029 y T031

---

## Parallel Example: User Story 2

```bash
# Tests de US2 en paralelo (archivos distintos):
Task: "Escribir PlaylistFormatTest en app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/PlaylistFormatTest.kt"
Task: "Escribir PlaylistParserTest en app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/PlaylistParserTest.kt"
Task: "Escribir ResolvePlaybackSourceUseCaseTest en app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/ResolvePlaybackSourceUseCaseTest.kt"
Task: "Escribir PlaylistPlaybackQueueTest en app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaylistPlaybackQueueTest.kt"

# Implementación pura en paralelo:
Task: "Implementar PlaylistFormat en app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaylistFormat.kt"
Task: "Implementar PlaylistParser en app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaylistParser.kt"

# Tras crear la interfaz PlaylistFetcher (T019):
Task: "Implementar OkHttpPlaylistFetcher en app/src/main/java/com/izquierdojl/tolocharadio/data/remote/playlist/OkHttpPlaylistFetcher.kt"
```

## Parallel Example: User Story 1

```bash
# Test de US1 (primero, en rojo):
Task: "Escribir PlayerAudioConfigTest en app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerAudioConfigTest.kt"

# Implementación secuencial (config → player → validación de dispositivo):
Task: "Crear PlayerAudioConfig en app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerAudioConfig.kt"
Task: "Modificar exoPlayer() en app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup (T001–T002)
2. US1: T007 (test rojo) → T008 → T009 → T010
3. **PARAR Y VALIDAR**: Escenario 1 de `quickstart.md` (VLC/Pocket Casts, llamada, auriculares)
4. Demo del foco exclusivo

### Incremental Delivery

1. Setup + Foundational → base lista
2. US1 → foco exclusivo (MVP)
3. US2 → listas `.m3u8`/`.m3u`/`.pls` + fallback + Cast (segundo incremento)
4. Polish → gates de CI, KDoc, validación integral y documentación de deuda

### Parallel Team Strategy

Con dos personas: tras Setup+Foundational, una toma US1 (T007–T010) y otra US2 (T011–T028). La única coincidencia es `di/PlayerModule.kt`: T009 (US1) y T024 (US2) deben coordinarse o aplicarse en secuencia. El resto de archivos es disjunto.

---

## Notes

- [P] = archivos distintos y sin dependencias pendientes
- La etiqueta [Story] da trazabilidad a `spec.md`
- Verificar que cada test falla antes de implementar (Red-Green)
- Los modelos y la lógica de decisión son Kotlin puro/JVM (`domain/playback`) para evitar Robolectric
- El token Bearer nunca se envía en el fetch de listas ni en reproducción directa (contracts/playlist-resolution.md §2)
- Evitar: tareas vagas, conflictos de archivo y dependencias cruzadas que rompan la independencia de las historias
