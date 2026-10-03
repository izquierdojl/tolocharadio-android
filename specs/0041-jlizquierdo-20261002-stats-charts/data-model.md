# Data Model: Gráficas de escucha (estadísticas) en la app Android

**Feature**: `0041-jlizquierdo-20261002-stats-charts` | **Date**: 2026-10-02

## Resumen

Esta feature **no introduce persistencia** (sin Room ni DataStore — R2 de `research.md`). El modelo son los DTOs de red del contrato del backend (`contracts/stats-api.md`), el modelo derivado del resumen y los estados de la vista. Los datos de origen (escuchas registradas) los sigue teniendo solo el servidor.

## Entidades de red (DTOs, `data/remote/dto/StatsDtos.kt`)

### `StatsRange`

| Campo | Tipo | Reglas |
|-------|------|--------|
| `from` | `String?` (YYYY-MM-DD) | opcional; si hay `to`, `from <= to` |
| `to` | `String?` (YYYY-MM-DD) | opcional; si hay `from`, `from <= to` |

- Presets de UI: `7d`, `30d`, `90d`, `all` (mapean a rangos; `all` = `{}` sin fechas). Defecto al abrir: `30d` (FR-003).
- Validación backend: `from` posterior a `to` → error `INVALID_PARAMS`; rango de serie temporal > 1830 días → error `INVALID_PARAMS`.

### `StatsTopEntry`

| Campo | Tipo | Reglas |
|-------|------|--------|
| `station` | `StationDto` | reutiliza el DTO existente del catálogo |
| `totalMs` | `Long` | ≥ 0 |

### `StatsTimelineEntry`

| Campo | Tipo | Reglas |
|-------|------|--------|
| `bucket` | `String` | fecha ISO del inicio del intervalo (día/semana/mes según granularidad) |
| `totalMs` | `Long` | ≥ 0; días sin escucha pueden no venir (tratar como 0 en la serie) |

### `StatsHabitEntry`

| Campo | Tipo | Reglas |
|-------|------|--------|
| `weekday` | `Int` | 0..6 (lunes..domingo, etiquetas web `WEEKDAY_LABELS`) |
| `hour` | `Int` | 0..23 |
| `totalMs` | `Long` | ≥ 0 |

### `StatsGenreEntry` / `StatsCountryEntry`

| Campo | Tipo | Reglas |
|-------|------|--------|
| `genre` / `country` | `String` | etiqueta legible |
| `countryCode` | `String?` | solo en países; puede ser `null` |
| `totalMs` | `Long` | ≥ 0 |

### `StatsRecentEntry`

| Campo | Tipo | Reglas |
|-------|------|--------|
| `station` | `StationDto` | reutiliza el DTO existente |
| `startedAt` | `Long` | epoch millis |
| `durationMs` | `Long` | ≥ 0 |

### Respuestas envueltas

`{items: [...]}` en todos los endpoints menos en timeline, que además devuelve `granularity`. El caso de uso `LoadStatsUseCase` agrega las seis respuestas en un `StatsBundle` (top, timeline, habits, genres, countries, recent).

## Entidades derivadas (dominio)

### `StatsSummary` (`ComputeStatsSummaryUseCase`, puro)

| Campo | Tipo | Reglas |
|-------|------|--------|
| `totalMs` | `Long` | suma de `totalMs` de la serie temporal |
| `topStation` | `StationDto?` | `items[0]` del top (si existe) |
| `peakBucket` | `String?` | bucket de mayor `totalMs` de la serie temporal |
| `peakTotalMs` | `Long` | `totalMs` del bucket pico |

- Invariante: `totalMs == 0` ⟺ estado vacío de la vista (SC-006, US1-3).

### `StatsGranularity` (derivada, R5)

`DAY | WEEK | MONTH` — automática: días del rango ≤ 62 → `DAY`; ≤ 370 → `WEEK`; si no → `MONTH`. Se envía al endpoint de timeline.

## Estados de la vista (`StatsViewModel`)

```text
Loading ──carga ok con datos──▶ Content(bundle, summary, granularity, habitsMatrix)
   │──carga ok sin escuchas──▶ Empty
   └──carga fallida──────────▶ Error(message, isAuthError)
Content/Empty/Error ──cambio de periodo o reintento──▶ Loading
Error ──isAuthError──▶ la acción abre la edición del servidor activo (principio IV)
```

- El periodo (`StatsPeriod`) vive como `StateFlow` del ViewModel (fuera de
  `StatsUiState`) para que el filtro sea visible también en `Empty`/`Error` y
  sobreviva a los cambios de estado.
- `Content.granularity` es la granularidad automática calculada por el cliente
  (R5) y `Content.habitsMatrix` la matriz 7×24 derivada de `habits` (US3).
- `bundle.recent.items` se entrega ordenado por `startedAt` descendente.
- Ningún estado conserva datos del periodo anterior tras un cambio (SC-004).

## Relaciones

```text
StatsRange ──(query)──▶ 6 endpoints ──▶ StatsBundle ──▶ ComputeStatsSummaryUseCase ──▶ StatsSummary
StationDto aparece dentro de StatsTopEntry y StatsRecentEntry (catálogo del servidor)
```
