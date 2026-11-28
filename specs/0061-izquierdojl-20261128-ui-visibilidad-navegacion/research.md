# Research: Visibilidad UI y navegación

**Feature**: `0061-izquierdojl-20261128-ui-visibilidad-navegacion` | **Date**: 2026-11-28

Sin `NEEDS CLARIFICATION` abiertos: la sesión del 2026-11-28 resolvió las 5 decisiones materiales (alcance del indicador, play/pausa en tarjeta, estado de radio pausada, ubicación de Detener y proporción de carátula). Este documento consolida las decisiones técnicas y las alternativas descartadas.

## R1. Capa de tokens semánticos

- **Decision**: Añadir `TolochaColors` (data class inmutable) y `LocalTolochaColors` (`staticCompositionLocalOf`) en `core/ui/theme/TolochaColors.kt`, provistos por `TolochaTheme`. Tokens: `isDark`, `cardContainer`, `cardBorder`, `cardPlaceholder`, `playerContainer`, `playerBorder`, `chromeDivider`, `navIndicator`, `navSelectedContent`, `navUnselectedContent`.
- **Rationale**: La paleta M3 tiene los roles `surface`/`surfaceContainer*` colapsados; reescribirlos globalmente afectaría a menús, sheets y campos. Una capa semántica pequeña permite cambiar solo tarjeta, panel, chips y navegación, y testear cada par. `staticCompositionLocalOf` es suficiente porque el tema cambia poco y el árbol se recompondrá al cambiar claro/oscuro.
- **Alternatives considered**: Cambiar `surfaceContainer*` en `darkColorScheme` (descartado: efecto dominó en toda la app y riesgo de romper sheets); hardcodear colores en cada composable (descartado: sin fuente única ni tests); usar `MaterialTheme.colorScheme` con `copy` local (descartado: difícil de testear y de documentar).

## R2. Contraste y ajuste de SC-002

- **Decision**: En oscuro, `onSurfaceVariant` pasa de Pine400 a Pine300; la tarjeta usa `#24422E` (`Pine650`) con borde Pine500; el panel usa `#1B3121` (`Pine725`) con borde Pine500. En claro, la tarjeta es blanca con borde `LightLineStrong` y 2 dp de elevación. SC-002 se ajusta: el objetivo de superficie 1.3:1 aplica a oscuro; en claro la separación se mide por borde (≥1.5:1) y elevación.
- **Rationale**: El chip actual (Pine400 sobre Pine800) da 4.13:1 y no cumple AA. Al aclarar la tarjeta a `#1A2E20`, Pine400 bajaría a 3.89:1; por eso superficie y texto se mueven juntos. En claro, blanco sobre `#EEF3EF` da 1.12:1 y no hay forma de llegar a 1.3 sin oscurecer todo el fondo; el borde 1.54:1 + sombra de 2 dp sí separa visualmente y mantiene la paleta web. FR-002 se interpreta como 3:1 para controles/estados esenciales; los bordes decorativos siguen SC-002 (≥1.5:1), evitando un conflicto de umbrales.
- **Alternatives considered**: Mantener Pine400 y solo añadir borde (descartado: el chip sigue por debajo de AA); oscurecer el fondo claro a ~#D8E3DB (descartado: cambia toda la app y rompe paridad web); usar una tarjeta más oscura que el fondo (descartado: invierte la jerarquía y empeora la lectura).

Pares medidos (WCAG):

| Par | Oscuro | Claro | Objetivo |
|-----|--------|-------|----------|
| Texto secundario / tarjeta | 4.73:1 | 5.91:1 | ≥4.5 |
| Texto de chip / chip | 6.53:1 | 4.53:1 | ≥4.5 |
| Borde / tarjeta | 1.77:1 | 1.54:1 | ≥1.5 |
| Tarjeta / fondo | 1.74:1 | 1.12:1 + sombra | ≥1.3 en oscuro |
| Borde / panel | 2.23:1 | 1.45:1 | separación visual |
| Indicador nav / barra | 8.59:1 | 5.61:1 | ≥3 |

## R3. Modelo de estado de la tarjeta

- **Decision**: Nuevo enum `StationPlaybackUiState { Inactive, Playing, Paused, Loading, Error }` y `NowPlayingUi(stationId, state)` en `core/ui/playback/`. La derivación desde `PlayerState`/`CastPlayerState` vive en `feature/player/PlayerStateMapper.kt` como función pura. `Playing`/`Paused` comparan `station.id`; `Buffering`/`Reconnecting` → `Loading`; `Error` → `Error`; el resto → `Inactive`.
- **Rationale**: La UI necesita un estado estable y pequeño, independiente de los detalles de Media3. El mapeo puro se testea sin Hilt y evita duplicar `when (state)` en cuatro pantallas. Cast se resuelve con la misma función: `(castState as? Cast)?.playerState ?: localState`.
- **Alternatives considered**: Pasar `PlayerState` a las tarjetas (descartado: acopla `core/ui/components` a `feature/player` y a Media3); añadir `isPlaying`/`isPaused` sueltos (descartado: dos booleanos permiten estados imposibles); crear un `StateFlow` nuevo en el ViewModel (descartado: no hace falta, el estado ya existe y se puede combinar en UI).

