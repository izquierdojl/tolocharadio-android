# Data Model: Emisoras personalizadas — Mis emisoras

**Date**: 2026-09-06 | **Feature**: 006-custom-stations

## Entities

### StationDto (existente — reutilizado sin cambios)

`GET /custom-stations` devuelve `{items: StationDto[]}` con `isCustom=true` y sin `favicon` propio. Ver campos completos en `data/remote/dto/StationDtos.kt`. La UI muestra nombre + emblema local de TolochaRadio en lugar del favicon.

### CustomStationResultDto (DTO nuevo)

Respuesta de `POST /custom-stations` (`{station}`), verificado en `apps/web/src/lib/api.ts` (`createCustomStation`).

| Field | Type | Description |
|-------|------|-------------|
| `station` | `StationDto` | Emisora personalizada creada (`isCustom=true`) |

### CreateCustomStationBody (DTO nuevo)

Cuerpo de `POST /custom-stations`. Valores ya recortados (`trim`) por el ViewModel antes de enviar.

| Field | Type | Validation (servidor) |
|-------|------|----------------------|
| `name` | `String` | 1–256 caracteres |
| `url` | `String` | URL `http(s)` del stream |

### StationListDto (existente — reutilizado)

Envoltorio `{items: List<StationDto>}` para `GET /custom-stations`. Sin `pagination`/`hasMore` (el contrato no pagina personalizadas).

### CachedCustomStation (Room Entity nueva)

Persistencia local de lectura para soporte offline (FR-011).

| Field | Type | Room Column | Description |
|-------|------|-------------|-------------|
| `id` | `String` | `@PrimaryKey` | ID asignado por el servidor |
| `name` | `String` | `name` | Nombre dado por el usuario |
| `streamUrl` | `String` | `stream_url` | URL del stream (depuración; la reproducción usa el proxy por id) |
| `cachedAt` | `Long` | `cached_at` | Momento del cacheo (ms) |

**Table name**: `custom_stations_cache`

**Primary Key**: `id` — una fila por emisora (el contrato no declara duplicados; si el servidor devuelve el mismo id dos veces, `REPLACE` lo deduplica).

**Ordering**: orden de inserción del servidor (la web muestra `items` en el orden recibido; sin ordenación cliente).

## State Transitions

```
[Empty] → GET /custom-stations (success, items≠∅) → [Cached + Displayed]
[Empty] → GET /custom-stations (success, items=∅) → [Empty state + form]
[Empty] → GET /custom-stations (failure) → [Error with retry + form visible]
[Cached] → GET /custom-stations (success) → [Updated cache + Displayed]
[Cached] → GET /custom-stations (failure) → [Cached displayed + offline badge]
[Displayed] → POST validado → [Optimistic add + clear form] → success: [Stay] / failure: [Revert + snackbar]
[Displayed] → POST inválido cliente → [Field error, sin llamada red]
[Displayed] → DELETE /custom-stations/:id → [Optimistic remove] → success: [Stay + invalidate favorites] / failure: [Revert + snackbar]
[Playing custom] → DELETE misma emisora → [Lista sin ella, audio continúa hasta stop/cambio]
```

## UI Models (domain / presentation layer)

```kotlin
data class CustomStationItem(
    val station: Station,  // mapped from StationDto (isCustom = true)
)

data class CustomStationFormState(
    val name: String = "",
    val url: String = "",
    val nameError: String? = null,  // p. ej. "Escribe un nombre para la emisora."
    val urlError: String? = null,   // p. ej. "La URL debe empezar por http:// o https://."
    val submitting: Boolean = false,
)
```

## Validation Rules

- `name.trim().isNotEmpty()` en cliente (mensaje: "Escribe un nombre para la emisora."); longitud 1–256 impuesta por el servidor (422 → error por campo).
- `streamUrl`: parseable como URL y esquema `http` o `https` (mensajes: "La URL del stream no es válida." / "La URL debe empezar por http:// o https://."). Sin restricción de host local (difiere de `NormalizeBaseUrlUseCase` — ver research D1).
- `station.id` no vacío (entradas sin ID se descartan).
- Botón Añadir deshabilitado mientras `submitting`; botones de borrado deshabilitados mientras su petición está en curso (FR-012).

## Migration: Room version 3 → 4

```sql
CREATE TABLE IF NOT EXISTS `custom_stations_cache` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `name` TEXT NOT NULL,
    `stream_url` TEXT NOT NULL,
    `cached_at` INTEGER NOT NULL
)
```
