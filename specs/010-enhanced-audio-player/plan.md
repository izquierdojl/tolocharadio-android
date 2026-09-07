# Implementation Plan: Enhanced Audio Player UX

**Branch**: `010-enhanced-audio-player` | **Date**: 2026-09-07 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/010-enhanced-audio-player/spec.md`

## Summary

Mejoras al reproductor de audio de TolochaRadio: (1) notificación nativa con controles play/pause/stop que persiste al pausar, (2) panel de información completa de emisora al tocar el logo, (3) persistencia del mini-player al salir/volver a la app, y (4) consistencia visual profesional con la paleta Tema Tolocha.

El proyecto ya usa Media3 ExoPlayer + MediaSessionService con `DefaultMediaNotificationProvider`. La notificación nativa ya existe parcialmente; se requiere personalización para controles stop y comportamiento de pausa. El mini-player ya persiste entre pantallas (spec 004); el bug de pérdida al salir/volver se debe a ViewModel lifecycle. El panel de información es un componente nuevo.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17

**Primary Dependencies**: Media3 1.4.1 (ExoPlayer, Session, UI, DataSource-OkHttp), Compose BOM 2025.01.00, Hilt 2.60.1, Coil 2.6.0, Retrofit 2.11.0, Room 2.7.2

**Storage**: Room (caché local), DataStore (ajustes/tema/baseUrl)

**Testing**: JUnit + Turbine + MockK, Compose Test, Detekt + ktlint

**Target Platform**: Android 8+ (API 26), minSdk 26

**Project Type**: Mobile app (Android)

**Performance Goals**: Transiciones < 300ms, arranque frío < 2s

**Constraints**: HTTPS-only, Bearer auth en memoria, sin DRM, self-hosted backend

**Scale/Scope**: App de consumo personal, ~6 pantallas principales

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. MVVM + Clean por Capas | ✅ | PlayerViewModel (domain) → PlayerUi (UI) → PlaybackRepo (data). Cambios se limitan a estas 3 capas. |
| II. Kotlin-First, Compose M3, Media3 | ✅ | Toda la UI es Compose M3. Player usa Media3 ExoPlayer + MediaSessionService. Sin dependencias nuevas requeridas. |
| III. Test-First | ✅ | Tests unitarios para PlayerViewModel (toggle, stop, retry) + Compose tests para mini-player y station-info-sheet. |
| IV. Streaming Robusto | ✅ | PlayerState sellado ya implementado. Notificación persiste en pausa (spec clarification). Error states con retry. |
| V. Simplicidad Modular (YAGNI) | ✅ | Sin nuevos módulos. Cambios dentro del módulo `app` existente. Sin nuevas dependencias externas. |

**Gate Result**: PASS — sin violaciones.

## Project Structure

### Documentation (this feature)

```text
specs/010-enhanced-audio-player/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── station-info-ui.md
└── tasks.md             # Phase 2 output (/speckit.tasks - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── feature/player/
│   ├── PlayerViewModel.kt          # MOD: state persistence fix, expose station for info panel
│   ├── PlayerUi.kt                 # MOD: logo tap → station info sheet, mini-player persistence
│   ├── RadioPlaybackService.kt     # MOD: custom MediaNotification.Provider or config
│   ├── StationInfoSheet.kt         # NEW: bottom sheet with full station details
│   └── PanelHelpers.kt             # MOD: minor adjustments
├── core/ui/components/
│   └── StationArtwork.kt           # MOD: clickable logo with proper semantics
└── di/
    └── PlayerModule.kt             # No changes expected
```

**Structure Decision**: Cambios concentrados en `feature/player/` (carpeta existente). Un nuevo archivo `StationInfoSheet.kt` para el panel de información. Sin nuevos módulos.
