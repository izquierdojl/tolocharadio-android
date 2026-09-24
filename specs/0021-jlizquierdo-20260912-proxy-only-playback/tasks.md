---
description: "Task list for Reproducción unificada por el proxy autenticado"
---

# Tasks: Reproducción unificada por el proxy autenticado

**Input**: Design documents from `/specs/0021-jlizquierdo-20260912-proxy-only-playback/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: INCLUIDOS Y OBLIGATORIOS. La constitución (Principio III, NON-NEGOTIABLE) exige tests unitarios y Red-Green: escribir el test que falla antes de la implementación. Sin Robolectric; la lógica de decisión es Kotlin puro/JVM.

**Organization**: tareas agrupadas por user story (US1 ruta única P1; US2 HLS por proxy P2; US3 errores P3).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: US1/US2/US3 según `spec.md`
- Rutas exactas de archivo en cada tarea

## Path Conventions

- Módulo único `app` (Android/Kotlin). Código: `app/src/main/java/com/izquierdojl/tolocharadio/`
- Tests: `app/src/test/java/com/izquierdojl/tolocharadio/`
- Base package: `com.izquierdojl.tolocharadio`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: tipos puros base compartidos por las tres historias

- [X] T001 [P] Modificar `PlaybackSource` a `data class Proxied(stationId: String, hls: Boolean)` y retirar `DirectProgressive`/`DirectHls` (KDoc: única fuente por proxy) en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackSource.kt`
- [X] T002 [P] Crear el helper puro `HlsStation.isHls(url: String): Boolean` (path sin query, case-insensitive `.m3u8`; URL inválida/blank → `false`) en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/HlsStation.kt`
- [X] T003 [P] Crear `PlaybackStatusReason.reasonToMessage(reason: String?): String` (mapeo `PLAYLIST_*`/`STREAM_UNAVAILABLE`/desconocido → mensajes en español de `contracts/proxy-playback.md` §4) en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackStatusReason.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: adaptar resolución, holder, Cast y DI a la fuente proxy única

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase

- [X] T004 Simplificar `ResolvePlaybackSourceUseCase` (quitar `PlaylistFetcher`; `invoke(station): PlaybackSource` = `Proxied(station.id, HlsStation.isHls(station.url))`; eliminar `ResolutionResult`) en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/ResolvePlaybackSourceUseCase.kt`
- [X] T005 Simplificar `ActiveStationHolder` (retirar `resolvedSource` y `updateResolvedSource`; conservar `station`/`playerState`/`clear`) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/ActiveStationHolder.kt`
- [X] T006 Actualizar `CastPlayerManager.resumeLocalPlayback()` para calcular la fuente como `PlaybackSource.Proxied(station.id, HlsStation.isHls(station.url))` (sin `holder.resolvedSource`) en `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`
- [X] T007 Actualizar `PlayerModule` (retirar providers de `PlaylistFetcher`/`OkHttpPlaylistFetcher` y de `DirectDataSourceFactory`, y sus imports) en `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt`

**Checkpoint**: base lista; US1, US2 y US3 pueden empezar

---

## Phase 3: User Story 1 - Escuchar cualquier emisora con la misma ruta (Priority: P1) 🎯 MVP

**Goal**: todas las emisoras (directas y de lista) se reproducen por el proxy autenticado, con preestado de disponibilidad (precheck) bloqueante, historial server-side restaurado y sin fallback de candidatos en cliente.

**Independent Test**: reproducir una emisora directa y una de cada lista (`.m3u`, `.pls`, `.m3u8`) y comprobar en tráfico que todas pasan por `/playback/:id` con Bearer y quedan en historial. Ver `quickstart.md` Escenario 1.

### Tests for User Story 1 ⚠️ (escribir primero, deben FALLAR)

- [X] T008 [P] [US1] Escribir `HlsStationTest` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/HlsStationTest.kt` (`.m3u8` case-insensitive, con query, `.m3u`/`.pls`/`.mp3`/sin extensión → `false`, URL inválida/blank → `false`)
- [X] T009 [P] [US1] Reescribir `ResolvePlaybackSourceUseCaseTest` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/ResolvePlaybackSourceUseCaseTest.kt` (directa → `Proxied(id, hls=false)`; `.m3u8` → `Proxied(id, hls=true)`; `.m3u`/`.pls` → `Proxied(id, hls=false)`; sin fetcher)
- [X] T010 [P] [US1] Actualizar `PlayerViewModelTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt`: preestado de disponibilidad (precheck) bloqueante para listas (no arranca si `playable=false`), todas las fuentes por proxy, `onPlayerError` → `Error` sin fallback, eliminar fakes de `PlaylistFetcher`

### Implementation for User Story 1

- [X] T011 [US1] Unificar `play()`/`startSource()` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt`: resolución pura + preestado de disponibilidad (precheck) `playback.status` bloqueante para todo tipo de emisora; arranque con `Proxied` — hacer pasar T010
- [X] T012 [US1] Retirar de `PlayerViewModel.kt` la cola de candidatos (`playlistQueue`, `playlistAttempts`, `MAX_PLAYLIST_ATTEMPTS`) y el fallback de `onPlayerError` (deja `Error` genérico) — depende de T011
- [X] T013 [US1] Actualizar KDoc/comentarios de `PlayerViewModel.kt` y `ActiveStationHolder.kt` (quitar referencias a listas directas, fallback y `resolvedSource`)
- [X] T014 [US1] Validar manualmente el Escenario 1 de `quickstart.md` en emulador/dispositivo (ruta única por proxy + historial de listas) — verificado manualmente por el usuario (2026-09-12)

