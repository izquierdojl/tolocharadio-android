# Implementation Plan: Reordenación intuitiva de favoritos

**Branch**: `0034-jlizquierdo-20260913-favorites-reorder-animation` | **Date**: 2026-09-13 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0034-jlizquierdo-20260913-favorites-reorder-animation/spec.md`

## Summary

Mejorar la experiencia de reordenar favoritas, hoy poco intuitiva y sin animación. El
reorden ya existe (`FavoritesScreen` con asa de arrastre y `moveItem` en vivo, autoguardado
con `commitOrder`), pero las filas cambian de golpe, la fila activa no se distingue y el
gesto exige una pulsación larga poco evidente.

Enfoque técnico: (a) animar la reubicación de las filas con `Modifier.animateItem()` del
`LazyColumn`; (b) iniciar el arrastre de forma inmediata sobre el asa
(`detectDragGestures` en lugar de `detectDragGesturesAfterLongPress`); (c) resaltar la fila
activa con `graphicsLayer` (escala) + `shadow` + `zIndex`; (d) añadir respuesta háptica y
auto-scroll con velocidad proporcional al borde; (e) ofrecer una alternativa accesible con
un menú de desbordamiento por fila ("Mover arriba"/"Mover abajo"); (f) deshabilitar el
reorden con caché offline. Todo local, sin backend ni dependencias nuevas.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17, `minSdk` 26 / `targetSdk` 37, AGP 9.3.2 (`gradle/libs.versions.toml`, `app/build.gradle.kts`)

**Primary Dependencies**: Jetpack Compose + Material 3 (Compose BOM `2025.01.00` → foundation 1.7.x con `Modifier.animateItem()`), Hilt (DI), `ViewModel` + `StateFlow`/`SharedFlow`, Coroutines + Flow, Coil (favicon)

**Storage**: N/A — no se persiste nada nuevo. La caché Room de favoritas sigue siendo de solo lectura; el orden se guarda en el servidor vía `PUT /favorites/order` (sin cambios de contrato)

**Testing**: JUnit + MockK + Turbine (unit, `FavoritesViewModel`) y Compose UI Test (`androidTest`, flujo crítico: reorden, menú accesible, offline). Uso de `MainDispatcherRule` existente

**Target Platform**: Android (móvil)

**Project Type**: Mobile app (Android), módulo único `app`

**Performance Goals**: reacomodación visual percibida como inmediata (<100 ms por cruce, SC-002); animaciones a 60 fps; sin trabajo de red durante el arrastre (el guardado ocurre al soltar)

**Constraints**: sin dependencias nuevas; sin cambios de backend; reorden solo con conexión (caché offline en solo lectura); sin PII en logs; no alterar la reproducción en curso

**Scale/Scope**: 1 pantalla (`FavoritesScreen`) y su `ViewModel`; lista completa de favoritas del usuario (paginación ya resuelta por el servidor)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Justificación |
|-----------|--------|---------------|
| I. MVVM + Clean por capas | ✅ | El orden y el guardado viven en `FavoritesViewModel` (`moveUp`/`moveDown`/`moveItem`/`commitOrder`); la UI Compose solo pinta estado y delega. El estado transitorio del arrastre (`draggingId`) es puramente presentacional y se queda en el Composable. Sin `Context` de Activity. |
| II. Kotlin-First, Compose M3 y Media3 | ✅ | Todo con Compose + Material 3; no se toca XML/Views ni el player (`Media3`). El menú accesible usa `DropdownMenu` de M3. |
| III. Calidad Test-First (NON-NEGOTIABLE) | ✅ | Red-Green: tests de `FavoritesViewModel` para `moveUp`/`moveDown` (bordes, guardado, offline) y Compose UI test del menú, asa y offline. Se conservan y amplían los tests actuales de Favoritos. |
| IV. Streaming Robusto y Manejo de Errores | ✅ | El fallo de guardado restaura el último orden confirmado y avisa (FR-010); el conflicto entre dispositivos ya muestra el orden del servidor (FR-011). Reordenar no detiene la reproducción (FR-016). |
| V. Simplicidad Modular (YAGNI) | ✅ | Sin dependencias nuevas (se descarta Accompanist reorderable), sin módulos Gradle, sin cola offline. Se reutilizan `ReorderFavoritesUseCase`, `FavoritesRepo` y `animateItem()` del stack actual. |

**Resultado**: sin violaciones. Todas las puertas pasan. Re-evaluado tras el diseño
(Phase 1): se mantiene sin violaciones.

## Project Structure

### Documentation (this feature)

```text
specs/0034-jlizquierdo-20260913-favorites-reorder-animation/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── favorites-reorder-ui.md
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/
├── FavoritesScreen.kt        # MODIFIED: animateItem, arrastre inmediato, lift, hápticos,
│                             #   menú accesible, auto-scroll proporcional, gating offline
└── FavoritesViewModel.kt     # MODIFIED: moveUp/moveDown (semánticos) + guardas offline

app/src/test/java/com/izquierdojl/tolocharadio/feature/favorites/
└── FavoritesViewModelTest.kt # MODIFIED: tests Red-Green de moveUp/moveDown y offline

app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/favorites/
└── FavoritesScreenTest.kt    # MODIFIED: menú accesible, asa gated offline, reorden
```

**Structure Decision**: La mejora se mantiene dentro de `feature/favorites/` (extensión de
la pantalla y su `ViewModel`), reutilizando `ReorderFavoritesUseCase` y `FavoritesRepo` sin
tocarlos. No se crean módulos, pantallas ni capas nuevas; el único estado nuevo (arrastre)
es presentacional y local al Composable.

## Complexity Tracking

> No se requiere: sin violaciones de la constitución. No hay dependencias, módulos ni
> abstracciones nuevas; se descartaron explícitamente librerías de reorden (Accompanist)
> por YAGNI.
