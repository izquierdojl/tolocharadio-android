# Phase 0 — Research: Reordenación intuitiva de favoritos

**Feature**: 0034-jlizquierdo-20260913-favorites-reorder-animation
**Date**: 2026-09-13

Este documento resuelve las decisiones técnicas del plan. No quedan
`NEEDS CLARIFICATION`: el contexto del proyecto (Compose BOM `2025.01.00`) y las
aclaraciones de la spec fijan las elecciones.

## D1 — Animación de la reubicación de filas

- **Decision**: usar `Modifier.animateItem()` en los `items` del `LazyColumn` de
  `FavoritesList` (con el `key = { it.station.id }` ya existente).
- **Rationale**: `animateItem()` anima automáticamente el desplazamiento de las filas
  cuando su índice cambia, sin código manual de interpolación. Está disponible de forma
  estable en `androidx.compose.foundation` 1.7+ (Compose BOM `2025.01.00` trae 1.7.x) y
  respeta la escala de duración de animaciones del sistema. Es exactamente el hueco que
  hoy hace que el movimiento "solo se vea al soltar".
- **Alternatives considered**: `animateItemPlacement()` (deprecado en 1.7, mismo efecto);
  interpolar a mano con `animateDpAsState` (más código y frágil); Accompanist
  Reorderable (dependencia nueva → rechazada por YAGNI).

## D2 — Inicio del arrastre

- **Decision**: sustituir `detectDragGesturesAfterLongPress` por `detectDragGestures`
  sobre el asa, de modo que el arrastre empiece tras el desplazamiento mínimo y sin
  pulsación larga (FR-006, aclaración Q2).
- **Rationale**: el asa ya es un control dedicado y visible; la pulsación larga añadía una
  espera no evidente que contribuía a la sensación de "solo cambia al final". Al tocar y
  mover se inicia de inmediato; un toque sin movimiento no dispara arrastre (lo consume el
  *touch slop*), por lo que no rompe las acciones de la fila (FR-017).
- **Alternatives considered**: mantener long-press con pista visual (seguía siendo poco
  descubrible); long-press en cualquier parte de la fila (interfería con abrir/reproducir).

## D3 — Feedback visual de la fila activa

- **Decision**: llevar el estado `draggingId: String?` en el Composable (`remember
  { mutableStateOf<String?>(null) }`) y aplicar a la fila activa `graphicsLayer` (escala
  ligera), `Modifier.shadow(...)`/elevación y `zIndex(1f)`; el resto de filas se reacomoda
  con D1.
- **Rationale**: distingue con claridad qué se está moviendo (FR-002) con APIs del stack.
  El estado es presentacional y no ensucia el `ViewModel` (Principio I).
- **Alternatives considered**: overlay flotante que sigue el dedo (mayor complejidad y
  riesgo de solapamientos; se puede valorar como mejora futura); solo cambio de color (poco
  perceptible).

## D4 — Respuesta háptica

- **Decision**: usar `LocalHapticFeedback` con `HapticFeedbackType.LongPress` al agarrar y
  `HapticFeedbackType.TextHandleMove` al cruzar una fila; el soltar puede reutilizar
  `LongPress`. Es un `SHOULD` (FR-008) y no debe provocar crash si el dispositivo no lo
  soporta.
- **Rationale**: feedback táctil barato que refuerza el reorden; APIs disponibles en
  `androidx.compose.ui` 1.7 sin dependencias extra.
- **Alternatives considered**: vibración con `Vibrator` + permisos (innecesario y más
  invasivo).

## D5 — Alternativa accesible (menú por fila)

- **Decision**: añadir `DropdownMenu` (M3) con un `IconButton` de desbordamiento en cada
  fila; ítems "Mover arriba"/"Mover abajo", deshabilitados en los extremos (FR-012/FR-013,
  aclaración Q3). El `ViewModel` gana `moveUp(stationId)`/`moveDown(stationId)` que mueven
  una posición y autoguardan.
- **Rationale**: un control real es accesible por defecto para TalkBack y sirve a todos los
  usuarios, sin botones permanentes que recarguen la fila (SC-005).
- **Alternatives considered**: acciones de accesibilidad personalizadas (invisibles para
  quien no usa lector); botones siempre visibles (ruido visual).

## D6 — Comportamiento sin conexión

- **Decision**: cuando `FavoritesUiState.Content.offline == true`, no mostrar/inhabilitar el
  asa ni las acciones del menú; no permitir cambios de orden (FR-018, aclaración Q4).
- **Rationale**: la caché offline es de solo lectura y no existe cola de escritura; evitar
  un guardado condenado a fallar y un estado inconsistente. El flag `offline` ya lo expone
  `FavoritesRepo`/`ObserveFavoritesUseCase` y ya está en el estado de UI.
- **Alternatives considered**: encolar cambios offline (fuera de alcance, YAGNI); permitir y
  revertir al fallar (peor UX).

## D7 — "Reducir movimiento" del sistema

- **Decision**: confiar en que las animaciones de Compose (incluida `animateItem()`)
  respetan la escala de duración del sistema; mantener siempre el resaltado de posición de
  la fila activa. Opcionalmente leer `LocalMotionDurationScale` para omitir efectos propios
  cuando la escala sea 0.
- **Rationale**: cumple FR-014/SC-006 sin mecanismos propios de detección.
- **Alternatives considered**: detectar el ajuste con APIs de plataforma (innecesario).

## D8 — Auto-scroll durante el arrastre

- **Decision**: conservar el auto-scroll existente (`scrollBy` al acercarse al borde) y
  hacer la velocidad proporcional a la cercanía al borde, manteniendo el umbral actual.
- **Rationale**: permite reordenar listas largas sin soltar (FR-007). El código actual ya
  tiene la base (`DRAG_EDGE_PX`, `DRAG_SCROLL_PX`) que se parametriza por distancia.
- **Alternatives considered**: velocidad fija (menos preciso en listas largas).

## D9 — Semántica en el ViewModel y autoguardado

- **Decision**: mantener `moveItem(from, to)` para el arrastre y añadir `moveUp(id)`/
  `moveDown(id)` para el menú. Cualquier cambio de orden llama a `commitOrder()` (autoguardado
  al soltar/elegir). Cancelar o interrumpir el arrastre se trata como un soltar normal,
  conservando el orden mostrado (aclaración Q1).
- **Rationale**: las acciones semánticas por id son fáciles de testear (bordes, no-op) y
  evitan que la UI calcule índices. Se reutiliza `ReorderFavoritesUseCase` (permutación
  exacta) y la restauración existente ante error/conflicto.
- **Alternatives considered**: que la UI calcule índices y llame a `moveItem` (duplica
  lógica y dificulta los tests de límites).

## D10 — Qué NO cambia

- **Decision**: no tocar `ReorderFavoritesUseCase`, `FavoritesRepo`, la API
  (`PUT /favorites/order`), el modelo de datos, ni la reproducción.
- **Rationale**: el contrato de datos ya soporta el reorden; el problema es de UX. Mantiene
  el cambio pequeño y revisable (Principio V).

## Resumen de dependencias

Ninguna dependencia nueva. Todo se apoya en Compose foundation/ui/material3 1.7.x (BOM
`2025.01.00`) ya presente.
