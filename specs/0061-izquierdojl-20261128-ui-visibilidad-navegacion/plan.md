# Implementation Plan: Visibilidad UI y navegación

**Branch**: `0061-izquierdojl-20261128-ui-visibilidad-navegacion` | **Date**: 2026-11-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/0061-izquierdojl-20261128-ui-visibilidad-navegacion/spec.md`

## Summary

Mejora visual y de interacción sin cambios de datos, backend ni reproducción: (1) una capa semántica de color que separa tarjetas, panel de reproducción y chrome, corrigiendo el contraste de textos y chips; (2) tarjetas con borde, superficie y estado de reproducción visible, con el botón de play separado del resto de la tarjeta; (3) panel de radio compacto y panel de podcast a dos filas con play centrado, saltos, progreso, tiempos y Detener secundario; (4) barra inferior con etiquetas, selección visible y transiciones suaves; (5) modo claro coherente y tests de contraste que impiden regresiones.

Enfoque: un `CompositionLocal` con tokens semánticos (`TolochaColors`) sobre la paleta existente, un contenedor común `NowPlayingContainer` para ambos reproductores y un modelo de estado de tarjeta (`StationPlaybackUiState`) derivado del `PlayerState`/Cast ya existente. Sin dependencias nuevas y sin tocar Media3.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17

**Primary Dependencies**: Jetpack Compose (BOM 2026.05.00) + Material3, Navigation Compose, Hilt 2.60.1, Coil 2.6.0, Media3 1.11.0 (sin cambios); sin dependencias nuevas (YAGNI)

**Storage**: N/A (sin cambios de Room, DataStore, credenciales ni caché)

**Testing**: JUnit + kotlinx-coroutines-test + Turbine + MockK para unit tests puros; `composeTestRule` para tarjetas, paneles y barra inferior; Detekt + ktlint + Android Lint en CI

**Target Platform**: Android `minSdk` 26, `targetSdk`/`compileSdk` 37

**Project Type**: mobile-app de un solo módulo `app`, organizado por feature (`explore/`, `favorites/`, `history/`, `customstations/`, `player/`, `podcasts/player/`)

**Performance Goals**: 60 fps en listas y transiciones; transiciones de navegación de 180-220 ms; sin trabajo de red, CPU ni memoria nuevo; sin regresión en arranque en frío

**Constraints**: MVVM + Clean (`ui → domain → data`); ViewModel sin `Context` de Activity; Compose M3 sin XML/Views; Media3 intacto; oscuro por defecto con paleta verde/ocre; HTTPS-only; sin PII en logs; textos en español; WCAG AA (4.5:1 texto normal, 3:1 texto grande y controles/estados esenciales); `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug` obligatorios

**Scale/Scope**: 4 pantallas de emisoras (Explorar, Favoritos, Historial, Mis emisoras), 2 paneles (radio y podcast), barra inferior y cabecera; 1 módulo; 0 endpoints nuevos; ~15 ficheros de producción y ~8 de tests

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. MVVM + Clean por capas**: PASS. Todo el cambio vive en `ui` (tokens, composables y mapeo de estado de presentación). No se añaden reglas de negocio ni se accede a red/BD desde la UI. El estado de reproducción se lee de `PlayerViewModel` y se transforma con funciones puras; los ViewModels existentes no cambian de contrato.
- **II. Stack Kotlin-First, Compose M3 y Media3**: PASS. Solo Compose M3, sin XML/Views nuevos, sin `MediaPlayer`/`VideoView`, sin pantallas de login y sin dependencias nuevas. Media3 y el `MediaSessionService` no se tocan.
- **III. Calidad Test-First**: PASS. Se añaden unit tests puros para contraste y mapeo de estado, y Compose tests para tarjeta, panel de radio, panel de podcast y barra inferior. Gates estáticos y de compilación se mantienen.
- **IV. Streaming Robusto y Manejo de Errores**: PASS. Los estados `Buffering`, `Reconnecting` y `Error` conservan sus mensajes y acciones; el panel no promete "sonando" en carga/error; Cast sigue mostrando el dispositivo y el estado efectivo. No se altera la lógica de reproducción.
- **V. Simplicidad Modular (YAGNI)**: PASS. Un solo módulo, sin abstracciones nuevas más allá de tres tipos de UI pequeños (`TolochaColors`, `StationPlaybackUiState`, `NowPlayingContainer`). Se elimina `panelSubtitle` si queda sin consumidores, en lugar de conservarlo.

Re-evaluación post-diseño (Fase 1): sin violaciones nuevas; no se añade `Complexity Tracking`.

## Project Structure

### Documentation (this feature)

```text
specs/0061-izquierdojl-20261128-ui-visibilidad-navegacion/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── design-tokens.md
│   └── ui-contracts.md
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/ui/theme/
│   ├── Theme.kt                          # onSurfaceVariant Pine300; provee TolochaColors
│   ├── TolochaColors.kt                  # NUEVO: tokens semánticos + CompositionLocal
│   └── ThemeMode.kt                      # sin cambios
├── core/ui/components/
│   ├── CommonUi.kt                       # StationCard/StationListItem: borde, superficie, onOpen/onPlay
│   ├── StationPlaybackBadge.kt           # NUEVO: indicador "En directo"/"Pausado"/carga/error
│   ├── NowPlayingContainer.kt            # NUEVO: superficie, borde superior y forma del panel
│   └── SectionHeader.kt                  # sin cambios
├── core/ui/playback/
│   └── StationPlaybackUiState.kt         # NUEVO: enum + NowPlayingUi + stateFor
├── core/ui/navigation/
│   ├── TolochaNavGraph.kt                # etiquetas, colores, divisor, transiciones, player a Explore
│   └── TolochaTopBar.kt                  # divisor inferior
├── feature/player/
│   ├── PlayerUi.kt                       # MiniPlayerContent compacto; PlayerPanelUi privado; sin línea técnica
│   ├── PlayerStateMapper.kt              # NUEVO: PlayerState/Cast -> NowPlayingUi
│   ├── PanelHelpers.kt                   # retirar panelSubtitle si queda sin uso
│   └── StationInfoSheet.kt               # sin cambios: ya muestra Audio/país/idioma
├── feature/podcasts/player/
│   └── EpisodeMiniPlayer.kt              # dos filas, play 56 dp, saltos, tiempos, stop
├── feature/explore/ExploreScreen.kt      # recibe player; tarjeta abre ficha y play reproduce
├── feature/favorites/FavoritesScreen.kt  # estado activo y play/pausa en grid/lista
├── feature/history/HistoryScreen.kt      # tokens de tarjeta + estado activo en grid/lista
└── feature/customstations/CustomStationsScreen.kt  # tokens de tarjeta + estado activo en grid/lista

