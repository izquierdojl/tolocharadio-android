# Implementation Plan: Buffer profundo y caché en disco para episodios

**Branch**: `0058-izquierdojl-20261122-podcast-buffer-cache` | **Date**: 2026-11-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0058-izquierdojl-20261122-podcast-buffer-cache/spec.md`

## Summary

Se separa el buffer del reproductor por tipo de contenido: la radio en directo
conserva su perfil actual y los episodios usan un `LoadControl` de 60 s–240 s.
Encima de eso, se añade una caché en disco con `SimpleCache` de Media3, con
límites de 500 MB en redes no medidas y 50 MB en medidas, y una precarga
adaptativa: episodio completo en Wi-Fi, 5 minutos por delante en datos móviles.

El cambio de buffer obliga a intercambiar el `ExoPlayer` al pasar de radio a
episodio (Media3 no permite cambiar `LoadControl` en caliente). Se reutiliza el
intercambio que ya existe para los perfiles de buffer, centralizando en el
servicio la actualización de `MediaSession`.

Depende del fix del bug 0057 (recuperación de red del VOD), ya aplicado.

## Technical Context

**Language/Version**: Kotlin 2.x, JVM 17

**Primary Dependencies**: AndroidX Media3 1.11.0 (`media3-exoplayer`,
`media3-datasource`, `media3-session`, `media3-cast`), Hilt, Compose Material 3,
DataStore, Room, OkHttp 4.12

**Storage**: `SimpleCache` de Media3 en `cacheDir/episode-cache`; DataStore para
los límites de caché; Room existente intacto

**Testing**: JUnit 4, `kotlinx-coroutines-test`, Turbine, MockK; Compose UI solo
para flujos críticos de Ajustes

**Target Platform**: Android 26–37 (`minSdk 26`, `compileSdk`/`targetSdk` 37)

**Project Type**: Aplicación móvil Android, módulo Gradle único (`app`)

**Performance Goals**:
- Arranque de episodio < 3 s con red normal.
- Pérdida de cobertura de 3 min sin corte (con buffer lleno).
- Latencia de radio sin cambios (±2 s).
- Buffer en RAM ≤ 8 MB.

**Constraints**:
- Sin dependencias nuevas (Media3 ya incluye caché y `StandaloneDatabaseProvider`).
- Solo Media3; prohibido `MediaPlayer`/`VideoView`.
- La caché no guarda credenciales, tokens ni URLs firmadas.
- Sin PII en logs.
- Un único `SimpleCache` por proceso.

**Scale/Scope**: Un episodio activo a la vez; caché de ~10 episodios de 60 min
en el límite de 500 MB; 4 pantallas tocadas (Ajustes, reproductor de episodios,
servicio y motor).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Gate | Estado |
|-----------|------|--------|
| I. MVVM + Clean | Política y precarga en `domain`; caché en `data`; UI en `feature`; ViewModel sin `Context`; Hilt | PASS |
| II. Kotlin/Compose/Media3 | `SimpleCache`/`CacheDataSource`/`CacheWriter` son Media3; UI en Compose M3; sin XML | PASS |
| III. Test-First | Tests de `EpisodeBufferSpec`, `EpisodeCachePolicy`, `QuotaCacheEvictor`, `PlaybackEngine.ensureKind`, `EpisodePrefetchPolicy` y ViewModel de Ajustes; gates obligatorios | PASS |
| IV. Streaming robusto | Degradación a streaming si la caché falla (`FLAG_IGNORE_CACHE_ON_ERROR`); logs estructurados sin URLs ni PII; cancelación por red/pausa | PASS |
| V. Simplicidad (YAGNI) | Sin WorkManager, sin módulos nuevos, sin dependencias nuevas; se reutiliza el swap existente | PASS |

**Post-design re-check**: se mantiene PASS. El único punto delicado es el
intercambio de player, cubierto con tests y con liberación diferida.

## Project Structure

### Documentation (this feature)

```text
specs/0058-izquierdojl-20261122-podcast-buffer-cache/
├── plan.md              # Este archivo
├── research.md          # Decisiones R1–R10
├── data-model.md        # Entidades y restricciones
├── quickstart.md        # Validación manual y automática
├── contracts/
│   └── episode-cache.md # Contratos internos
├── checklists/
│   └── requirements.md  # Calidad de la spec
└── tasks.md             # /speckit.tasks (pendiente)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/network/
│   ├── ConnectivityMonitor.kt              # + isMetered
│   └── AndroidConnectivityMonitor.kt       # + capacidades y Ahorro de datos
├── domain/playback/
│   ├── ContentKind.kt                      # nuevo: LIVE | VOD
│   └── EpisodeBufferSpec.kt                # nuevo: 60 s / 4 min
├── domain/podcasts/
│   ├── EpisodeCachePolicy.kt               # nuevo: 500/50 MB, 5 min
│   ├── EpisodePrefetchPolicy.kt            # nuevo: objetivo por red
│   └── EpisodePrefetchManager.kt           # nuevo: CacheWriter
├── data/local/podcasts/
│   ├── EpisodeCache.kt                     # nuevo: SimpleCache + factories
│   ├── QuotaCacheEvictor.kt                # nuevo: LRU con límite dinámico
│   └── EpisodeCacheEntry.kt                # nuevo: modelo de entrada
├── feature/player/
│   ├── PlaybackEngine.kt                   # + ensureKind / pending release
│   ├── PlaybackPlayerFactory.kt            # + ContentKind
│   ├── EpisodeMediaSourceFactory.kt        # nuevo: CacheDataSource por episodio
│   └── RadioPlaybackService.kt             # switch de sesión + liberación
├── feature/podcasts/
│   ├── EpisodePlaybackController.kt        # ensureKind(VOD) + prefetch
│   └── EpisodePlayerViewModel.kt           # estado de precarga opcional
└── feature/settings/
    ├── SettingsViewModel.kt                # stats + clear
    ├── SettingsScreen.kt                   # sección Caché de episodios
    └── SettingsLabels.kt                   # textos

