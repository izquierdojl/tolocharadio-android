# Implementation Plan: Emisoras recientes en los accesos directos del icono

**Branch**: `0018-jlizquierdo-20260910-app-shortcut-recent-stations` | **Date**: 2026-09-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0018-jlizquierdo-20260910-app-shortcut-recent-stations/spec.md`

**Note**: `setup-plan.ps1` no es ejecutable en este entorno (bug: `Get-Python3Command` elige el stub `python3` de Microsoft Store, exit 9009, y el resolver lanza `Invalid preset manifest`). Este `plan.md` se creó copiando `.specify/templates/plan-template.md` y rellenándolo a mano; ver research.md R0.

## Summary

Publicar hasta 4-5 accesos directos dinámicos del lanzador con las últimas emisoras únicas del historial (incluidas personalizadas), mantenerlos sincronizados con la app en primer plano, reproducir directamente al pulsar abriendo el reproductor a pantalla completa, con caché local como fuente offline, limpieza al cerrar sesión/cambiar de cuenta y manejo de sesión caducada.

Enfoque técnico: nuevo paquete `core/shortcuts/` (publisher Android sobre `ShortcutManagerCompat` + coordinador de sincronización) y `domain/shortcuts/` (casos de uso puros), reutilizando `HistoryRepo`/`SessionManager`/`PlayerViewModel` existentes. Sin dependencias nuevas, sin cambios de esquema Room y sin shortcuts estáticos.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JDK 17

**Primary Dependencies**: Jetpack Compose (BOM 2025.01.00) + Material3, Navigation Compose 2.7.7, Hilt 2.60.1, Media3 1.4.1 (ExoPlayer + MediaSessionService), Room 2.7.2, DataStore 1.1.1 + EncryptedSharedPreferences, Coil 2.6.0, Coroutines 1.8.1, `androidx.core:core-ktx` 1.10.1 (`ShortcutManagerCompat`, ya incluida), ProcessPhoenix (reinicio de proceso al cambiar servidor)

**Storage**: Room (`TolochaDb` v6, tabla `CachedHistoryEntry` de solo lectura) y DataStore/EncryptedSharedPreferences (sesión/baseUrl). Esta feature NO añade tablas, entidades ni migraciones; los accesos directos son estado derivado del historial.

**Testing**: JUnit4 4.13.2, MockK 1.13.12, Turbine 1.1.0, `kotlinx-coroutines-test`, Compose UI test (`createComposeRule`) y androidTest Room. CI: `./gradlew assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug` (`.github/workflows/android.yml`)

**Target Platform**: Android, `minSdk 26` (app shortcuts desde API 25), `targetSdk`/`compileSdk` 37; lanzador del sistema decide cuántos slots muestra (típico 4-5)

**Project Type**: mobile-app (un único módulo Gradle `app`, organizado por capas `core/`, `data/`, `domain/`, `feature/`)

**Performance Goals**: pulsar shortcut → reproducción en ≤ 5 s (SC-002); cambio de historial → menú actualizado ≤ 5 s (SC-003/SC-004); refresco de historial y publicación de shortcuts fuera del hilo principal

**Constraints**: actualizaciones de shortcuts solo garantizadas en foreground (límite de plataforma); máx. `getMaxShortcutCountPerActivity()` ≤ 5 slots (reservando acciones del sistema); extras del intent sin PII (solo id/nombre público de emisora); funcionar offline con la última lista conocida; sin nuevas dependencias ni módulos Gradle

**Scale/Scope**: ≤ 5 accesos publicados por cuenta; historial de decenas a cientos de entradas (spec 005); ~6 clases nuevas en `core/shortcuts`/`domain/shortcuts`, 1 caso de uso, ~4 archivos de test nuevos + 1 ampliado

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento | Evidencia |
|-----------|--------------|-----------|
| I. MVVM + Clean por capas | PASS | Reglas de selección/dedupe/límite en `domain/shortcuts` (puro Kotlin, sin Android); orquestación y acceso a datos en `core/shortcuts` (coordinador), sin lógica en composables; Hilt para todas las clases nuevas; un solo módulo `app` |
| II. Kotlin-First, Compose M3 y Media3 | PASS | Kotlin + Compose; la reproducción reutiliza `PlayerViewModel`/Media3 sin tocar el pipeline de streaming; la apertura del reproductor reutiliza `FullPlayerSheet` Compose; sin XML/Views nuevos; sin dependencias nuevas |
| III. Calidad Test-First (NON-NEGOTIABLE) | PASS | Tests unitarios JUnit + Turbine + MockK para casos de uso puros, coordinador (publisher fake), parser de intents y `PlayerViewModel`; lógica de plataforma reducida a wrapper delgado; tests escritos antes de la implementación (Red-Green) |
| IV. Streaming robusto y manejo de errores | PASS | Resultado tipado `ResolveShortcutLaunchUseCase` (Play/GoLogin/Unavailable), reutiliza precheck `playback/:id/status` y `DomainError.userMessage()` en español; sin `try/catch` genéricos; offline cae a la caché de historial (FR-015) |
| V. Simplicidad modular (YAGNI) | PASS | Sin tablas/migraciones nuevas, sin WorkManager, sin static shortcuts, sin módulo Gradle, sin dependencias nuevas; una sola pasada de publicación de iconos |

**Post-Phase 1 re-check**: PASS — el diseño (6 clases + 2 casos de uso + tests; sin persistencia nueva; iconos best-effort con fallback) no introduce violaciones ni requiere `Complexity Tracking`.

## Project Structure

### Documentation (this feature)

```text
specs/0018-jlizquierdo-20260910-app-shortcut-recent-stations/
├── plan.md              # Este archivo (/speckit.plan)
├── research.md          # Fase 0 (/speckit.plan)
├── data-model.md        # Fase 1 (/speckit.plan)
├── quickstart.md        # Fase 1 (/speckit.plan)
├── contracts/           # Fase 1 (/speckit.plan)
│   ├── shortcut-intent.md
│   └── shortcut-publication.md
├── checklists/
│   └── requirements.md  # Calidad de spec (ya existente)
└── tasks.md             # Fase 2 (/speckit.tasks — NO creado aquí)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── MainActivity.kt                          # + parsea intent de shortcut (onCreate/onNewIntent) → PendingShortcutHolder
├── TolochaApp.kt                            # + registra ProcessLifecycleOwner → ShortcutSyncCoordinator
├── core/shortcuts/                          # NUEVO (infra transversal de plataforma)
│   ├── ShortcutPublisher.kt                 # interfaz: publish(List<ShortcutStation>), clear(), maxSlots()
│   ├── AndroidShortcutPublisher.kt          # ShortcutManagerCompat + IconCompat + favicon best-effort (Coil)
│   ├── ShortcutSpec.kt                      # modelo puro de publicación + factory testable
│   ├── ShortcutSyncCoordinator.kt           # observa historial/sesión/foreground → publisher
│   ├── ShortcutIntents.kt                   # action/extras + parser puro
│   └── PendingShortcutHolder.kt             # StateFlow<stationId?> MainActivity → NavGraph
├── core/ui/navigation/TolochaNavGraph.kt    # + consume pending: resuelve, reproduce y abre full player
├── domain/shortcuts/
│   ├── BuildShortcutStationsUseCase.kt      # NUEVO puro: dedupe/ordena/cap → List<ShortcutStation>
│   └── ResolveShortcutLaunchUseCase.kt      # NUEVO puro: Play/GoLogin/Unavailable
├── feature/player/PlayerViewModel.kt        # + fullPlayerVisible/openFullPlayer/closeFullPlayer
├── feature/player/PlayerUi.kt               # FullPlayerSheet controlado por el VM (hoy inalcanzable)
├── data/repo/HistoryRepo.kt                 # reutilizado (items StateFlow + list() con fallback a caché)
├── data/repo/StationsRepo.kt                # reutilizado (detail(id) para shortcuts huérfanos)
└── data/repo/servers/ServerRepository.kt    # + clearNow de shortcuts al cambiar de instancia

app/src/test/java/com/izquierdojl/tolocharadio/
├── domain/shortcuts/BuildShortcutStationsUseCaseTest.kt
├── domain/shortcuts/ResolveShortcutLaunchUseCaseTest.kt
├── core/shortcuts/ShortcutSyncCoordinatorTest.kt
├── core/shortcuts/ShortcutIntentsTest.kt
├── core/shortcuts/ShortcutSpecFactoryTest.kt
└── feature/player/PlayerViewModelTest.kt     # ampliado (full player visible)
```

**Structure Decision**: se mantiene el único módulo `app` y la organización por capas existente. `domain/shortcuts` contiene las reglas puras (selección/dedupe/límite y resolución del lanzamiento); `core/shortcuts` contiene la infraestructura Android (publisher, coordinador, parser de intents, holder), siguiendo el patrón de `core/session` (p. ej. `SessionRestorer`). No se extrae módulo Gradle nuevo (constitución I y V).

## Complexity Tracking

> Sin violaciones de la constitución. No aplica.