**Checkpoint**: reproducción unificada por proxy operativa — MVP demostrable

---

## Phase 4: User Story 2 - Emisión HLS continua a través del servicio (Priority: P2)

**Goal**: `.m3u8` se reproduce de forma continua siguiendo el manifiesto reescrito y los subrecursos autenticados del servicio.

**Independent Test**: reproducir varias emisoras HLS ≥5 min sin cortes; manifiestos/segmentos con Bearer por el proxy. Ver `quickstart.md` Escenarios 2 y 6.

### Tests for User Story 2 ⚠️ (escribir primero, deben FALLAR)

- [X] T015 [P] [US2] Actualizar `StationMediaItemFactoryTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactoryTest.kt` (URI siempre `streamUrl` del proxy; `hls=true` → `mimeType == MimeTypes.APPLICATION_M3U8`; `hls=false` → sin mime; metadata conservada; `favicon = null`)

### Implementation for User Story 2

- [X] T016 [US2] Actualizar `StationMediaItemFactory` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactory.kt`: `uriFor` siempre proxy; `mimeTypeFor` HLS; `createMediaSource` `HlsMediaSource.Factory(authDataSource)` vs `ProgressiveMediaSource.Factory(authDataSource)`; retirar `directDataSource` — hacer pasar T015
- [X] T017 [US2] Actualizar KDoc de `StationMediaItemFactory.kt` (proxy-only, Bearer, HLS reescrito)
- [X] T018 [US2] Validar manualmente los Escenarios 2 y 6 de `quickstart.md` (HLS continuo por proxy; Cast/reanudación con la misma fuente) — verificado manualmente por el usuario (2026-09-12)

**Checkpoint**: HLS por proxy continuo y Cast/reanudación alineados

---

## Phase 5: User Story 3 - Errores de lista claros y sin cierres (Priority: P3)

**Goal**: los motivos tipados del servicio se muestran como mensajes accionables en español, con reintento y sin códigos técnicos crudos.

**Independent Test**: provocar lista vacía, solo HTTP, malformada e inalcanzable → mensaje específico + reintento, sin crash. Ver `quickstart.md` Escenarios 3 y 4.

### Tests for User Story 3 ⚠️ (escribir primero, deben FALLAR)

- [X] T019 [P] [US3] Escribir `PlaybackStatusReasonTest` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackStatusReasonTest.kt` (cada código `PLAYLIST_*`/`STREAM_UNAVAILABLE` → su mensaje; `null`/desconocido → genérico)

### Implementation for User Story 3

