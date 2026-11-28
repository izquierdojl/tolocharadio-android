# UI Contracts: Visibilidad UI y navegación

**Feature**: `0061-izquierdojl-20261128-ui-visibilidad-navegacion` | **Date**: 2026-11-28

Contratos observables de los componentes tocados. No fijan implementación interna; fijan qué se ve, qué se pulsa y qué anuncia TalkBack.

## C1. `StationCard`

**Entradas**: `station`, `isFavorite`, `playback: StationPlaybackUiState`, `onOpen`, `onPlay`, `onToggleFavorite`, `isCustom`.

| Zona | Acción | Resultado |
|------|--------|-----------|
| Tarjeta (fuera de botones) | `onOpen` | Abre la ficha |
| Botón inferior izquierdo | `onPlay` | Inactiva → reproduce; `Playing` → pausa; `Paused` → reanuda; `Error` → reintenta; `Loading` → no accionable |
| Corazón superior derecho | `onToggleFavorite` | Alterna favorito |
| Badge superior izquierdo | — | Indicador de estado, no clicable |

- Superficie `cardContainer`, borde 1 dp `cardBorder`, esquinas 16 dp, elevación 0 dp en oscuro y 2 dp en claro.
- Carátula 16:9; sin favicon → emblema sobre `cardPlaceholder`; favicon roto → emblema, nunca hueco vacío.
- Botón de play 40-44 dp visual (mínimo táctil 48 dp): `PlayRestBackground`/`PlayRestContent` en reposo, `PlayActiveBackground`/`PlayActiveContent` si es la activa. Icono `Pause` en `Playing`, `PlayArrow` en el resto.
- `contentDescription` del botón: `Pausar`, `Reanudar` o `Reproducir`.
- Título 1 línea, país/idioma 1 línea, hasta 3 chips y bitrate; sin cambios de jerarquía salvo contraste.
- El badge no duplica el nombre de la emisora; anuncia solo el estado.

## C2. `StationListItem` y filas

**Entradas**: `station`, `isFavorite`, `playback`, `onOpen`, `onToggleFavorite` (y `onPlay` donde ya exista).

- La fila sin botón de play mantiene `onOpen` al tocar; el badge de estado se muestra junto al título.
- Favoritos/Historial/Mis emisoras conservan su botón de play; si la emisora es la activa, alterna play/pausa y cambia el icono.
- Las tarjetas de cuadrícula de Historial y Mis emisoras usan `cardContainer`, `cardBorder`, `cardPlaceholder`, elevación clara y un botón de play de 48 dp táctil.
- Badge compacto: icono 14-16 dp con `contentDescription` (`En reproducción`, `Pausado`, `Cargando`, `Error`); sin texto visible para no romper la densidad de la fila.
- El toque en el texto abre la ficha; el botón de play no abre la ficha.

## C3. `StationPlaybackBadge`

| Estado | Icono | Texto en tarjeta | Semántica |
|--------|-------|------------------|-----------|
| `Playing` | ecualizador o `GraphicEq` | `En directo` | `En reproducción` |
| `Paused` | `Pause` | `Pausado` | `Pausado` |
| `Loading` | `CircularProgressIndicator` 12 dp | `Cargando` | `Cargando emisora` |
| `Error` | `ErrorOutline` | `Error` | `Error de reproducción` |
| `Inactive` | — | — | No se compone |

- Fondo `surface` con alpha 0.85 y contenido `onSurface`; legible sobre cualquier carátula.
- En tarjeta: pill con texto; en fila: solo icono.
- No depende solo del color: icono + texto/semántica.

## C4. `NowPlayingContainer`

- `Surface` con `playerContainer`, `RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)`, `tonalElevation = 0.dp`.
- `HorizontalDivider` de 1 dp `playerBorder` en la parte superior, dentro del `Surface`.
- Ocupa todo el ancho; las esquinas inferiores son 0 para unirse a la barra inferior.
- Acepta `onClick` opcional (podcast) y conserva el clic de las acciones internas.
- No añade padding exterior; cada contenido decide el suyo.

## C5. `MiniPlayerContent` (radio)

| Estado | Contenido | Acciones |
|--------|-----------|----------|
| `Playing` | Carátula 40 dp, nombre, `En directo` (+ `· dispositivo` si Cast) | Play/pausa 56 dp + silenciar 48 dp |
| `Paused` | Carátula 40 dp, nombre, `Pausado` (+ dispositivo) | Play/pausa 56 dp + silenciar 48 dp |
| `Buffering` | Carátula, nombre, mensaje de carga | Cancelar (48 dp) |
| `Reconnecting` | Carátula, nombre, mensaje de reconexión | Indicador de progreso |
| `Error` | Carátula, nombre, mensaje | Reintentar/editar servidor; sin silenciar |

