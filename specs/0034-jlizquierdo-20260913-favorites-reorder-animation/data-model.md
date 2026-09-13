# Phase 1 — Data Model: Reordenación intuitiva de favoritos

**Feature**: 0034-jlizquierdo-20260913-favorites-reorder-animation
**Date**: 2026-09-13

Esta feature es de presentación/UX: **no introduce entidades persistidas ni cambios de
esquema**. El modelo de datos del reorden ya existe (spec 003) y se reutiliza tal cual.

## Entidades existentes (sin cambios)

### Favorita — `FavoriteDto`

- **Origen**: `data/remote/dto` (serializada contra la API).
- **Campos relevantes**: `station` (`StationDto`, con `id`, `name`, `country`, `language`,
  `tags`, …) y `addedAt` (epoch ms).
- **Identidad**: `station.id` es único en la lista; se usa como `key` del `LazyColumn` y como
  identificador para mover/quitar.
- **Regla de unicidad**: la lista se deduplica por `station.id` en `FavoritesRepo.list()`
  (VR-01) antes de publicarse.

### Orden de favoritas

- **Representación en red**: `PUT /favorites/order` con cuerpo `{ stationIds: string[] }`
  (`ReorderBody`), que debe ser la **permutación exacta** de los ids actuales.
- **Representación en memoria**: el orden de `List<FavoriteDto>` es la fuente de verdad
  local autorizada; el servidor es la verdad última.
- **Validación** (`ReorderFavoritesUseCase`): aborta sin llamar a la API si el destino no
  es permutación exacta (tamaño distinto, duplicados, ids en blanco).

## Estado de UI (existente, se amplía su uso)

`FavoritesUiState.Content` (`feature/favorites/FavoritesViewModel.kt`):

| Campo | Tipo | Uso en esta feature |
|-------|------|---------------------|
| `items` | `List<FavoriteDto>` | Orden mostrado; cambia en vivo durante el arrastre y con el menú. |
| `offline` | `Boolean` | Si es `true`, se deshabilitan asa y acciones de movimiento (FR-018). |
| `savingOrder` | `Boolean` | Indicador de guardado en curso (FR-009); ya se pinta con `LinearProgressIndicator`. |
| `pendingUndo` | `PendingUndo?` | Sin cambios (quitar/deshacer). |

## Estado transitorio nuevo (solo Composable, no persistido)

| Estado | Tipo | Dónde | Propósito |
|--------|------|-------|-----------|
| `draggingId` | `String?` | `FavoritesScreen.kt` (`remember`) | Identifica la fila activa para elevarla/resaltarla (FR-002). No entra en el `ViewModel`. |

## Reglas de comportamiento (derivadas de la spec)

1. Mover `from → to` (arrastre) o `±1` (menú) solo modifica el orden en memoria; el
   guardado se dispara al confirmar (soltar o elegir acción) — `commitOrder()`.
2. Cancelar o interrumpir el arrastre conserva el orden mostrado y lo guarda igual que un
   soltar normal (aclaración Q1).
3. No-op cuando `from == to`, índice fuera de rango, o el extremo lo impide (FR-013).
4. Si el guardado falla, se restaura `confirmed` (último orden confirmado) y se emite
   mensaje (FR-010); si el servidor rechaza por conflicto, gana su orden (FR-011).
5. Sin conexión (`offline == true`), el reorden queda deshabilitado y no se emite escritura
   (FR-018).

## Transiciones de estado (orden)

```text
confirmado(servidor) --[drag/menu]--> mostrado(modificado) --[soltar/elegir]--> guardando
   ^                                                                   |
   |                       Ok: confirmado = mostrado  <-----------------+
   +---------------------- Err/conflicto: se restaura confirmado <------+
```

No hay migraciones Room ni cambios de DTO/OpenAPI.
