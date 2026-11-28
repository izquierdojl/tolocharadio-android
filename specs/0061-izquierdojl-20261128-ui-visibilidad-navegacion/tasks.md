# Tasks: Visibilidad UI y navegación

**Input**: Design documents from `/specs/0061-izquierdojl-20261128-ui-visibilidad-navegacion/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Incluidos porque el plan y la spec los exigen para los flujos críticos (contraste, tarjetas, reproductores y navegación).

**Organization**: Tareas agrupadas por historia de usuario para que cada una sea implementable y testeable de forma independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo (ficheros distintos, sin dependencias pendientes)
- **[Story]**: US1–US4, según `spec.md`
- Todas las rutas son relativas a la raíz del repo

## Path Conventions

- Módulo único Android: `app/src/main/java/com/izquierdojl/tolocharadio/`
- Tests unitarios: `app/src/test/java/com/izquierdojl/tolocharadio/`
- Tests instrumentados: `app/src/androidTest/java/com/izquierdojl/tolocharadio/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Tokens de tema y proveedor común que necesitan todas las historias.

- [X] T001 Crear `TolochaColors` con los campos `data-model.md` `TolochaColors.isDark`, `TolochaColors.cardContainer`, `TolochaColors.cardBorder`, `TolochaColors.cardPlaceholder`, `TolochaColors.playerContainer`, `TolochaColors.playerBorder`, `TolochaColors.chromeDivider`, `TolochaColors.navIndicator`, `TolochaColors.navSelectedContent` y `TolochaColors.navUnselectedContent`, más `LocalTolochaColors` y las instancias oscura/clara, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/theme/TolochaColors.kt`
- [X] T002 Proveer `LocalTolochaColors` desde `TolochaTheme`, añadir `Pine750 = Color(0xFF1A2E20)` y fijar `onSurfaceVariant = Pine300` en el esquema oscuro, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/theme/Theme.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Modelo de estado, contenedor y badge que usan todas las historias.

**⚠️ CRITICAL**: Ninguna historia puede empezar hasta completar esta fase.

- [X] T003 [P] Extender `ContrastTest` con los pares de `contracts/design-tokens.md`: `data-model.md` `TolochaColors.cardBorder` / `TolochaColors.cardContainer` (≥1.5 decorativo), `TolochaColors.cardContainer` / fondo (≥1.3 solo en oscuro; en claro validar borde + elevación), `TolochaColors.navIndicator` / barra (≥3 esencial) y `TolochaColors.navSelectedContent` / `TolochaColors.navIndicator` (≥4.5), en `app/src/test/java/com/izquierdojl/tolocharadio/core/ui/theme/ContrastTest.kt`
- [X] T004 [P] Crear `StationPlaybackUiState` con `data-model.md` `StationPlaybackUiState.Inactive`, `StationPlaybackUiState.Playing`, `StationPlaybackUiState.Paused`, `StationPlaybackUiState.Loading` y `StationPlaybackUiState.Error`; crear `NowPlayingUi` con `data-model.md` `NowPlayingUi.stationId` y `NowPlayingUi.state`, y `stateFor(stationId)`, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/playback/StationPlaybackUiState.kt`
- [X] T005 Crear el mapeo puro desde `data-model.md` `PlayerState.Idle`, `PlayerState.Playing`, `PlayerState.Paused`, `PlayerState.Buffering`, `PlayerState.Reconnecting`, `PlayerState.Error`, `CastPlayerState.Local` y `CastPlayerState.Cast` a `data-model.md` `NowPlayingUi.stationId` y `NowPlayingUi.state`, incluyendo Cast y station mismatch, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerStateMapper.kt`
- [X] T006 [P] Crear `NowPlayingContainer` con `data-model.md` `TolochaColors.playerContainer` y `TolochaColors.playerBorder`, forma superior de 20 dp y divisor superior de 1 dp, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/NowPlayingContainer.kt`
- [X] T007 Crear `StationPlaybackBadge` para `StationPlaybackUiState.Playing`, `Paused`, `Loading` y `Error`, con texto/icono y `contentDescription` (depende de T004), en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/StationPlaybackBadge.kt`
- [X] T008 Crear `PlayerStateMapperTest` para todos los `PlayerState`, Cast y estación distinta, verificando `data-model.md` `NowPlayingUi.stationId` y `NowPlayingUi.state`, en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerStateMapperTest.kt`