app/src/test/java/com/izquierdojl/tolocharadio/
├── domain/playback/EpisodeBufferSpecTest.kt
├── domain/podcasts/EpisodeCachePolicyTest.kt
├── domain/podcasts/EpisodePrefetchPolicyTest.kt
├── data/local/podcasts/QuotaCacheEvictorTest.kt
├── feature/player/PlaybackEngineKindTest.kt
├── feature/player/EpisodeMediaSourceFactoryTest.kt
└── feature/settings/SettingsCacheTest.kt
```

**Structure Decision**: se mantiene el módulo único y la separación
`ui → domain → data` de la constitución. La caché es infraestructura (`data`);
la política de red y de precarga es lógica de dominio pura y testeable.

## Implementation Strategy

### Slice A — Buffer VOD (P1, FR-001–FR-004, SC-001, SC-002, SC-004)

1. `ContentKind` y `EpisodeBufferSpec`.
2. `PlaybackPlayerFactory.create(profile, kind)`; `PlaybackEngine.ensureKind`.
3. `RadioPlaybackService` escucha `engine.playerFlow`, actualiza
   `RecoveryAwarePlayer` y `MediaSession.setPlayer`, y libera el player anterior.
4. `EpisodePlaybackController.play()` llama `ensureKind(VOD)` antes de cargar.
5. `LocalPlaybackStarter.start()` llama `ensureKind(LIVE)`.

### Slice B — Caché de lectura/escritura (P2, FR-005–FR-011, FR-013)

1. `ConnectivityMonitor.isMetered` + implementación Android + fake de test.
2. `EpisodeCachePolicy` y `EpisodeCacheEntry`.
3. `QuotaCacheEvictor` con límite dinámico y LRU.
4. `EpisodeCache` (SimpleCache singleton, `factoryFor(episodeId)`, `passthrough`,
   `clear`, `stats`).
5. `EpisodeMediaSourceFactory`: `CacheDataSource` para streaming, clave
   `episode-<id>`, sin caché para local.
6. `EpisodePlaybackController` usa la factory y actualiza `lastUsedAt`.

### Slice C — Precarga adaptativa (P2, FR-014–FR-018, SC-008, SC-009)

1. `EpisodePrefetchPolicy` (objetivo por red).
2. `EpisodePrefetchManager` con `CacheWriter`: inicio al reproducir, progreso,
   cancelación en pausa/parada/cambio/pérdida de red, reajuste al cambiar de red.
3. Cableado en `EpisodePlaybackController` y observación de `isMetered`.

### Slice D — Ajustes y control (P3, FR-012, SC-005)

1. Sección «Caché de episodios» con espacio ocupado, límites y «Liberar espacio».
2. `SettingsViewModel` con stats y `clear`.
3. Test de ViewModel y de la sección sin Hilt.

### Slice E — Validación

Ejecutar `quickstart.md` en dispositivo: buffer, radio, caché, precarga,
móvil/Wi-Fi y ajustes.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| Intercambio de `ExoPlayer` al cambiar de contenido | `DefaultLoadControl` no se puede cambiar en caliente y la radio no puede heredar 4 min de latencia | Dos players vivos duplican renderers, hilos y memoria, y tocan volumen/Cast/sesión en todos los caminos |
| `QuotaCacheEvictor` propio | El límite depende de la red y cambia en caliente (500/50 MB) | `LeastRecentlyUsedCacheEvictor` fija el límite en el constructor |
| Caché en `cacheDir` | No duplica la política de descargas ni crece sin control | `filesDir` haría permanente un contenido que el sistema puede reproducir de nuevo |

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Carrera entre el swap y `MediaSession.setPlayer` | El servicio centraliza el cambio de sesión; el player anterior se libera después (`takePendingRelease`) |
| Caché corrupta o incompleta | `FLAG_IGNORE_CACHE_ON_ERROR` y clave por episodio |
| URL firmada caducada durante una espera larga | La clave no depende de la URL; el bug 0057 ya reanuda y se puede reemitir en un follow-up |
| Consumo de datos en móvil | Límite de 50 MB y 5 min; roaming y Ahorro de datos cuentan como medidos |
| Batería por precarga | Solo mientras el servicio está vivo y el episodio suena; se cancela al pausar |
| `SimpleCache` duplicado | Se crea como singleton de Hilt; nunca por episodio |