app/src/test/java/com/izquierdojl/tolocharadio/
├── core/ui/theme/ContrastTest.kt         # nuevos pares de tokens
└── feature/player/
    ├── PlayerStateMapperTest.kt          # NUEVO: mapeo y Cast
    └── PanelHelpersTest.kt               # retirar panelSubtitle si se elimina

app/src/androidTest/java/com/izquierdojl/tolocharadio/
├── CommonUiTest.kt                       # StationCard: onOpen/onPlay, badge, tamaños
├── core/ui/navigation/TolochaNavigationBarTest.kt  # etiquetas + 5 destinos
├── feature/player/MiniPlayerTest.kt      # radio: En directo/Pausado, 2 acciones
├── feature/podcasts/player/EpisodeMiniPlayerTest.kt  # 2 líneas, tiempos, stop
└── feature/favorites/FavoritesScreenTest.kt  # indicador activo en grid/lista
```

**Structure Decision**: Se mantiene el módulo único `app` y la organización por feature existente. Los tokens y los componentes compartidos viven en `core/ui`; el mapeo de estado de reproducción vive en `feature/player` para que `core/ui/components` no dependa de una feature. No se crean módulos ni paquetes de dominio nuevos.

## Design Decisions

Las decisiones completas y sus alternativas están en [research.md](research.md). Resumen:

- **R1. Tokens semánticos**: `TolochaColors` + `LocalTolochaColors` en `TolochaTheme`; no se reescriben los roles M3 para evitar efectos globales. `onSurfaceVariant` pasa a Pine300 en oscuro para chips y texto secundario.
- **R2. Contraste**: tarjeta `#24422E` (1.74:1 vs fondo) + borde Pine500 (1.77:1), chip 6.53:1, panel `#1B3121` con borde 2.23:1. En claro, tarjeta blanca + borde 1.54:1 + elevación 2 dp (SC-002 ajustado: la superficie de 1.3:1 es objetivo de oscuro). El 3:1 se reserva a controles/estados esenciales; los bordes decorativos se miden con SC-002.
- **R3. Tarjeta**: `StationCard` recibe `StationPlaybackUiState`, separa `onOpen` y `onPlay`, muestra pausa/play según estado y un badge no basado solo en color.
- **R4. Panel**: `NowPlayingContainer` con esquinas superiores de 20 dp, borde superior de 1 dp y color explícito; sin `tonalElevation` ocre. Radio 64 dp; podcast 112 dp con dos líneas y tiempos integrados.
- **R5. Navegación**: etiquetas visibles, indicador `primary` con icono `onPrimary` (8.59:1 en oscuro), etiqueta `onSurface` semibold, icono sin `contentDescription` para no duplicar el anuncio y transición fade de 200 ms.
- **R6. Tests**: contraste unitario de los pares usados, mapeo puro de estado y Compose tests de tarjeta/paneles/barra.

## Implementation Strategy

1. **Tokens y contraste**: crear `TolochaColors`, ajustar `onSurfaceVariant`, extender `ContrastTest`; sin cambios visibles todavía.
2. **Tarjetas**: actualizar `StationCard`, `StationListItem`, `StationPlaybackBadge`, `ExploreScreen`, `FavoritesScreen`, `HistoryScreen` y `CustomStationsScreen`; cablear `NowPlayingUi` y el play/pausa.
3. **Paneles**: crear `NowPlayingContainer`, refactorizar `MiniPlayerContent` y `EpisodeMiniPlayerContent`, retirar la línea técnica del panel y conservar estados/Cast.
4. **Navegación**: etiquetas, colores, divisor de cabecera y barra, transiciones del `NavHost`.
5. **Cierre**: tests de UI, previews claro/oscuro, limpieza de `panelSubtitle` y gates completos.

La descomposición en tareas ejecutables corresponde a `/speckit.tasks`.

## Complexity Tracking

Sin violaciones constitucionales que justificar; sección intencionalmente vacía.
