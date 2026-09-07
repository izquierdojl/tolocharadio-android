# Implementation Plan: Bottom Nav Icons Only + Section Titles

**Branch**: `012-bottom-nav-icons-only` | **Date**: 2026-09-07 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/012-bottom-nav-icons-only/spec.md`

## Summary

Eliminar las etiquetas de texto de los 5 `NavigationBarItem` de la barra inferior para resolver el problema de texto en dos líneas ("Mis emisoras", "Configuración"). Añadir títulos de sección consistentes (`headlineSmall`, padding `16dp horizontal, 8dp vertical`) a las pantallas que carecen de ellos (Explorar, Favoritos) y normalizar los existentes (Configuración usa `headlineMedium` + 24dp). Añadir tooltip de accesibilidad al pulsación larga en los iconos.

## Technical Context

**Language/Version**: Kotlin, minSdk 26, targetSdk última estable

**Primary Dependencies**: Jetpack Compose + Material Design 3 (NavigationBar, TooltipBox, Typography), Hilt, Coroutines + Flow

**Storage**: N/A (cambio puramente de UI)

**Testing**: JUnit + Compose Test (`composeTestRule`)

**Target Platform**: Android (app móvil)

**Project Type**: mobile-app

**Performance Goals**: N/A (cambio visual sin impacto en rendimiento)

**Constraints**: Debe mantener compatibilidad con mini-player persistente sobre la bottom bar (spec 004). No romper la navegación existente ni el estado seleccionado.

**Scale/Scope**: 5 pantallas afectadas (Explorar, Favoritos, Historial, Mis emisoras, Configuración), 1 archivo de navegación principal.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. MVVM + Clean por Capas | ✅ PASS | Cambio solo en capa UI (Composables). No toca domain ni data. |
| II. Kotlin-First, Compose M3 | ✅ PASS | Usa Compose + M3 (NavigationBar, TooltipBox, Typography). Sin XML ni Java. |
| III. Test-First | ✅ PASS | Se añadirá Compose Test para verificar que los títulos aparecen y los tooltips se muestran. |
| IV. Streaming Robusto | ✅ PASS | No afecta al player ni a la red. |
| V. Simplicidad (YAGNI) | ✅ PASS | Sin nuevas dependencias ni abstracciones. TooltipBox es parte de M3. |

**GATE RESULT: PASS** — No violations.

## Project Structure

### Documentation (this feature)

```text
specs/012-bottom-nav-icons-only/
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
├── core/ui/navigation/
│   └── TolochaNavGraph.kt          # NavigationBar + BottomDest (MODIFY)
├── feature/explore/
│   └── ExploreScreen.kt            # Añadir título "Explorar" (MODIFY)
├── feature/favorites/
│   └── FavoritesScreen.kt          # Añadir título "Tus favoritos" (MODIFY)
├── feature/history/
│   └── HistoryScreen.kt            # Normalizar padding título (MODIFY)
├── feature/customstations/
│   └── CustomStationsScreen.kt     # Normalizar padding título (MODIFY)
└── feature/settings/
    └── SettingsScreen.kt           # Cambiar headlineMedium→headlineSmall, padding 24dp→8dp vertical (MODIFY)
```

**Structure Decision**: Cambios limitados a la capa UI existente. No se crean nuevas clases, módulos ni dependencias. Se modifica `TolochaNavGraph.kt` para eliminar labels y añadir tooltips, y se ajustan 5 pantallas para títulos consistentes.

## Complexity Tracking

> No violations — table intentionally empty.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | — | — |
