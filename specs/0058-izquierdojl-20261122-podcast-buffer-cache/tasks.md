# Tasks: Buffer profundo y caché en disco para episodios

**Input**: Design documents from `/specs/0058-izquierdojl-20261122-podcast-buffer-cache/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: obligatorios para `domain`, `data` y ViewModel (constitución III,
regla Red-Green). Se escriben antes de la implementación y deben fallar.

**Organization**: tareas agrupadas por historia de usuario para que cada una sea
implementable y verificable por separado.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: paralelizable (ficheros distintos, sin dependencias pendientes)
- **[Story]**: US1, US2, US3
- Rutas exactas en cada tarea

## Phase 1: Setup

**Purpose**: confirmar el punto de partida y la rama.

- [x] T001 Confirmar rama `0058-izquierdojl-20261122-podcast-buffer-cache` y baseline verde con `./gradlew testDebugUnitTest detekt ktlintCheck`
- [x] T002 [P] Verificar que el fix del bug 0057 está presente en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt` (estados `WaitingForNetwork`/`Reconnecting`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: entidades y contratos que necesitan las tres historias.

**⚠️ CRITICAL**: ninguna historia puede empezar antes de completar esta fase.

- [x] T003 [P] Crear el enum `ContentKind` con los valores `ContentKind.LIVE` y `ContentKind.VOD` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/ContentKind.kt`
- [x] T004 [P] Crear `EpisodeBufferSpec` según `data-model.md` con `EpisodeBufferSpec.minBufferMs = 120_000`, `EpisodeBufferSpec.maxBufferMs = 240_000`, `EpisodeBufferSpec.bufferForPlaybackMs = 2_000` y `EpisodeBufferSpec.bufferForPlaybackAfterRebufferMs = 2_500`, validando `maxBufferMs >= minBufferMs` y `minBufferMs >= bufferForPlaybackMs` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/EpisodeBufferSpec.kt`
- [x] T005 [P] Crear `EpisodeCachePolicy` y `NetworkKind` según `data-model.md` con `EpisodeCachePolicy.limitUnmeteredBytes = 524_288_000`, `EpisodeCachePolicy.limitMeteredBytes = 52_428_800` y `EpisodeCachePolicy.prefetchAheadMeteredMs = 300_000`, validando `limitUnmeteredBytes >= limitMeteredBytes` y los valores `NetworkKind.UNMETERED`/`NetworkKind.METERED` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeCachePolicy.kt`
- [x] T006 [P] Crear `EpisodeCacheEntry` con `EpisodeCacheEntry.episodeId > 0`, `EpisodeCacheEntry.bytes >= 0` y `EpisodeCacheEntry.lastUsedAt >= 0` en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/podcasts/EpisodeCacheEntry.kt`
- [x] T007 [P] Crear `EpisodeCacheStats` con `EpisodeCacheStats.sizeBytes >= 0` y `EpisodeCacheStats.entryCount >= 0` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeCachePolicy.kt`, y `PrefetchState` con `PrefetchState.episodeId`, `PrefetchState.cachedBytes`, `PrefetchState.targetBytes` y `PrefetchState.running` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePrefetchPolicy.kt`
- [x] T008 Añadir `isMetered: StateFlow<Boolean>` a `ConnectivityMonitor` y su implementación en `AndroidConnectivityMonitor` (capabilities, roaming y Ahorro de datos) en `app/src/main/java/com/izquierdojl/tolocharadio/core/network/` y en `app/src/test/java/com/izquierdojl/tolocharadio/core/network/FakeConnectivityMonitor.kt`
- [x] T009 Extender `PlaybackPlayerFactory` con `create(profile, kind)` y `PlaybackEngine` con `ensureKind(kind: ContentKind)` y `takePendingRelease()` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackPlayerFactory.kt` y `PlaybackEngine.kt`
- [x] T010 Centralizar en `RadioPlaybackService` el cambio de sesión al recibir `engine.playerFlow` (actualizar `RecoveryAwarePlayer`, `MediaSession.setPlayer` y liberar el player pendiente) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`

**Checkpoint**: entidades, contratos y motor listos; las historias pueden empezar.

---

## Phase 3: User Story 1 — Buffer profundo (Priority: P1) 🎯 MVP

**Goal**: un episodio aguanta 3 minutos sin cobertura sin cortar el audio, sin
cambiar la latencia de la radio.

**Independent Test**: reproducir un episodio en streaming, llenar el buffer,
activar modo avión 3 minutos y comprobar que el audio sigue.

### Tests for User Story 1 (write first, must fail)

- [x] T011 [P] [US1] Test de `EpisodeBufferSpec` (invariantes y valores, incluido `maxBufferMs` dentro del objetivo de 8 MB) en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/EpisodeBufferSpecTest.kt`
- [x] T012 [P] [US1] Test de `PlaybackEngine.ensureKind`: `VOD` usa `EpisodeBufferSpec`, `LIVE` conserva el `BufferProfile` y el cambio es idempotente, en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackEngineKindTest.kt`

### Implementation for User Story 1

- [x] T013 [US1] Llamar `engine.ensureKind(ContentKind.VOD)` en `EpisodePlaybackController.play()` antes de crear la fuente, y `ensureKind(ContentKind.LIVE)` en `LocalPlaybackStarter.start()` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/LocalPlaybackStarter.kt`
- [x] T014 [US1] Mantener el `BufferProfile` del usuario para `ContentKind.LIVE` y no aplicarlo a `ContentKind.VOD` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackEngine.kt`
- [x] T015 [US1] Verificar que un cambio de perfil de buffer durante un episodio no reinicia la radio en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`

**Checkpoint**: US1 funcional y medible (SC-001, SC-002, SC-004, SC-006).

---

## Phase 4: User Story 2 — Caché y precarga (Priority: P2)

**Goal**: el audio ya escuchado se conserva en disco y, en Wi-Fi, se precarga el
episodio completo; en móvil, 5 minutos por delante y 50 MB.

**Independent Test**: escuchar 5 minutos, activar modo avión y retroceder 3
minutos; con Wi-Fi, esperar la precarga y reproducir el episodio completo sin red.

### Tests for User Story 2 (write first, must fail)

- [x] T016 [P] [US2] Test de `QuotaCacheEvictor` (LRU y recorte al bajar de 500 MB a 50 MB) en `app/src/test/java/com/izquierdojl/tolocharadio/data/local/podcasts/QuotaCacheEvictorTest.kt`
- [x] T017 [P] [US2] Test de `EpisodeMediaSourceFactory` (clave `episode-<id>`, `FLAG_IGNORE_CACHE_ON_ERROR`, sin caché para `content://`) en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/EpisodeMediaSourceFactoryTest.kt`
- [x] T018 [P] [US2] Test de `EpisodePrefetchPolicy` (completo en no medida, 5 min en medida, cancelación) en `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePrefetchPolicyTest.kt`
- [x] T036 [P] [US2] Test de `EpisodeCachePolicy` (`limitUnmeteredBytes >= limitMeteredBytes`, `prefetchAheadMeteredMs <= 300_000` y `NetworkKind`) en `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeCachePolicyTest.kt`

### Implementation for User Story 2

- [x] T019 [US2] Implementar `QuotaCacheEvictor` con límite dinámico y orden LRU usando `EpisodeCacheEntry.lastUsedAt` en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/podcasts/QuotaCacheEvictor.kt`
- [x] T020 [US2] Implementar la interfaz `EpisodeCache` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeCache.kt` y su implementación `SimpleCacheEpisodeCache` con `SimpleCache` singleton, `factoryFor(episodeId)`, `passthrough()`, `clear()` y `stats` (poblando `EpisodeCacheStats.sizeBytes` y `EpisodeCacheStats.entryCount`), aplicando `EpisodeCachePolicy.limitFor(NetworkKind)`, ejecutando la E/S en `Dispatchers.IO` (FR-011) y degradando a `passthrough()` si `SimpleCache` no se puede crear (U1) en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/podcasts/SimpleCacheEpisodeCache.kt`
- [x] T021 [US2] Crear `EpisodeMediaSourceFactory` con `CacheDataSource` y clave `episode-<EpisodeCacheEntry.episodeId>`, sin caché para `content://`/`file://` y sin persistir cabeceras ni tokens (FR-010) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/EpisodeMediaSourceFactory.kt`
- [x] T022 [US2] Usar `EpisodeMediaSourceFactory` en `EpisodePlaybackController.play()` y en la recuperación, actualizando `EpisodeCacheEntry.lastUsedAt` y manteniendo el reporte de progreso (FR-013) en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt`
- [x] T023 [US2] Implementar `EpisodePrefetchPolicy` con el objetivo de `EpisodePrefetchPolicy.prefetchAheadMeteredMs = 300_000` en red medida y episodio completo en no medida en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePrefetchPolicy.kt`
- [x] T024 [US2] Implementar la interfaz `EpisodePrefetch` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePrefetch.kt` y su implementación `CacheWriterEpisodePrefetch` con `CacheWriter` en `Dispatchers.IO`, publicando `PrefetchState.episodeId`, `PrefetchState.cachedBytes`, `PrefetchState.targetBytes` y `PrefetchState.running`, y cancelando al pausar/parar/cerrar (FR-011, FR-018) en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/podcasts/CacheWriterEpisodePrefetch.kt`
- [x] T025 [US2] Cablear la precarga en `EpisodePlaybackController` (inicio al reproducir, `update(positionMs)` en el ticker, cancelación en pausa/stop/error) en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt`
- [x] T026 [US2] Reaccionar a `ConnectivityMonitor.isMetered` para reajustar límite y precarga sin cortar el audio en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/podcasts/SimpleCacheEpisodeCache.kt` y `CacheWriterEpisodePrefetch.kt`

**Checkpoint**: US2 funcional (SC-003, SC-005, SC-008, SC-009).

---

## Phase 5: User Story 3 — Límites y control (Priority: P3)

**Goal**: ver y liberar el espacio de caché desde Ajustes, con límites visibles.

**Independent Test**: abrir Ajustes, ver el espacio ocupado, pulsar «Liberar
espacio» y comprobar que baja a 0 sin cortar la reproducción.

### Tests for User Story 3 (write first, must fail)

- [x] T027 [P] [US3] Test del ViewModel de Ajustes: `EpisodeCacheStats.sizeBytes` visible, límite según `NetworkKind` y `clear()` invocado en `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsCacheTest.kt`

### Implementation for User Story 3

- [x] T028 [US3] Añadir `CacheSettingsUi` y las acciones `onClearCache` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt`
- [x] T029 [US3] Añadir la sección plegable «Caché de episodios» con espacio, límites 500/50 MB y «Liberar espacio» en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt`
- [x] T030 [P] [US3] Añadir textos de la sección en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsLabels.kt`
- [x] T031 [US3] Recortar la caché al límite de la red activa al cambiar de no medida a medida en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/podcasts/SimpleCacheEpisodeCache.kt`

**Checkpoint**: las tres historias funcionan por separado.

---

## Phase 6: Polish & Cross-Cutting

- [x] T032 [P] Añadir logs estructurados `EpisodePrefetch` y `EpisodeCache` sin URLs ni PII en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/podcasts/CacheWriterEpisodePrefetch.kt` y `SimpleCacheEpisodeCache.kt`
- [x] T033 Ejecutar `./gradlew testDebugUnitTest detekt ktlintCheck lintDebug` y corregir lo que falle
- [x] T034 Ejecutar `quickstart.md` en dispositivo: escenarios 1–6 y regresión de radio, descargas y Cast
- [x] T035 [P] Actualizar `docs/RELEASE.md` si la feature cambia el comportamiento de reproducción o almacenamiento — sin cambios necesarios; documentación de usuario en T039 (2026-11-22)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (T001–T002)**: sin dependencias.
- **Foundational (T003–T010)**: depende de Setup; bloquea las tres historias.
- **US1 (T011–T015)**: depende de Foundational; es el MVP.
- **US2 (T016–T026)**: depende de Foundational y de `ContentKind`/motor de US1.
- **US3 (T027–T031)**: depende de US2 (`EpisodeCache`, `EpisodeCacheStats`).
- **Polish (T032–T035)**: depende de las historias que se vayan a entregar.

### Within Each Story

1. Tests primero (deben fallar).
2. Entidades/modelos.
3. Servicio/caché.
4. Integración en el controlador/UI.
5. Verificación de la historia.

### Parallel Opportunities

- T003–T007 en paralelo (ficheros distintos).
- T008 y T009–T010 en paralelo tras T003.
- T011 y T012 en paralelo; T013–T015 secuenciales.
- T016–T018 en paralelo; T019–T021 en paralelo; T022–T026 secuenciales.
- T027 y T030 en paralelo; T028–T029 secuenciales.

## Parallel Example: User Story 2

```text
T016 QuotaCacheEvictorTest.kt
T017 EpisodeMediaSourceFactoryTest.kt
T018 EpisodePrefetchPolicyTest.kt
```

## Implementation Strategy

1. **MVP**: Setup + Foundational + US1. Entrega el margen de 3 minutos sin tocar
   la radio.
2. **Incremento 2**: US2 (caché + precarga). Es el grueso del valor offline.
3. **Incremento 3**: US3 (ajustes y control de espacio).
4. **Cierre**: Polish + quickstart en dispositivo.

## Notes

- Cada tarea de test debe fallar antes de su implementación (Red-Green).
- No se añaden dependencias nuevas: `SimpleCache`, `CacheDataSource` y
  `CacheWriter` vienen con Media3.
- La caché no guarda credenciales ni URLs firmadas; la clave es el episodio.

---

## Phase 7: Convergence

- [x] T037 [P] [US2] Añadir tests de degradación y `passthrough` de `SimpleCacheEpisodeCache` y del ciclo de `CacheWriterEpisodePrefetch` por Constitution III (`partial`)
- [x] T038 [P] [US2] Extraer la decisión de red medida a una política pura y testearla (NOT_METERED, roaming y Ahorro de datos) por FR-017 (`partial`)
- [x] T039 Actualizar `docs/uso.md` con la caché de episodios y la escucha sin conexión por T035 (`partial`)
