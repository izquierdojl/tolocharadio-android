# Phase 1 — Contrato: UI de reordenación de favoritos

**Feature**: 0034-jlizquierdo-20260913-favorites-reorder-animation
**Date**: 2026-09-13

La feature no expone APIs públicas ni endpoints: el contrato con el backend
(`PUT /favorites/order`) no cambia. El contrato relevante es el de la **UI de la pantalla
de Favoritos** (`FavoritesScreenContent` / `FavoriteListActions`), que es lo que los tests
Compose verifican. Es un contrato interno del módulo `app`.

## Superficie de la pantalla

`FavoritesScreenContent(state: FavoritesUiState, actions: FavoriteListActions, mode: ViewMode)`

- `state`: estado de UI de Favoritos (sin cambios en su forma).
- `mode`: `LIST` o `GRID`. El reorden **solo** existe en `LIST` (FR-015).
- `actions`: delegación de eventos al `ViewModel`.

## Acciones (contrato)

`FavoriteListActions` (ampliada con las acciones del menú accesible):

| Acción | Firma | Comportamiento |
|--------|-------|----------------|
| `onStation` | `(String) -> Unit` | Abre la ficha. Sin cambios. |
| `onExplore` | `() -> Unit` | Atajo del estado vacío. Sin cambios. |
| `onPlay` | `(FavoriteDto) -> Unit` | Reproduce. Sin cambios. |
| `onRemove` | `(String) -> Unit` | Quita con deshacer. Sin cambios. |
| `onMove` | `(Int, Int) -> Unit` | Movimiento en vivo durante el arrastre. Sin cambios. |
| `onCommit` | `() -> Unit` | Guarda el orden al soltar. Sin cambios. |
| `onMoveUp` | `(String) -> Unit` | **NUEVA**: mueve una posición arriba y guarda. |
| `onMoveDown` | `(String) -> Unit` | **NUEVA**: mueve una posición abajo y guarda. |
| `onRetry` | `() -> Unit` | Reintenta carga. Sin cambios. |
| `onEditServer` | `() -> Unit` | Edita servidor ante error de credenciales. Sin cambios. |

Contrato del `ViewModel` asociado (testeable unitariamente):

- `moveItem(from: Int, to: Int)`: reordena en memoria (ya existe).
- `moveBy(stationId: String, delta: Int)`: mueve la favorita `delta` posiciones
  (la UI usa -1 y +1) y llama a `commitOrder()`; no-op en los extremos, con
  `offline == true` o si `stationId` no existe.
- `commitOrder()`: autoguardado (ya existe).

## Contrato visual y de accesibilidad

| Elemento | Visible cuando | Comportamiento | `contentDescription` |
|----------|----------------|----------------|----------------------|
| Asa de arrastre (`DragHandle`) | `mode == LIST`, `!offline`, `items.size > 1` | Arrastre inmediato; eleva/resalta la fila activa; autoscroll en bordes | `"Reordenar"` (existente) |
| Fila activa | durante el arrastre | escala + sombra + `zIndex` (FR-002) | — |
| Botón de menú (`MoreVert`) | `mode == LIST`, `!offline`, `items.size > 1` | Abre `DropdownMenu` | `"Más opciones"` |
| Ítem "Mover arriba" | menú abierto | Mueve una posición; deshabilitado si es la primera | texto `"Mover arriba"` |
| Ítem "Mover abajo" | menú abierto | Mueve una posición; deshabilitado si es la última | texto `"Mover abajo"` |
| Indicador de guardado | `savingOrder == true` | `LinearProgressIndicator` (existente) | — |

### Reglas de habilitación

1. `items.size <= 1`: no se muestran asa ni menú de movimiento (nada que reordenar).
2. `offline == true`: no se muestran/inhabilitan asa y menú (FR-018).
3. `mode == GRID`: la rejilla de tarjetas no ofrece reorden (FR-015).
4. En los extremos, el ítem correspondiente del menú aparece **deshabilitado** (FR-013).

## Eventos y resultados verificables (para tests)

1. Arrastrar el asa y cruzar una fila ⇒ el orden mostrado cambia antes de soltar (FR-004).
2. Soltar ⇒ se llama a `onCommit` una vez y el orden se guarda (FR-009).
3. Cancelar/intrrumpir el gesto ⇒ se conserva el orden mostrado y se guarda (FR-005, Q1).
4. Elegir "Mover arriba"/"Mover abajo" ⇒ la fila cambia una posición y se guarda (FR-012).
5. Extremo ⇒ ítem deshabilitado y sin efecto (FR-013).
6. `offline` ⇒ sin asa ni acciones de movimiento (FR-018).
7. Reordenar no altera la reproducción (FR-016): los eventos de player no se tocan.

## Fuera de contrato (no cambia)

- `ReorderFavoritesUseCase`, `FavoritesRepo.reorder`, `FavoritesApi`, `ReorderBody`,
  `FavoriteDto`, esquema Room y OpenAPI del backend.