- No muestra país, idioma, códec, bitrate ni `panelSubtitle`.
- Altura 64 dp con fuente por defecto; el contenido se recorta a una línea por texto.
- El nombre accesible del panel anuncia estado + emisora (`Sonando: X`, `Pausado: X`, etc.).
- Tocar la identidad abre `StationInfoSheet`; no interrumpe la reproducción.

## C6. `EpisodeMiniPlayerContent` (podcast)

| Zona | Contenido | Acción |
|------|-----------|--------|
| Fila 1 | Carátula 40 dp, título a 1 línea, podcast/contexto `labelSmall` | Abre el reproductor completo |
| Fila 2 | `FastRewind` 48 dp, play/pausa 56 dp centrado, `FastForward` 48 dp, Detener 48 dp | Saltos, play/pausa, detener |
| Pie | Tiempo transcurrido y restante 10 sp + progreso 2-3 dp | No interactivo |

- Máximo dos líneas de texto; el título largo se elide.
- Sin duración: solo transcurrido, sin barra.
- `contentDescription` de saltos: `Retroceder N segundos` / `Avanzar N segundos`.
- El play/pausa es visualmente mayor (icono 30 dp); el resto de iconos 22-24 dp.
- Altura 112 dp con fuente por defecto; Detener es secundario y no desplaza el play del centro.
- Tocar la superficie abre el reproductor completo; los botones no propagan el toque.

## C7. `TolochaNavigationBar`

- 4 destinos en radio (`Explorar`, `Favoritos`, `Historial`, `Mis emisoras`) y 5 en podcast (`Inicio`, `Descubrir`, `Suscripciones`, `Cola`, `Favoritos`).
- `label` visible, `alwaysShowLabel = true`, `labelSmall` a 1 línea con elipsis.
- Seleccionado: indicador `navIndicator`, icono `navSelectedContent`, etiqueta `onSurface` y semibold.
- No seleccionado: `navUnselectedContent`.
- Mantiene `TooltipBox` para nombres largos/elididos y `testTag` por destino para tests de anchura.
- El icono del destino no lleva `contentDescription` cuando la etiqueta visible está presente; TalkBack anuncia un solo nombre por destino.
- A 200 % de fuente y 320 dp no hay solapamiento; la etiqueta puede elidirse y el nombre accesible sigue completo.
- No cambia el orden ni el número de destinos; la insignia de cola se conserva.

## C8. `TolochaTopBar`

- Mantiene logo, selector Radio|Podcasts, Cast y menú.
- Añade un `HorizontalDivider` de 1 dp `chromeDivider` en la parte inferior, sin aumentar la altura.
- No cambia el comportamiento de los botones ni el menú.

## C9. Movimiento y accesibilidad

- `NavHost`: `fadeIn(tween(200))` / `fadeOut(tween(180))`; sin slide.
- Con reducción de animaciones del sistema, el cambio es inmediato (MotionDurationScale).
- Todos los controles interactivos ≥48 dp; play/pausa ≥56 dp; iconos con `contentDescription`, salvo el icono de la barra inferior cuando la etiqueta visible ya da el nombre (C7).
- El color no es la única señal de estado: badge, icono, texto y peso acompañan.
- Los anuncios de TalkBack existentes se conservan; no se duplican etiquetas visibles/accesibles.

## C10. Matriz de tests de contrato

| Contrato | Test |
|----------|------|
| C1 | `CommonUiTest`: onOpen vs onPlay, icono pausa/play, badge, fallback |
| C2 | `FavoritesScreenTest`: badge en lista/grid, play/pausa |
| C3 | `CommonUiTest`: semántica de los 4 estados |
| C4 | `MiniPlayerTest` / `EpisodeMiniPlayerTest`: presencia del contenedor y altura |
| C5 | `MiniPlayerTest`: `En directo`/`Pausado`, 2 acciones, sin línea técnica, error/carga |
| C6 | `EpisodeMiniPlayerTest`: 2 líneas, tiempos, stop, saltos, sin duración |
| C7 | `TolochaNavigationBarTest`: 4/5 etiquetas, selección, anchura, fuente 200 % |
| C8 | `TolochaTopBarTest`: acciones intactas |
| C9 | Tests de accesibilidad y `assertWidthIsAtLeast`/`assertHeightIsAtLeast` |