**Checkpoint**: Tokens, estado de tarjeta, contenedor y badge listos; las historias pueden empezar.

---

## Phase 3: User Story 1 - Distinguir tarjetas y emisora en reproducción (Priority: P1) 🎯 MVP

**Goal**: Tarjetas separadas del fondo, chips legibles, indicador de emisora activa y botón play/pausa correcto en las cuatro superficies.

**Independent Test**: Abrir Explorar en oscuro con varias tarjetas y una emisora sonando; comprobar separación, contraste de chips, badge, play/pausa y apertura de ficha. Repetir en Favoritos, Historial y Mis emisoras.

### Tests for User Story 1

- [X] T009 [P] [US1] Actualizar `CommonUiTest` para la nueva firma de `StationCard`/`StationListItem`, `onOpen` vs `onPlay`, badge, fallback de favicon y área táctil de 48 dp, usando `data-model.md` `NowPlayingUi.state`, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/CommonUiTest.kt`
- [X] T010 [P] [US1] Actualizar `FavoritesScreenTest` para el badge activo en grid/lista y para play/pausa sin abrir la ficha, usando `data-model.md` `NowPlayingUi.state`, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreenTest.kt`

### Implementation for User Story 1

- [X] T011 [P] [US1] Actualizar `StationCard` y `StationListItem` con `data-model.md` `TolochaColors.cardContainer`, `TolochaColors.cardBorder`, `TolochaColors.cardPlaceholder` y `TolochaColors.isDark`, badge, separación `onOpen`/`onPlay`, botón de play de 48 dp táctil (40-44 visual), `StationDto.id`, `StationDto.name`, `StationDto.favicon`, `StationDto.country`, `StationDto.language`, `StationDto.tags` y `StationDto.bitrate`, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/CommonUi.kt`
- [X] T012 [P] [US1] Pasar `PlayerViewModel` a `ExploreScreen`, derivar `data-model.md` `NowPlayingUi.stationId` y `NowPlayingUi.state`, separar `onOpen`/`onPlay` y aplicar el mapeo estado→acción (`Inactive`→play, `Playing`/`Paused`→toggle, `Loading`→no accionable, `Error`→play/retry) en tarjeta y lista; añadir un valor por defecto para no romper `ViewModeToggleTest`, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreScreen.kt`
- [X] T013 [P] [US1] Actualizar `FavoritesScreen` y `FavoriteRow` con `data-model.md` `NowPlayingUi.stationId` y `NowPlayingUi.state`, badge, play/pausa y el mismo mapeo estado→acción de T012 en grid/lista, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt`
- [X] T014 [P] [US1] Actualizar `HistoryScreen`, `HistoryRow` y `HistoryGridCard` con `data-model.md` `NowPlayingUi.stationId`, `NowPlayingUi.state`, `TolochaColors.cardContainer`, `TolochaColors.cardBorder`, `TolochaColors.cardPlaceholder` y `TolochaColors.isDark`; badge, elevación clara de 2 dp, play/pausa con área táctil de 48 dp y el mismo mapeo estado→acción de T012 en grid/lista, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryScreen.kt`
- [X] T015 [P] [US1] Actualizar `CustomStationsScreen`, `CustomStationRow` y `CustomStationGridCard` con `data-model.md` `NowPlayingUi.stationId`, `NowPlayingUi.state`, `TolochaColors.cardContainer`, `TolochaColors.cardBorder`, `TolochaColors.cardPlaceholder` y `TolochaColors.isDark`; badge, elevación clara de 2 dp, play/pausa con área táctil de 48 dp y el mismo mapeo estado→acción de T012 en grid/lista, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/customstations/CustomStationsScreen.kt`
- [X] T016 [US1] Pasar `playerVm` a `ExploreScreen` y aplicar play/pausa compartido en los callbacks de reproducción, usando `data-model.md` `NowPlayingUi.stationId`, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`

**Checkpoint**: US1 funcional y testeable de forma independiente; las tarjetas ya muestran el estado activo en las cuatro superficies.

---

## Phase 4: User Story 2 - Panel compacto de radio y podcast (Priority: P1)

**Goal**: Panel de radio de 64 dp sin línea técnica y panel de podcast de 112 dp con dos líneas, play centrado, saltos, progreso, tiempos y Detener secundario.

