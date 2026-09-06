# Data Model: Historial — lista, reproducción y gestión

**Date**: 2026-09-06 | **Feature**: 005-history-management

## Entities

### HistoryEntry (API DTO)

Source: backend `izquierdojl/tolocharadio`, `GET /history` response.

| Field | Type | Description |
|-------|------|-------------|
| `station` | `StationDto` | Emisora reproducida (todos los campos de Station) |
| `playedAt` | `Long` | Timestamp Unix en milisegundos de la reproducción |

**Wrapper**: `HistoryListDto` — `{ items: HistoryEntryDto[] }`

**Relationships**: `station` embebe un `StationDto` completo (no es referencia/ID).

### CachedHistoryEntry (Room Entity)

Persistencia local de lectura para soporte offline.

| Field | Type | Room Column | Description |
|-------|------|-------------|-------------|
| `id` | `String` | `@PrimaryKey` | ID de la emisora (`station.id`) — PK deduplicada |
| `name` | `String` | `name` | Nombre de la emisora |
| `favicon` | `String?` | `favicon` | URL del favicon |
| `country` | `String?` | `country` | País |
| `language` | `String?` | `language` | Idioma |
| `tagsCsv` | `String` | `tags_csv` | Tags separados por coma |
| `playedAt` | `Long` | `played_at` | Timestamp de la reproducción más reciente |
| `cachedAt` | `Long` | `cached_at` | Momento del cacheo (para depuración) |

**Table name**: `history_cache`

**Primary Key**: `id` (station ID) — una entrada por emisora (deduplicada).

**Ordering**: Por `playedAt DESC` (más reciente primero).

### Station (existing DTO — reference)

Campos relevantes mostrados en cada entrada de historial:
- `id`, `name`, `favicon`, `country`, `language`, `tags[]`
- `codec`, `bitrate` (para línea secundaria de calidad)

## State Transitions

```
[Empty] → GET /history (success) → [Cached + Displayed]
[Empty] → GET /history (failure) → [Error with retry]
[Cached] → GET /history (success) → [Updated cache + Displayed]
[Cached] → GET /history (failure) → [Cached displayed + offline badge]
[Displayed] → DELETE /history/:stationId → [Optimistic remove] → success: [Stay] / failure: [Revert + snackbar]
[Displayed] → DELETE /history → [Optimistic clear] → success: [Empty] / failure: [Revert + snackbar]
```

## UI Model (domain layer)

```kotlin
data class HistoryItem(
    val station: Station,    // mapped from StationDto
    val playedAt: Long,      // most recent play timestamp
)
```

Deduplication: `groupBy { it.station.id }.map { (_, entries) -> entries.maxByOrNull { it.playedAt }!! }`

## Validation Rules

- `playedAt` > 0 (malformed entries filtered out client-side)
- `station.id` non-empty (entries without ID are dropped)
- Deduplicated list sorted by `playedAt DESC`

## Migration: Room version 2 → 3

```sql
CREATE TABLE IF NOT EXISTS `history_cache` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `name` TEXT NOT NULL,
    `favicon` TEXT,
    `country` TEXT,
    `language` TEXT,
    `tags_csv` TEXT NOT NULL DEFAULT '',
    `played_at` INTEGER NOT NULL,
    `cached_at` INTEGER NOT NULL
)
```
