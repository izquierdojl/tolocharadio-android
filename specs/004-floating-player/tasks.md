# Tasks: Reproductor flotante inferior

**Input**: Design documents from `/specs/004-floating-player/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/mini-panel-ui.md, quickstart.md

**Tests**: incluidos por mandato constitucional (III. Calidad Test-First, NON-NEGOTIABLE): tests nuevos en `PlayerViewModelTest.kt` y `PanelHelpersTest.kt` se escriben PRIMERO y deben FALLAR antes de implementar. Compose Test sujeto a la limitación conocida del entorno (API 37 incompatible con `compose-ui-test`, como en specs 002/003 → verificación manual documentada como fallback).

**Organization**: tareas agrupadas por historia de usuario para implementación y test independientes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: paralelizable (ficheros distintos, sin dependencias)
- **[Story]**: historia a la que pertenece (`US1`, `US2`, `US3`)
- Todas las descripciones incluyen rutas exactas

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: línea base verificada antes de tocar código del player

- [X] T001 Comprobar que no hace falta ninguna dependencia nueva (revisar `gradle/libs.versions.toml`: Compose M3, Media3 1.4.1, Coil, Hilt y `LocalClipboardManager` de foundation ya disponibles)
- [X] T002 Ejecutar la batería unitaria base y dejarla en verde con `./gradlew :app:testDebugUnitTest` antes de empezar

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: un único estado de reproducción compartido en toda la app (R3) — sin esto el panel global (FR-009) es imposible

**⚠️ CRITICAL**: ningún trabajo de historias puede empezar hasta completar esta fase

- [X] T003 Crear tests base del VM en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` (Idle inicial, `play()`→`Buffering` con status ok, `Error` con status no-playable; MockK para `ExoPlayer`/`PlaybackRepo`/`InstancePrefs`/`AuthDataSourceFactory`) — deben FALLAR
- [X] T004 Compartir un único `PlayerViewModel` a ámbito de Activity y pasarlo a `MiniPlayer`, `FavoritesScreen` y `StationDetailScreen` en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/StationDetail.kt` (sustituye los `hiltViewModel()` por defecto)
- [X] T005 Verificar compilación y tests base en verde con `./gradlew :app:assembleDebug :app:testDebugUnitTest`

**Checkpoint**: un solo VM vivo al navegar — reproducir en una pantalla y leer el mismo estado desde otra

---

## Phase 3: User Story 1 - Ver qué suena sin perder la pantalla (Priority: P1) ⭐ MVP

**Goal**: panel fijo sobre la barra inferior con avatar, nombre y línea técnica, visible en todo estado no-`Idle`

**Independent Test**: reproducir una emisora, navegar por 3 secciones y comprobar que el panel sigue visible encima de los botones con icono, nombre y datos técnicos (spec US1)

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T006 [P] [US1] Crear `PanelHelpersTest.kt` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PanelHelpersTest.kt` (`panelSubtitle` completa/parcial/vacía→`"Emisora de radio"`; `resolveCopyLink` con URL y en blanco→`null`)

### Implementation for User Story 1

- [X] T007 [P] [US1] Crear `PanelHelpers.kt` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PanelHelpers.kt` (`panelSubtitle(station)`, `resolveCopyLink(station)` puras según `data-model.md`; hace pasar T006)
- [X] T008 [US1] Reordenar el slot `bottomBar` en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` a `Column(MiniPanel sobre NavigationBar)`
- [X] T009 [US1] Reescribir `MiniPlayer` como `MiniPanel` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` (izquierda: `StationArtwork` 48 dp reutilizado + título 1 línea ellipsis + subtítulo; oculto en `Idle`; derecha conserva play/pausa por ahora)
- [X] T010 [US1] Conservar `Detener` solo en `FullPlayerSheet` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` (verificar acción existente stop→`Idle`+oculta panel)

