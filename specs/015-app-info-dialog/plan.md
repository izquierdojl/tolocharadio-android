# Implementation Plan: Diálogo de información de la aplicación

**Branch**: `015-app-info-dialog` | **Date**: 2026-09-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/015-app-info-dialog/spec.md`

## Summary

Añadir un diálogo modal en la pantalla de Configuración que muestre datos generales de la aplicación: nombre, versión, enlace al repositorio, desarrollador y licencia. Funcionalidad local sin backend, puramente informativa con enlace interactivo al repositorio.

## Technical Context

**Language/Version**: Kotlin (null-safe, compileSdk declarado en Gradle)

**Primary Dependencies**: Jetpack Compose + Material3, Hilt

**Storage**: N/A — datos estáticos derivados del build o hardcodeados

**Testing**: JUnit + MockK, Compose Test

**Target Platform**: Android (minSdk 26)

**Project Type**: Mobile app (Android)

**Performance Goals**: Apertura del diálogo en <3s (2 pulsaciones desde Configuración)

**Constraints**: Sin dependencias nuevas, sin comunicación backend, datos estáticos

**Scale/Scope**: Feature local de un solo usuario, un elemento en Configuración + diálogo modal

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Justificación |
|-----------|--------|---------------|
| I. MVVM + Clean por capas | ✅ | ViewModel existente + UI separada |
| II. Kotlin/Compose/Material3 | ✅ | Usa Compose M3 para UI |
| III. Test-First | ✅ | Tests unitarios para ViewModel y Compose |
| IV. Streaming Robusto | ✅ | No afecta streaming; es UI informativa |
| V. Simplicidad (YAGNI) | ✅ | Sin dependencias nuevas, sin módulos adicionales |

**Resultado**: Sin violaciones. Todas las puertas pasan.

## Project Structure

### Documentation (this feature)

```text
specs/015-app-info-dialog/
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
├── feature/
│   └── settings/
│       ├── SettingsScreen.kt              # MODIFIED: añadir elemento "Acerca de"
│       └── SettingsViewModel.kt           # MODIFIED: añadir lógica del diálogo
└── core/
    └── ui/
        └── components/
            └── AppInfoDialog.kt           # NEW: diálogo modal Compose

app/src/test/java/com/izquierdojl/tolocharadio/
└── feature/
    └── settings/
        └── SettingsViewModelTest.kt       # MODIFIED: tests para info dialog
```

**Structure Decision**: Feature integrada en el paquete existente `feature/settings/`. No se crea un paquete nuevo porque el diálogo es una extensión de Configuración, no una sección independiente. El componente `AppInfoDialog` se coloca en `core/ui/components/` por reutilizabilidad.

## Complexity Tracking

> No se requiere: sin violaciones de constitución.
