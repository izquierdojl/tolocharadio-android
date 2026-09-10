# Implementation Plan: Temporizador de apagado (Sleep Timer)

**Branch**: `014-sleep-timer` | **Date**: 2026-09-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/014-sleep-timer/spec.md`

## Summary

Añadir un temporizador de apagado (sleep timer) a la barra superior de la app. El usuario selecciona una duración predefinida (15, 30, 45, 60, 90 min), la reproducción se detiene automáticamente al expirar. Funcionalidad local sin backend, integrada con el MediaSessionService existente.

## Technical Context

**Language/Version**: Kotlin (null-safe, compileSdk declarado en Gradle)

**Primary Dependencies**: Jetpack Compose + Material3, Media3 (ExoPlayer + MediaSessionService), Hilt, Coroutines + Flow

**Storage**: N/A — el temporizador es transitorio (no persiste entre sesiones)

**Testing**: JUnit + kotlinx-coroutines-test + Turbine + MockK, Compose Test

**Target Platform**: Android (minSdk 26)

**Project Type**: Mobile app (Android)

**Performance Goals**: Activación en <3s (2 pulsaciones), detención en <5s tras expirar, indicador actualizado 1 vez/minuto (sin segundos) para minimizar CPU/batería

**Constraints**: Sin dependencias nuevas, sin comunicación backend, parada silenciosa (sin notificación/sonido)

**Scale/Scope**: Feature local de un solo usuario, un botón en la barra superior + lógica de temporización

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Justificación |
|-----------|--------|---------------|
| I. MVVM + Clean por capas | ✅ | ViewModel + UseCase + UI separados |
| II. Kotlin/Compose/Media3 | ✅ | Usa Compose M3 para UI, corrutinas para timer |
| III. Test-First | ✅ | Tests unitarios para UseCase y ViewModel |
| IV. Streaming Robusto | ✅ | Timer se integra con stop() existente, sin riesgo de crash |
| V. Simplicidad (YAGNI) | ✅ | Sin dependencias nuevas, sin módulos adicionales, sin persistencia |

**Resultado**: Sin violaciones. Todas las puertas pasan.

## Project Structure

### Documentation (this feature)

```text
specs/014-sleep-timer/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── domain/
│   └── SleepTimerUseCase.kt              # NEW: lógica del temporizador
├── feature/
│   └── player/
│       ├── SleepTimerViewModel.kt         # NEW: estado del temporizador
│       └── SleepTimerButton.kt            # NEW: botón + menú Compose
└── core/
    └── ui/
        └── navigation/
            └── TolochaNavGraph.kt         # MODIFIED: añadir botón en TopAppBar

app/src/test/java/com/izquierdojl/tolocharadio/
├── domain/
│   └── SleepTimerUseCaseTest.kt           # NEW: tests unitarios
└── feature/
    └── player/
        └── SleepTimerViewModelTest.kt     # NEW: tests unitarios
```

**Structure Decision**: Feature integrada en los paquetes existentes (`domain/`, `feature/player/`). No se crea un paquete nuevo `feature/sleeptimer/` porque el temporizador es una extensión del player, no una sección independiente.

## Complexity Tracking

> No se requiere: sin violaciones de constitución.