- [X] T020 [US3] Usar `PlaybackStatusReason.reasonToMessage()` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` para `playable=false` y errores del preestado de disponibilidad (precheck) (en vez del `reason` crudo) — hacer pasar T019; depende de T011
- [X] T021 [US3] Validar manualmente los Escenarios 3 y 4 de `quickstart.md` (errores accionables y preestado de disponibilidad (precheck) bloqueante) — verificado manualmente por el usuario (2026-09-12)

**Checkpoint**: errores de lista claros y preestado de disponibilidad (precheck) bloqueante

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: eliminar código muerto, gobernanza, gates y validación integral

- [X] T022 [P] Eliminar `PlaylistParser.kt` y `PlaylistParserTest.kt`
- [X] T023 [P] Eliminar `PlaylistFetcher.kt` (`PlaylistFetcher`/`PlaylistContent`/`PlaylistFetchException`) y `data/remote/playlist/OkHttpPlaylistFetcher.kt` (y referencias en tests)
- [X] T024 [P] Eliminar `feature/player/PlaylistPlaybackQueue.kt` y `PlaylistPlaybackQueueTest.kt`
- [X] T025 [P] Eliminar `feature/player/DirectDataSourceFactory.kt`
- [X] T026 [P] Eliminar `domain/playback/PlaybackError.kt`
- [X] T027 [P] Eliminar `domain/playback/PlaylistFormat.kt` y `PlaylistFormatTest.kt`
- [X] T028 Verificar con búsqueda global 0 referencias a `PlaylistParser`, `PlaylistFetcher`, `OkHttpPlaylistFetcher`, `PlaylistPlaybackQueue`, `DirectDataSourceFactory`, `PlaybackError`, `PlaybackSource.Direct*` y `PlaylistFormat`
- [X] T029 [P] Actualizar la documentación de gobernanza: retirar la excepción en `specs/0019-jlizquierdo-20260911-audio-focus-playlists/plan.md` (Complexity Tracking) y la nota de deuda en `specs/0019-jlizquierdo-20260911-audio-focus-playlists/spec.md` (FR-012)
- [X] T030 Actualizar `detekt-baseline.xml` si el cambio introduce/retira entradas (ejecutar `./gradlew detektBaseline` solo si hace falta) — no hizo falta: detekt sin hallazgos
- [X] T031 Cerrar el issue #4 con comentario que enlace el commit/PR de la 0021 y confirme la reanudación del historial server-side (FR-012) — comentado; el cierre efectivo queda para el merge del PR
- [X] T032 Ejecutar `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` y corregir cualquier error o warning nuevo (obligatorio para el merge)
- [X] T033 Validar el Escenario 5 de `quickstart.md` (no regresión de streams directos y comportamientos existentes) con build de debug — verificado manualmente por el usuario (2026-09-12)
- [X] T034 Verificar que las peticiones de subrecursos HLS (`GET /playback/:id/hls?u=&d=&s=`) salen con `Authorization: Bearer` (test del datasource o paso explícito añadido a `quickstart.md` §2) — FR-004 (paso explícito en `quickstart.md` §2)
- [X] T035 Abrir un issue de seguimiento en el repo backend `izquierdojl/tolocharadio` para que las listas de texto que resuelvan a HLS se sirvan reescritas, y enlazarlo desde `spec.md`/PR — FR-014 (abierto `izquierdojl/tolocharadio#8`)
- [X] T036 Añadir la nota de release/README indicando que las emisoras de lista requieren una instancia del servicio actualizada — FR-013

**Estado de gates**: `assembleDebug`, `testDebugUnitTest` (322 tests, 0 fallos), `detekt`, `ktlintCheck` y `lintDebug` en verde (2026-09-12). Verificación manual del usuario completada (T014, T018, T021, T033).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: depende de Setup; bloquea todas las user stories
- **US1 (Phase 3)**: depende de Foundational; es el MVP
- **US2 (Phase 4)**: depende de Foundational y de `PlaybackSource` (T001); comparte `PlayerViewModel` con US1 pero toca sobre todo `StationMediaItemFactory`
- **US3 (Phase 5)**: depende de US1 (T011) para el punto de integración del mapeo de errores
- **Polish (Phase 6)**: depende de que US1–US3 estén completas

### User Story Dependencies

- **US1**: sin dependencias de otras historias (tras Foundational)
- **US2**: sin dependencias de US1 (tras Foundational)
- **US3**: depende de US1 (usa el mismo `PlayerViewModel`)

### Within Each User Story

- Tests PRIMERO y en rojo antes de implementar (constitución III)
- Tipos/decisiones puras antes de la integración en `PlayerViewModel`
- Cada historia termina con su validación manual de `quickstart.md`
- No commitear sin que lo pida el usuario (el hook `speckit.git.commit` es opcional)

### Parallel Opportunities