**Checkpoint**: US1 funciona y se puede probar sola — el panel identifica la emisora y sobrevive a la navegación

---

## Phase 4: User Story 2 - Controlar el audio desde el panel (Priority: P1)

**Goal**: derecha del panel con play/pausa, silencio (emisión en curso) y copiar enlace con confirmación; layout de error reintentar+copiar

**Independent Test**: con una emisora sonando, pausar, silenciar, copiar el enlace y pegarlo fuera comprobando que es la URL original (spec US2)

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T011 [US2] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` (mute: `toggleMute` solo cambia volumen, coexiste con pausa, reset en `play()`/`stop()`; `cancelLoad()`: `Buffering`→`Idle` con job cancelado)

### Implementation for User Story 2

- [X] T012 [US2] Implementar `isMuted`+`toggleMute` (volumen `0f`/`1f`) y `loadJob`+`cancelLoad()` con reset de mute en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` (hace pasar T011)
- [X] T013 [US2] Añadir botones de silencio y copiar con `LocalClipboardManager`+`SnackbarHostState`/`SnackbarHost` (`"Enlace copiado"` / `"enlace no disponible"`, solo `station.url`) y cablear el botón principal en `Buffering` a `cancelLoad()` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`
  *(El botón de copiar del panel queda SUPERSEDED por la spec 0039 FR-003/FR-004 — converge 2026-09-20: compartir/copiar se ofrece en la ficha de la emisora; el silenciar y el `cancelLoad()` se mantienen.)*
- [X] T014 [US2] Implementar layout de error (principal=reintentar, copiar visible, mute oculto + mensaje breve en ES) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` según `contracts/mini-panel-ui.md`
  *(Parte "copiar visible" SUPERSEDED por la spec 0039 FR-003 — converge 2026-09-20: en error solo reintentar.)*

**Checkpoint**: US1+US2 funcionan — panel completo con los 3 controles y sus estados

---

## Phase 5: User Story 3 - Escuchar igual en toda la app (Priority: P2)

**Goal**: mismo panel y estado en todos los destinos con barra inferior; el toque izquierdo abre el completo

**Independent Test**: reproducir desde Búsqueda y desde Favoritos comprobando panel idéntico y estado conservado al navegar (spec US3)

- [X] T015 [US3] Abrir `FullPlayerSheet` con la misma emisora al tocar la zona izquierda (los botones no abren el sheet) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt`
  *(SUPERSEDED por la spec 010 FR-004 — converge 2026-09-20: el tap abre `StationInfoSheet`; el full-player se abre desde el estado de reproducción/notificación.)*
- [X] T016 [US3] Verificar paridad en Mis emisoras/Perfil/inicio+ficha (mismo panel/estado) con `PlayerPanelTest.kt` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/PlayerPanelTest.kt`, o recorrido manual documentado si el entorno API 37 lo impide (precedente specs 002/003)

**Checkpoint**: las 3 historias funcionan de forma independiente y coherente

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: validación end-to-end y gates de merge

- [X] T017 [P] Ejecutar el recorrido de `specs/004-floating-player/quickstart.md` §2 (8 pasos: panel global, mute, copiar, cancelar carga, error, degradación, reset de mute, full-player+rotación)
- [X] T018 Pasar gates estáticos con `./gradlew lintDebug ktlintCheck detekt` (cero errores)
- [X] T019 [P] Revisión manual de TalkBack (descripciones ES de los 3 botones + anuncio `"Sonando: {nombre}"`) y de textos en español del panel

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias — empezar inmediatamente
- **Foundational (Phase 2)**: depende de Setup — BLOQUEA todas las historias (el VM compartido es requisito de FR-009)
- **User Stories (Phases 3–5)**: dependen de Foundational; entre ellas en orden P1 → P1 → P2 (US2 reutiliza el panel de US1; US3 lo verifica globalmente)
- **Polish (Phase 6)**: depende de las historias que se quieran entregar

### User Story Dependencies