**Independent Test**: Reproducir una emisora y medir/comprobar el panel de radio; reproducir un episodio y comprobar el panel de podcast, tiempos, stop, error/carga y Cast.

### Tests for User Story 2

- [X] T017 [P] [US2] Actualizar `MiniPlayerTest` para `En directo`/`Pausado`, ausencia de línea técnica, 2 acciones, altura de 64 dp y estados de error/carga, usando `data-model.md` `PlayerPanelUi.status`, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/MiniPlayerTest.kt`
- [X] T018 [P] [US2] Actualizar `EpisodeMiniPlayerTest` para dos líneas, tiempos, stop, saltos, sin duración, altura de 112 dp y tamaño del play, usando `data-model.md` `EpisodePlayerUi.positionMs`, `EpisodePlayerUi.durationMs` y `EpisodePlayerUi.playing`, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayerTest.kt`

### Implementation for User Story 2

- [X] T019 [US2] Refactorizar `MiniPlayer`/`MiniPlayerContent` con `data-model.md` `TolochaColors.playerContainer`, `TolochaColors.playerBorder`, `PlayerPanelUi.status`, `PlayerPanelUi.castDeviceName`, `PlayerViewModel.state`, `PlayerViewModel.castState` y `PlayerViewModel.isMuted`; definir el tipo privado `PlayerPanelUi(status, castDeviceName)` en `PlayerUi.kt`, con estados `En directo`/`Pausado`/mensajes, altura de 64 dp, carátula 40 dp, play 56 dp, silenciar 48 dp y sin `panelSubtitle`, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt`
- [X] T020 [P] [US2] Refactorizar `EpisodeMiniPlayerContent` con `data-model.md` `TolochaColors.playerContainer`, `TolochaColors.playerBorder`, `PodcastSummary.title`, `PodcastEpisode.id`, `PodcastEpisode.title`, `PodcastEpisode.durationSec`, `PodcastEpisode.imageUrl`, `EpisodePlayerUi.positionMs`, `EpisodePlayerUi.durationMs`, `EpisodePlayerUi.playing` y `PlayerViewModel.state`: dos filas, título a una línea, máximo dos líneas de texto, altura de 112 dp, play 56 dp, saltos/stop 48 dp con `FastRewind`/`FastForward` y tiempos/progreso compactos, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayer.kt`
- [X] T021 [US2] Eliminar `panelSubtitle` y sus tests si no quedan consumidores, conservando `resolveCopyLink` y los tests de `resolveCopyLink` (depende de T019), en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PanelHelpers.kt` y `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PanelHelpersTest.kt`
- [X] T022 [US2] Verificar que `StationInfoSheet` muestra `data-model.md` `StationDto.codec`, `StationDto.bitrate`, `StationDto.country` y `StationDto.language`, y conserva `StationDto.url` para compartir, tras retirarlos del panel, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationInfoSheet.kt`

**Checkpoint**: US2 funcional; radio y podcast compactos, con estados y Cast intactos.

---

## Phase 5: User Story 3 - Saber dónde estoy y a dónde voy (Priority: P2)

**Goal**: Barra inferior con etiquetas, selección visible, divisores y transición suave.

**Independent Test**: Recorrer las 4 secciones de radio y las 5 de podcast; comprobar etiquetas, selección, fuente al 200 %, divisores y fade.

### Tests for User Story 3

- [X] T023 [P] [US3] Actualizar `TolochaNavigationBarTest` para etiquetas visibles en 360 dp por defecto, 4/5 destinos, selección, un único anuncio accesible por destino y 320 dp con `fontScale = 2.0`, usando `data-model.md` `TolochaColors.navIndicator`, `TolochaColors.navSelectedContent` y `TolochaColors.navUnselectedContent`, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavigationBarTest.kt`
- [X] T024 [P] [US3] Actualizar `TolochaTopBarTest` para el divisor inferior y las acciones intactas, usando `data-model.md` `TolochaColors.chromeDivider`, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaTopBarTest.kt`

### Implementation for User Story 3

