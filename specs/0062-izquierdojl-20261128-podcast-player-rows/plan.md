# Implementation Plan: Refinamiento del reproductor y filas de podcast

**Branch**: `0062-izquierdojl-20261128-podcast-player-rows` | **Date**: 2026-11-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/0062-izquierdojl-20261128-podcast-player-rows/spec.md`

## Summary

Refinar el reproductor de podcast y las filas de episodios sin tocar reproducción, datos ni red: (1) carátula de 56 dp integrada en la fila de controles del mini-player, ocultando título/podcast cuando hay imagen; (2) iconos de salto iguales al detalle (`Undo`/`Redo`); (3) filas con imagen de 56 dp, play de 48 dp a la derecha y acciones secundarias en un `ModalBottomSheet` al mantener pulsado. Se aplica a todas las listas que usan `EpisodeRow`.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM 17

**Primary Dependencies**: Compose M3, Material Icons Extended, Coil, Hilt; sin dependencias nuevas

**Storage**: N/A

**Testing**: JUnit + Compose UI tests; `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug`, `connectedDebugAndroidTest`

**Target Platform**: Android `minSdk` 26, `targetSdk`/`compileSdk` 37

**Project Type**: mobile-app de un módulo `app`

**Performance Goals**: sin cambios de rendimiento; filas más ligeras al eliminar la fila de acciones

**Constraints**: MVVM + Clean; Compose M3; sin XML; sin permisos ni dependencias; tokens de 0061; textos en español; accesibilidad TalkBack

**Scale/Scope**: `EpisodeMiniPlayer`, `EpisodeRow`, nuevo `EpisodeActionsSheet`; 4-5 pantallas de podcast; 0 endpoints

## Constitution Check

- **I. MVVM + Clean**: PASS. Solo UI; los callbacks existentes de los ViewModels se conservan.
- **II. Compose M3 / Media3**: PASS. Sin XML ni cambios de reproducción; iconos Material existentes.
- **III. Test-First**: PASS. Tests de mini-player, fila y menú; gates completos.
- **IV. Streaming robusto**: PASS. No se toca el player ni sus estados; el panel conserva tiempos/progreso/Detener.
- **V. YAGNI**: PASS. Un componente nuevo (`EpisodeActionsSheet`) reutilizado por todas las listas; sin abstracciones extra.

## Project Structure

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── feature/podcasts/player/EpisodeMiniPlayer.kt   # imagen 56 en controls, texto condicional, Undo/Redo
├── core/ui/components/EpisodeRow.kt               # imagen 56, play 48, long-press, progreso descarga
├── core/ui/components/EpisodeActionsSheet.kt      # NUEVO: acciones al mantener pulsado
├── core/ui/components/DownloadUiState.kt          # sin cambios
└── feature/player/EpisodePlayerControls.kt        # referencia de iconos, sin cambios

app/src/androidTest/java/com/izquierdojl/tolocharadio/
├── core/ui/components/PodcastCardsTest.kt         # fila + menú
├── core/ui/components/EpisodeActionsSheetTest.kt  # NUEVO
└── feature/podcasts/player/EpisodeMiniPlayerTest.kt # con/sin imagen y altura
```

## Design Decisions

- **Mini-player**: si hay imagen, una `Box` de 56 dp con carátula a la izquierda, controles centrados y Detener a la derecha; sin fila de texto. Si no hay imagen, se mantiene la fila de título/podcast. Tiempos y progreso debajo. Alturas 72/112 dp.
- **Saltos**: `Icons.AutoMirrored.Filled.Undo/Redo`, igual que `EpisodePlayerControls`; `contentDescription` con los segundos configurados.
- **Fila**: `ListItem` con imagen 56 dp, título 2 líneas, podcast 1 línea, meta/progreso y `FilledIconButton` de 48 dp a la derecha. `combinedClickable` para abrir ficha o menú.
- **Menú**: `ModalBottomSheet` con descargar/reintentar, cola a continuación, cola al final, favorito, reproducido/pendiente y abrir ficha; se ocultan las acciones no disponibles. Feedback háptico en la pulsación larga.
- **Descarga**: progreso lineal bajo la meta; estado “Descargado/Completado” en la meta; el menú permite reintentar.
- **Alcance**: todas las llamadas a `EpisodeRow`; `EpisodeCard` de cuadrícula no cambia.

## Implementation Strategy

1. Mini-player image-first + iconos + alturas.
2. Filas con play principal y sin fila de acciones.
3. `EpisodeActionsSheet` y long press.
4. Progreso/estado de descarga en la fila.
5. Tests, previews y gates.

## Complexity Tracking

Sin violaciones constitucionales; sección vacía.