## R4. Acciones de la tarjeta

- **Decision**: `StationCard` separa `onOpen` (tocar la tarjeta) de `onPlay` (botón). Si la emisora es la activa, el botón alterna play/pausa; si no, arranca. `StationListItem` mantiene el clic de fila como `onOpen` y no añade botón de play (la spec lo permite); las filas de Favoritos/Historial/Mis emisoras ya tienen botón y lo conservan.
- **Rationale**: Hoy el icono de play de la cuadrícula abre la ficha, mientras que la lista de favoritas sí reproduce: es una affordance engañosa. Separar callbacks elimina la ambigüedad y permite que el icono cambie a pausa solo cuando corresponde. Para el arranque se usa `player.play(station)`; para el toggle, `player.toggle()`, ambos ya existentes.
- **Alternatives considered**: Mantener un único callback y abrir siempre la ficha (descartado: el icono seguiría mintiendo); hacer que toda la tarjeta reproduzca (descartado: rompe el acceso a la ficha y el patrón de la lista); añadir un menú de más opciones (descartado: ruido y fuera de alcance).

## R5. Contenedor común del panel

- **Decision**: `NowPlayingContainer` envuelve radio y podcast en un `Surface` con `playerContainer`, `RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)`, borde superior de 1 dp (`HorizontalDivider`) y sin `tonalElevation`. El panel de radio mide 64 dp; el de podcast 112 dp.
- **Rationale**: El `Surface` actual con `tonalElevation = 3.dp` aplica un velo ocre (~10,8 %) sobre Pine950 y queda a ~1.17:1 del fondo; además, las esquinas de 16 dp en los cuatro lados lo despegan de la barra y lo hacen parecer otra tarjeta. Un color explícito, esquinas superiores y un borde superior lo anclan al chrome sin competir con el contenido.
- **Alternatives considered**: Mantener `tonalElevation` y cambiar solo el color (descartado: el velo sigue tiñendo de ocre y el resultado depende del tema); usar `BottomAppBar` (descartado: no encaja con el layout de dos reproductores y el mini-player persistente); sombra profunda (descartado: invisible en oscuro y costosa en listas).

## R6. Panel de radio

- **Decision**: En reproducción normal muestra carátula 40 dp, nombre, estado `En directo` (sonando) o `Pausado` (pausa), play/pausa de 56 dp y silenciar de 48 dp. Si hay Cast, la línea de estado añade `· <dispositivo>`. La línea técnica (`panelSubtitle`) sale del panel; país/idioma/codec/bitrate ya están en `StationInfoSheet`.
- **Rationale**: Es el mínimo que permite entender qué suena y controlarlo. Elimina la segunda línea técnica que el usuario señaló como ruido. `panelSubtitle` queda sin consumidores y se elimina con sus tests; `resolveCopyLink` se conserva.
- **Alternatives considered**: Mantener la línea técnica (descartado: contradice FR-006 y la petición); mostrar solo el nombre sin estado (descartado: no distingue pausa de directo); añadir un tercer botón (descartado: FR-006 limita a dos acciones en normal).

## R7. Panel de podcast

- **Decision**: Dos filas: (1) carátula 40 dp, título del episodio a una línea y podcast/contexto en `labelSmall`; (2) controles centrados con saltos de 48 dp, play/pausa de 56 dp y Detener de 48 dp como acción secundaria; tiempos de 10 sp en los extremos de la fila de controles y barra de progreso de 2-3 dp al pie. Altura objetivo 112 dp (4 dp superior + 40 + 56 + 12).
- **Rationale**: El panel actual usa tres filas, título a dos líneas y play de 44 dp; el nuevo conserva toda la información útil con menos altura. Los tiempos integrados evitan una cuarta fila. `FastRewind`/`FastForward` comunican mejor "retroceder/avanzar" que `Undo`/`Redo`. Detener se mantiene por clarificación, pero subordinado visualmente.
- **Alternatives considered**: Tres filas con tiempos separados (descartado: supera 112 dp y vuelve al problema de altura); quitar Detener (descartado por clarificación); usar iconos `Replay10`/`Forward10` (descartado: los segundos son configurables y el icono mentiría).

## R8. Barra inferior

