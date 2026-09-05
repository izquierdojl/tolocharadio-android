# Data Model: Favoritos — lista, marcado y navegación

**Feature**: `specs/003-favorites-management/spec.md` | **Date**: 2026-09-05

## Entities

### Favorite (dominio, desde `GET /favorites`)

| Campo | Tipo | Reglas |
|-------|------|--------|
| `station.id` | `String` (UUID del servidor) | requerido, único por cuenta; clave de `favoriteIds` |
| `station.name` | `String` | requerido; si vacío → placeholder "Emisora sin nombre", nunca rompe la lista |
| `station.favicon` | `String?` | opcional; si nulo/vacío o falla la carga → placeholder emblema (Coil + fallback) |
| `station.country` / `station.language` | `String?` | opcionales; línea `país · idioma` omite ausentes |
| `station.tags` | `List<String>` | máx. 3 chips en mayúsculas en tarjeta; resto se ignora en UI |
| `station.bitrate` | `Int?` | opcional, alineado a la derecha si existe |
| `addedAt` | `Long` (ms) | requerido (default `0` en DTO); se muestra como fecha relativa ("hace 2 días"); `0`/futuro → se oculta |

Relaciones: `Favorite` 1—1 `Station` (snapshot del catálogo en el momento del guardado); un `station.id` aparece como máximo una vez por cuenta (el servidor es la autoridad; el cliente deduplica por `id` conservando la primera ocurrencia del orden del servidor).

### Station (referencia)

Modelo de catálogo ya existente (`StationDto` en `data/remote/dto/StationDtos.kt`): `{id, name, url, homepage, favicon, country, countryCode, language, tags[], codec, bitrate, isSsl, lastCheckOk, votes, clickCount, isCustom}`. `url` nunca se usa directamente (solo proxy autenticado existente). Sin cambios de esquema.

### FavoriteOrder (valor, no entidad persistente remota propia)

Secuencia ordenada de `station.id` tal como la devuelve `GET /favorites`. La verdad es el servidor. Regla de validación (dominio, `ReorderFavoritesUseCase`): el `PUT` solo se envía si el conjunto propuesto es permutación exacta del último confirmado (mismo tamaño, sin duplicados, sin ids ajenos); si no, se aborta localmente con aviso. Conflicto concurrente → gana servidor (descartar local + aviso).

### CachedFavorite (persistencia local, Room, NUEVO)

Caché de solo lectura para FR-010. Tabla `favorites_cache`:

| Columna | Tipo | Notas |
|---------|------|-------|
| `id` | `TEXT PK` | `station.id` |
| `name` | `TEXT` | |
| `favicon` | `TEXT?` | |
| `country` / `language` | `TEXT?` | |
| `tagsCsv` | `TEXT` | `tags.joinToString(",")` (máx. lo necesario para 3 chips) |
| `addedAt` | `INTEGER` | para fecha relativa |
| `sortIndex` | `INTEGER` | posición del orden del servidor |
| `cachedAt` | `INTEGER` | ms de la última escritura OK |

DAO: `replaceAll(items)` (transacción: `clear + insert`), `loadOrdered()` (`ORDER BY sortIndex`), `clear()`. `TolochaDb`: `version 1 → 2` + migración probada (ver quickstart). Limpieza: al hacer logout o cambiar `baseUrl` se vacía (igual que la sesión/tokens; coherente con spec 001).

## UiState (presentación)

```text
FavoritesUiState =
  Loading
  | Empty                          # 0 favoritas, con CTA a Explorar
  | Content(items, isFavorite=id→true, offline, savingOrder, pendingUndo?)
  | Error(message, offlineItems?)   # con caché → muestra lista + aviso; sin caché → solo error + reintento
```

Transiciones: `Loading → (Empty | Content | Error)`; `Content --toggle--> Content(optimista) --ok--> Content / --err--> Content(revertido) + mensaje`; `Content --quitar--> Content + Snackbar(10 s) --deshacer--> Content(reinsertado) / --expira--> Content`; `Content --drag--> Content(reordenado) + savingOrder --ok--> Content / --err--> Content(último confirmado) + reintento`; `* --refresh--> Loading(parcial, conserva lista)`.

## Validation rules (resumen exigible en tests)

- VR-01: dedup por `station.id` ante respuesta con duplicados.
- VR-02: `PUT order` solo con permutación exacta (mismo set, sin duplicados).
- VR-03: `stationId` en blanco nunca se envía a `POST/DELETE`.
- VR-04: mensajes de error siempre ES y accionables; nunca volcar `message` crudo ni PII (mapeo `ApiError → userMessage` existente).
- VR-05: `sortIndex` continuo `0..n-1` al persistir caché.