- Setup: T001, T002 y T003 en paralelo
- Foundational: T004, T005 y T006 en paralelo; T007 tras T005/T006
- US1 tests: T008, T009 y T010 en paralelo
- US2: T015 (test) → T016 → T017
- US3: T019 (test) → T020
- Polish: T022–T027 y T034–T036 en paralelo (archivos distintos); T029 en paralelo con T028/T030/T031

---

## Parallel Example: User Story 1

```bash
# Tests de US1 en paralelo (archivos distintos):
Task: "Escribir HlsStationTest en app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/HlsStationTest.kt"
Task: "Reescribir ResolvePlaybackSourceUseCaseTest en app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/ResolvePlaybackSourceUseCaseTest.kt"
Task: "Actualizar PlayerViewModelTest en app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt"
```

## Parallel Example: Polish (eliminación de código muerto)

```bash
# Archivos independientes, en paralelo:
Task: "Eliminar PlaylistParser.kt y PlaylistParserTest.kt"
Task: "Eliminar PlaylistFetcher.kt y OkHttpPlaylistFetcher.kt"
Task: "Eliminar PlaylistPlaybackQueue.kt y PlaylistPlaybackQueueTest.kt"
Task: "Eliminar DirectDataSourceFactory.kt"
Task: "Eliminar PlaybackError.kt"
Task: "Eliminar PlaylistFormat.kt y PlaylistFormatTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup (T001–T003)
2. Completar Phase 2: Foundational (T004–T007)
3. Completar Phase 3: US1 (T008–T014)
4. **PARAR Y VALIDAR**: Escenario 1 de `quickstart.md` (ruta única + historial)
5. Demo de reproducción unificada por proxy

### Incremental Delivery

1. Setup + Foundational → base lista
2. US1 → reproducción unificada por proxy e historial restaurado (MVP)
3. US2 → HLS continuo por proxy
4. US3 → errores accionables y preestado de disponibilidad (precheck) bloqueante
5. Polish → retirada de código muerto, gobernanza (issue #4), gates y validación integral

### Parallel Team Strategy

Con dos personas: tras Setup+Foundational, una toma US1 y otra US2 (coinciden en `PlayerViewModel`/`StationMediaItemFactory`: coordinar T011 con T016). US3 llega después de US1. El resto de archivos es disjunto.

---

## Notes

- [P] = archivos distintos y sin dependencias pendientes
- La etiqueta [Story] da trazabilidad a `spec.md`
- Verificar que cada test falla antes de implementar (Red-Green)
- La única lógica de cliente que queda es pura/JVM (`HlsStation`, `ResolvePlaybackSourceUseCase`, `PlaybackStatusReason`)
- El Bearer va en todas las peticiones (incl. subrecursos `/playback/:id/hls`); nunca en la URL
- Evitar: tareas vagas, conflictos de archivo y dependencias cruzadas que rompan la independencia de las historias

---

## Phase 7: Convergence

**Origen**: `/speckit.converge` 2026-09-20 — 14 FR, 7 SC, decisiones de `plan.md` y 5 principios de constitución revisados contra el código. Sin regresión en la vía local (proxy-only, Bearer, precheck bloqueante, errores accionables y 0 referencias a código de listas en `app/src`).

- [X] T037 Documentar en `specs/0021-jlizquierdo-20260912-proxy-only-playback/contracts/proxy-playback.md` y en la Complexity Tracking de la spec que Cast usa la URL pública (`StationMediaItemFactory.createForCast`/`castUriFor` + `CastPlayerManager.connectToStation`) en vez del proxy autenticado, con motivo técnico (el receptor no puede enviar `Authorization` → 401, bug 0026) y consecuencia (sin historial server-side en Cast) per FR-001/FR-010 + Constitución II (contradicts) — CRITICAL — hecho 2026-09-20 (§9 del contrato + nota en plan.md)
- [X] T038 Añadir o recuperar en `README.md` o `docs/` la nota de release de que las emisoras de lista requieren una instancia del servicio actualizada con `resolve-playlist-proxy` per FR-013 (partial) — hecho 2026-09-20 (`docs/instalacion.md`, Requisitos)
- [X] T039 Eliminar `PlayerDataSourceFactory.fallback()` sin usos y corregir el KDoc stale de `PlayerModule` ("sin autenticación") per Constitución V (unrequested) — hecho 2026-09-20