- **Decision**: Añadir `label` visible a cada `NavigationBarItem`, `alwaysShowLabel = true`, etiqueta `labelSmall` a una línea con elipsis y peso semibold en la seleccionada. Colores: indicador `primary` (ocre) con icono `onPrimary`; etiqueta seleccionada `onSurface`; no seleccionados `onSurfaceVariant`. Mantener los tooltips. Añadir `testTag` por destino para tests de anchura.
- **Rationale**: Hoy no se pasa `label`, así que solo hay iconos y el indicador `secondaryContainer` (Pine800) sobre `surfaceContainer` (Pine900) da 1.16:1. Las etiquetas ya eran un requisito de `0047 FR-021`; el indicador ocre da 8.59:1 en oscuro y 5.61:1 en claro. El icono se deja sin `contentDescription` para que la etiqueta visible sea el único nombre accesible; el peso y el color de la etiqueta añaden una señal que no depende solo del color.
- **Alternatives considered**: Solo etiqueta seleccionada (descartado: FR-010 pide etiquetas completas); indicador Pine600 (descartado: 1.94:1, insuficiente para un estado esencial); mantener iconos con tooltip (descartado: no resuelve la descubribilidad).

## R9. Movimiento

- **Decision**: Transiciones `fadeIn(tween(200))` / `fadeOut(tween(180))` en el `NavHost`, sin slide. Se confía en `MotionDurationScale` de Compose para respetar la reducción de animaciones del sistema.
- **Rationale**: Un fade corto suaviza el cambio de sección sin competir con el contenido ni añadir desplazamientos que puedan chocar con el mini-player. 180-220 ms es el rango pedido.
- **Alternatives considered**: Shared-axis X (descartado: más vistoso pero más riesgo de jank y de solaparse con el chrome); sin animación (descartado: la petición pide una navegación más agradable); animar también el mini-player en cada navegación (descartado: ya tiene su propia entrada/salida).

## R10. Metadatos técnicos

- **Decision**: `StationInfoSheet` sigue mostrando país/idioma en la cabecera y codec/bitrate/SSL en la sección Audio; no se añaden campos. El panel deja de mostrar `panelSubtitle`. Se elimina `panelSubtitle` y sus tests si no queda ningún consumidor.
- **Rationale**: La ficha ya es el lugar de detalle y no interrumpe la reproducción; mover allí la información cumple FR-006 sin perder datos. Eliminar el helper muerto evita deuda.
- **Alternatives considered**: Eliminar los metadatos (descartado: se pierde información útil); duplicarlos en la ficha y el panel (descartado: ruido y dos fuentes).

## R11. Accesibilidad, tamaños y fuente

- **Decision**: Play/pausa 56 dp (icono 30 dp), saltos y Detener 48 dp (icono 22-24 dp), silenciar 48 dp. Títulos a una línea con elipsis. Etiquetas de barra a una línea con elipsis y nombre accesible completo. Badge de tarjeta con `contentDescription` (`En reproducción`, `Pausado`, `Cargando`, `Error`) y sin duplicar el nombre del icono de play.
- **Rationale**: El código actual usa 40/44 dp en varios controles del panel de podcast, por debajo del mínimo táctil de 48 dp. La fuente al 200 % en 320 dp puede elidir etiquetas, pero no debe solaparlas; el `NavigationBar` de M3 usa `heightIn(min = 80.dp)` y puede crecer, lo que se verificará en test.
- **Alternatives considered**: Mantener 40/44 dp (descartado: accesibilidad); etiquetas a dos líneas (descartado: rompe la altura de la barra y la estética); ocultar la etiqueta seleccionada en fuente grande (descartado: FR-012).

## R12. Estrategia de tests

- **Decision**: Extender `ContrastTest` con los pares de R2; añadir `PlayerStateMapperTest` (todos los estados, station mismatch, Cast); actualizar `CommonUiTest`, `MiniPlayerTest`, `EpisodeMiniPlayerTest`, `TolochaNavigationBarTest` y `FavoritesScreenTest`. Añadir aserciones de tamaño con `assertWidthIsAtLeast`/`assertHeightIsAtLeast` donde sea posible. Gates: `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug`; `connectedDebugAndroidTest` para los flujos Compose.
- **Rationale**: La constitución exige tests de UI en flujos críticos; estos lo son (reproductor, tarjetas, navegación). Los tests de contraste impiden que un cambio futuro vuelva a Pine400 sin darse cuenta.
- **Alternatives considered**: Solo verificación manual (descartado: regresiones silenciosas); tests de screenshot (descartado: no hay infraestructura y añadiría dependencias).

## R13. Terminología y limpieza

- **Decision**: La etiqueta canónica de la barra es **Favoritos** (coincide con `BottomDest` y `0047`); la sección puede seguir titulándose "Tus favoritos". "Cast" es el término único para la emisión externa. Se elimina `panelSubtitle`; no se renombran rutas ni destinos.
- **Rationale**: Evita que la spec y los tests esperen "Favoritas" mientras la app muestra "Favoritos". Mantiene la coherencia con el resto de specs y con el código.
- **Alternatives considered**: Renombrar la barra a "Favoritas" (descartado: cambio de producto no pedido y rompe paridad con la web); mantener ambos términos (descartado: ambigüedad en acceptance tests).
