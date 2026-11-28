# Tasks: Refinamiento del reproductor y filas de podcast

**Input**: Design documents from `/specs/0062-izquierdojl-20261128-podcast-player-rows/`

**Prerequisites**: plan.md, spec.md

**Tests**: Incluidos por el plan y la constitución.

## Phase 1: Mini-player

- [X] T001 [US1] Integrar la carátula de 56 dp en la fila de controles de `EpisodeMiniPlayerContent`, ocultar título/podcast cuando hay imagen y conservar texto cuando no la hay, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayer.kt`
- [X] T002 [US1] Cambiar los saltos del mini-player a `Undo`/`Redo` (mismos iconos que el detalle), mantener play 56/saltos 48/Detener 48, tiempos y progreso, y ajustar alturas 72/112 dp en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayer.kt`
- [X] T014 [US1] Centrar el grupo central en el ancho y alto totales de la barra, con márgenes iguales, y llevar tiempos/progreso a la franja inferior sin sumar altura, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayer.kt`
- [X] T015 [US1] Alinear los extremos de la barra de progreso con los tiempos transcurrido/restante, sin invadir la carátula ni Detener, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayer.kt`

## Phase 2: Filas de episodio

- [X] T003 [US2] Rediseñar `EpisodeRow` con imagen de 56 dp, título 2 líneas, podcast 1 línea, meta/progreso, play de 48 dp a la derecha y sin fila de acciones, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/EpisodeRow.kt`
- [X] T004 [US2] Añadir `combinedClickable` con feedback háptico y abrir `EpisodeActionsSheet` al mantener pulsada la fila, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/EpisodeRow.kt`
- [X] T005 [P] [US2] Crear `EpisodeActionsSheet` con descargar/reintentar, cola a continuación, cola al final, favorito, reproducido/pendiente y abrir ficha; ocultar acciones no disponibles, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/EpisodeActionsSheet.kt`
- [X] T006 [US2] Mostrar progreso/estado de descarga en la fila y reflejarlo en el menú; conservar `DownloadUiState` y no romper descargas desactivadas, en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/EpisodeRow.kt`

## Phase 3: Tests y validación

- [X] T007 [P] [US2] Actualizar `PodcastCardsTest` para el nuevo layout y el menú de pulsación larga, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/components/PodcastCardsTest.kt`
- [X] T008 [P] [US2] Crear `EpisodeActionsSheetTest` para las acciones y la ocultación de descargas desactivadas, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/components/EpisodeActionsSheetTest.kt`
- [X] T009 [P] [US1] Actualizar `EpisodeMiniPlayerTest` con casos con/sin imagen, alturas 72/112 y saltos `Undo`/`Redo`, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayerTest.kt`
- [X] T010 [P] [US1] Actualizar previews claro/oscuro del mini-player con imagen y sin imagen, en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayer.kt`
- [X] T011 Ejecutar `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:detekt :app:ktlintCheck :app:lintDebug` y corregir fallos
- [X] T012 Ejecutar `./gradlew :app:connectedDebugAndroidTest` en emulador y corregir fallos
- [X] T013 Recorrer visualmente las listas de podcast y el mini-player con el mock local en `specs/0062-izquierdojl-20261128-podcast-player-rows/quickstart.md` — verificado manualmente en emulador por el usuario (2026-11-28)

## Dependencies

- T001–T002 antes de T009/T010.
- T003–T006 antes de T007/T008.
- T011–T013 al final.

## MVP

T001–T006: mini-player y filas; luego tests y gates.