- **US1 (P1)**: tras Foundational, sin dependencias de otras historias — es el MVP
- **US2 (P1)**: tras Foundational; amplía el panel de US1 pero sus tests de VM son independientes
- **US3 (P2)**: tras US1+US2 (verifica el conjunto globalmente + añade `onOpenFull`)

### Within Each User Story

- Tests PRIMERO y en rojo antes de implementar (mandato constitucional)
- Helpers/VM antes que UI; UI antes que verificación manual
- Completar la historia antes de pasar a la siguiente prioridad

### Parallel Opportunities

- T001+T002 (ficheros/comandos distintos) y T006+T007 (test y helpers en ficheros distintos, T007 hace pasar T006) y T017+T019 (verificaciones manuales independientes) pueden ir en paralelo
- T004 toca 4 ficheros del mismo grafo de compilación → secuencial
- Con varios desarrolladores: tras Foundational, US1 y los tests de US2 (T011) pueden avanzar en paralelo (ficheros distintos: `PanelHelpers*` vs `PlayerViewModelTest`)

---

## Parallel Example: User Story 1

```bash
# Tests y helpers de US1 juntos (ficheros distintos):
Task: "Crear PanelHelpersTest.kt en app/src/test/.../feature/player/PanelHelpersTest.kt"
Task: "Crear PanelHelpers.kt en app/src/main/.../feature/player/PanelHelpers.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup (línea base verde)
2. Completar Phase 2: Foundational (VM compartido — bloqueante)
3. Completar Phase 3: US1 (panel con identidad sobre la barra)
4. **STOP and VALIDATE**: test independiente de US1 (navegar 3 secciones con panel visible)
5. Demo si procede

### Incremental Delivery

1. Setup + Foundational → base lista
2. + US1 → panel visible (MVP) → validar
3. + US2 → panel completo P1 (mute + copiar + error + cancelar carga) → validar
4. + US3 → paridad global + apertura del completo → validar
5. Polish → merge (lint/ktlint/detekt en verde)

### Parallel Team Strategy

1. Todo el equipo: Setup + Foundational juntos
2. Después: Dev A → US1 (panel+helpers), Dev B → tests US2 (T011), y al cerrar US1 ambos en US2-implementación y US3

---

## Notes

- `[P]` = ficheros distintos, sin dependencias pendientes
- `[Story]` traza cada tarea a su historia (fases Setup/Foundational/Polish sin etiqueta)
- Cada historia es completable y testeable por separado; parar en cualquier checkpoint para validar
- Commits tras cada tarea o grupo lógico; copiar jamás incluye la URL del proxy
- Evitar: tareas vagas, conflictos en el mismo fichero en paralelo, dependencias cruzadas que rompan la independencia


---

## Phase 7: Convergence

**Origen**: `/speckit.converge` 2026-09-20 — 11 FR, 3 historias y 5 principios revisados contra el código. Panel, layout de error, mute, detener en full-player y VM compartido conformes; dos FRs quedaron obsoletos por specs posteriores sin anotar.

- [X] T020 Anotar el supersede de la 0039 en `specs/004-floating-player/spec.md`: marcar FR-003 ("exactamente tres acciones… y copiar enlace"), FR-006 (botón de copiar en el panel) y la parte de copiar de T013 en `tasks.md` como SUPERSEDED por la spec 0039 FR-003 (panel con solo play/pausa + silenciar en reproducción normal; compartir/copiar se ofrece en la ficha de emisora) per FR-003/FR-006 (contradicts) — MEDIUM
- [X] T021 Anotar el supersede de la 010 en `specs/004-floating-player/spec.md`: marcar FR-007 y T015 ("tocar la zona izquierda abre el reproductor completo") como SUPERSEDED por la spec 010 FR-004 (el tap abre `StationInfoSheet`; el full-player se abre desde el estado de reproducción/notificación) per FR-007 (contradicts) — MEDIUM
