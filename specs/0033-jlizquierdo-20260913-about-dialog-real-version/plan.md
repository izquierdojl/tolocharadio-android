# Implementation Plan: Diálogo "Acerca de" con versión real y detalles ampliados

**Branch**: `0033-jlizquierdo-20260913-about-dialog-real-version` | **Date**: 2026-09-13 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0033-jlizquierdo-20260913-about-dialog-real-version/spec.md`

## Summary

Corregir el diálogo "Acerca de" para que muestre la **versión real** del build (hoy fija en `1.0` en `AppInfo`) y ampliarlo con metadatos de la compilación (nº de compilación, identificador de la aplicación, tipo de build) y la **configuración activa** (tema, pantalla de arranque y alias del servidor activo). Añadir una acción **"Copiar información"** que vuelca el bloque completo al portapapeles y confirma con un **snackbar** sin cerrar el diálogo. Todo local, sin backend ni dependencias nuevas.

Enfoque técnico: `SettingsViewModel` compone la entidad `AppInfo` a partir de (a) un nuevo value object `AppBuildInfo` provisto por Hilt desde `BuildConfig`, (b) los flujos ya existentes de `InstancePrefs` (tema, pantalla de arranque) y (c) `GetServersUseCase` (alias del servidor activo). El texto copiable se genera con una función pura y testeable. El diálogo pasa de `AlertDialog` a una ventana `Dialog` a pantalla completa con `SnackbarHost` interno para que el aviso sea visible por encima del scrim sin cerrar el diálogo.

## Technical Context

**Language/Version**: Kotlin 2.x, JVM target 17, `minSdk` 26 / `targetSdk` 37 (según `app/build.gradle.kts`)

**Primary Dependencies**: Jetpack Compose + Material 3, Hilt (DI), DataStore Preferences (`InstancePrefs`), Room (`ServerDao`/`ServerRepository` vía `GetServersUseCase`), `BuildConfig` (con `buildConfig = true`)

**Storage**: N/A — solo lectura de preferencias y base de datos ya existentes; la feature no persiste datos nuevos

**Testing**: JUnit + MockK + Turbine (unit) para `SettingsViewModel` y el formateador puro; Compose UI test opcional para el flujo crítico (abrir → copiar → snackbar)

**Target Platform**: Android (móvil)

**Project Type**: Mobile app (Android), módulo único `app`

**Performance Goals**: Apertura del diálogo inmediata (<1 s percibido); el copiado es síncrono; sin operaciones de red

**Constraints**: Sin dependencias nuevas; sin comunicación con backend; sin credenciales, tokens ni PII en lo mostrado/copiado; reutilizar ajustes y repositorios existentes

**Scale/Scope**: 1 pantalla (Configuración) y 1 diálogo; un usuario local

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Justificación |
|-----------|--------|---------------|
| I. MVVM + Clean por capas | ✅ | `AppInfo` se compone en `SettingsViewModel` (sin `Context` de Activity); la UI Compose solo pinta estado y delega acciones. `AppBuildInfo` se provee vía Hilt. |
| II. Kotlin-First, Compose M3 y Media3 | ✅ | Se mantiene Compose + Material 3; no se toca el player ni la red autenticada. |
| III. Calidad Test-First (NON-NEGOTIABLE) | ✅ | Red-Green: tests de `SettingsViewModel` (versión derivada de `AppBuildInfo`, configuración activa, mensajes de copia) y del formateador puro. Se actualiza el test existente que asumía `"1.0"` fijo. |
| IV. Streaming Robusto y Manejo de Errores | ✅ | No afecta al player. El copiado fallido se comunica con snackbar y nunca lanza excepción visible. |
| V. Simplicidad Modular (YAGNI) | ✅ | Sin módulos ni dependencias nuevas; `AppBuildInfo` es un value object mínimo con proveedor Hilt (justificado por la regla Red-Green: permite inyectar versiones distintas en test). Se reutilizan `InstancePrefs` y `GetServersUseCase`. |

**Resultado**: Sin violaciones. Todas las puertas pasan. Re-evaluado tras el diseño (Phase 1): se mantiene sin violaciones.

## Project Structure

### Documentation (this feature)

```text
specs/0033-jlizquierdo-20260913-about-dialog-real-version/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── app-info-dialog-ui.md
│   └── settings-app-info-contract.md
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/
│   └── util/
│       └── AppBuildInfo.kt                  # NEW: value object con metadatos del build
├── di/
│   └── AppInfoModule.kt                     # NEW: @Provides AppBuildInfo desde BuildConfig
├── feature/
│   └── settings/
│       ├── SettingsViewModel.kt             # MODIFIED: compone AppInfo y mensajes de copia
│       └── SettingsScreen.kt                # MODIFIED: host de snackbar + apertura del diálogo
└── core/ui/components/
    └── AppInfoDialog.kt                     # MODIFIED: datos ampliados + copiar + snackbar

app/src/test/java/com/izquierdojl/tolocharadio/
└── feature/settings/
    └── SettingsViewModelTest.kt             # MODIFIED: tests de versión real, config activa y copia
```

**Structure Decision**: Se mantiene la feature en `feature/settings/` (extensión de Configuración, no sección nueva) y el componente reutilizable en `core/ui/components/AppInfoDialog.kt`, coherente con la spec 015. El nuevo `AppBuildInfo` va en `core/util/` (utilidad transversal, sin dependencias Android) con su proveedor Hilt en `di/`.

## Complexity Tracking

> No se requiere: sin violaciones de la constitución. `AppBuildInfo` + módulo Hilt se justifican por la regla Red-Green (inyectar versiones distintas en test) y no introducen dependencias ni módulos Gradle nuevos.