- [X] T025 [US3] Añadir `label`, `alwaysShowLabel`, colores de selección, `testTag` por destino y `contentDescription = null` en el icono para que la etiqueta sea el único nombre accesible, con `data-model.md` `TolochaColors.navIndicator`, `TolochaColors.navSelectedContent` y `TolochaColors.navUnselectedContent` (depende de T016: mismo fichero), en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`
- [X] T026 [P] [US3] Añadir el divisor inferior de la cabecera con `data-model.md` `TolochaColors.chromeDivider`, sin cambiar la altura, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaTopBar.kt`
- [X] T027 [US3] Añadir el divisor sobre la barra inferior y las transiciones `fadeIn(tween(200))`/`fadeOut(tween(180))` del `NavHost`, respetando `MotionDurationScale` (con animaciones desactivadas, transición inmediata), usando `data-model.md` `TolochaColors.chromeDivider`, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`

**Checkpoint**: US3 funcional; navegación legible y fluida sin cambiar destinos ni orden.

---

## Phase 6: User Story 4 - Modo claro con la misma jerarquía (Priority: P3)

**Goal**: Paridad visual en claro y ausencia de fondos oscuros heredados.

**Independent Test**: Cambiar a modo claro y repetir US1/US2; comprobar borde, elevación, placeholder claro, panel y barra.

### Tests for User Story 4

- [X] T028 [P] [US4] Añadir aserciones de modo claro a `CommonUiTest` para `data-model.md` `TolochaColors.cardContainer`, `TolochaColors.cardBorder`, `TolochaColors.cardPlaceholder` y `TolochaColors.isDark`, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/CommonUiTest.kt`
- [X] T029 [P] [US4] Añadir aserciones de modo claro a `MiniPlayerTest` para `data-model.md` `TolochaColors.playerContainer` y `TolochaColors.playerBorder`, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/MiniPlayerTest.kt`
- [X] T030 [P] [US4] Añadir aserciones de modo claro a `EpisodeMiniPlayerTest` para `data-model.md` `TolochaColors.playerContainer` y `TolochaColors.playerBorder`, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayerTest.kt`

### Implementation for User Story 4

- [X] T031 [US4] Verificar la paleta clara y la elevación de 2 dp de tarjeta, cubriendo `data-model.md` `TolochaColors.isDark`, `TolochaColors.cardContainer`, `TolochaColors.cardBorder`, `TolochaColors.cardPlaceholder`, `TolochaColors.playerContainer`, `TolochaColors.playerBorder`, `TolochaColors.chromeDivider`, `TolochaColors.navIndicator`, `TolochaColors.navSelectedContent` y `TolochaColors.navUnselectedContent`, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/theme/TolochaColors.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/CommonUi.kt`

**Checkpoint**: US4 funcional; claro y oscuro comparten jerarquía y no hay bloques oscuros fijos.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Limpieza, previews y validación final.

- [X] T032 [P] Eliminar imports y código muerto (`panelSubtitle`, colores fijos sustituidos) en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/CommonUi.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PanelHelpers.kt`
- [X] T033 Añadir previews claro/oscuro de `StationCard`, `MiniPlayerContent` y `EpisodeMiniPlayerContent` (después de T032) en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/CommonUi.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayer.kt`
- [X] T034 Ejecutar `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` según `app/build.gradle.kts` y corregir cualquier fallo — gates verdes en local (2026-11-28)
- [X] T035 Ejecutar `./gradlew connectedDebugAndroidTest` según `app/build.gradle.kts` con dispositivo/emulador y corregir cualquier fallo — 152/152 tests en emulador (2026-11-28)
- [X] T036 Recorrer los escenarios manuales de `specs/0061-izquierdojl-20261128-ui-visibilidad-navegacion/quickstart.md` en oscuro, claro, 200 % de fuente, Cast, reducción de animaciones (transición inmediata) y anuncio único de TalkBack — ASUMIDO: Cast/reducción de animaciones no probados, aceptado por el usuario al cierre (2026-11-28)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Fase 1)**: sin dependencias.
- **Foundational (Fase 2)**: depende de Fase 1 y bloquea todas las historias.
- **US1, US2, US3, US4**: dependen de Fase 2; pueden ir en paralelo si hay capacidad.
- **Polish (Fase 7)**: depende de las historias que se quieran entregar.

### User Story Dependencies

- **US1 (P1)**: independiente tras Fase 2. Es el MVP.
- **US2 (P1)**: independiente tras Fase 2; comparte `NowPlayingContainer` y tokens, no código de US1.
- **US3 (P2)**: independiente tras Fase 2; toca `TolochaNavGraph` y `TolochaTopBar`. Ojo: US1 (T016) y US3 (T025/T027) comparten `TolochaNavGraph.kt`; si se paralelizan, T025 debe esperar a T016 o coordinar el mismo fichero.
- **US4 (P3)**: depende de US1/US2 para validar el modo claro sobre los componentes ya cambiados.

### Within Each User Story

- Tests antes de implementación (T009/T010 antes de T011–T016; T017/T018 antes de T019–T022; T023/T024 antes de T025–T027; T028–T030 antes de T031).
- Implementación antes de integración.
- Checkpoint al final de cada historia.

### Parallel Opportunities

- Fase 1: T001 y T002 no son paralelos (T002 usa T001).
- Fase 2: T003 y T004 en paralelo; T005 y T007 después de T004; T006 en paralelo; T008 después de T005.
- US1: T009/T010 en paralelo; T011–T015 en paralelo (ficheros distintos); T016 después de T012.
- US2: T017/T018 en paralelo; T019/T020/T021 en paralelo; T022 independiente.
- US3: T023/T024 en paralelo; T025/T026 en paralelo; T027 después de T025.
- US4: T028–T030 en paralelo; T031 después de T011.
- Polish: T032 y después T033 (comparten ficheros); T034–T036 secuenciales.

---

## Parallel Example: User Story 1

```bash
# Tests en paralelo
Task: "T009 [P] [US1] Actualizar CommonUiTest ..."
Task: "T010 [P] [US1] Actualizar FavoritesScreenTest ..."

# Implementación en paralelo (ficheros distintos)
Task: "T011 [P] [US1] Actualizar CommonUi.kt ..."
Task: "T012 [P] [US1] Actualizar ExploreScreen.kt ..."
Task: "T013 [P] [US1] Actualizar FavoritesScreen.kt ..."
Task: "T014 [P] [US1] Actualizar HistoryScreen.kt ..."
Task: "T015 [P] [US1] Actualizar CustomStationsScreen.kt ..."
```

## Parallel Example: User Story 2

```bash
Task: "T017 [P] [US2] Actualizar MiniPlayerTest ..."
Task: "T018 [P] [US2] Actualizar EpisodeMiniPlayerTest ..."

Task: "T019 [US2] Refactorizar PlayerUi.kt ..."
Task: "T020 [P] [US2] Refactorizar EpisodeMiniPlayer.kt ..."
Task: "T021 [US2] Eliminar panelSubtitle ..."
```

## Parallel Example: User Story 3

```bash
Task: "T023 [P] [US3] Actualizar TolochaNavigationBarTest ..."
Task: "T024 [P] [US3] Actualizar TolochaTopBarTest ..."

Task: "T025 [US3] Actualizar TolochaNavigationBar ..."   # espera a T016
Task: "T026 [P] [US3] Actualizar TolochaTopBar.kt ..."
Task: "T027 [US3] Añadir divisor y transiciones ..."    # después de T025
```

## Parallel Example: User Story 4

```bash
Task: "T028 [P] [US4] Aserciones claras en CommonUiTest ..."
Task: "T029 [P] [US4] Aserciones claras en MiniPlayerTest ..."
Task: "T030 [P] [US4] Aserciones claras en EpisodeMiniPlayerTest ..."
Task: "T031 [US4] Verificar paleta clara y elevación ..."  # después de T011/T019/T020
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Fase 1 y Fase 2.
2. Completar US1 (T009–T016).
3. **PARAR Y VALIDAR**: Explorar/Favoritos/Historial/Mis emisoras en oscuro y claro; tarjeta activa y play/pausa.
4. Demo si procede.

### Incremental Delivery

1. Fase 1 + Fase 2 → base lista.
2. US1 → validar → demo (MVP de visibilidad de tarjetas).
3. US2 → validar → demo (reproductor compacto).
4. US3 → validar → demo (navegación).
5. US4 → validar → cierre visual.
6. Polish → gates completos.

### Parallel Team Strategy

1. Equipo completa Fase 1 + Fase 2.
2. Después:
   - Persona A: US1.
   - Persona B: US2.
   - Persona C: US3.
3. US4 valida la integración claro/oscuro.
4. Polish y gates al final.

---

## Notes

- [P] = ficheros distintos y sin dependencia pendiente.
- [US1]–[US4] mapean a las historias de `spec.md`.
- Los tests se escriben primero y deben fallar antes de la implementación.
- Cada checkpoint permite validar una historia sin esperar al resto.
- No se añaden dependencias ni se toca Media3, Room, DataStore o el contrato de red.
