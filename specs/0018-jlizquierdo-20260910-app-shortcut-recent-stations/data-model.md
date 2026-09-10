# Data Model: Emisoras recientes en los accesos directos del icono

**Feature**: `0018-jlizquierdo-20260910-app-shortcut-recent-stations` | **Date**: 2026-09-10

Esta feature **no añade persistencia**: los accesos directos son estado derivado del historial existente y se reconstruyen en cada publicación. No hay cambios de esquema Room, migraciones ni claves nuevas en DataStore.

## Modelos nuevos

### `ShortcutStation` (dominio, puro)

Emisora lista para publicarse como acceso directo.

| Campo | Tipo | Reglas |
|-------|------|--------|
| `stationId` | `String` | Obligatorio, no vacío; proviene de `StationDto.id` |
| `name` | `String` | Obligatorio; si viene vacío se usa "Emisora" en la etiqueta |
| `faviconUrl` | `String?` | Opcional; si falta o falla la descarga se usa el icono de la app |
| `rank` | `Int` | 0 = más reciente; se reasigna 0..N-1 en cada publicación |

**Origen**: `List<HistoryEntryDto>` (historial ya deduplicado y ordenado por `HistoryRepo`) o `List<CachedHistoryEntry>` (caché offline). El caso de uso `BuildShortcutStationsUseCase` no confía en el orden de entrada: deduplica por `stationId` conservando `playedAt` máximo, ordena descendente y recorta a `maxSlots`.

**Invariantes**:
- Una sola entrada por `stationId` (FR-003).
- Orden estrictamente por `playedAt` descendente (FR-001).
- Tamaño `≤ maxSlots` con `maxSlots = min(platformMax, 5)` y `≥ 0` (FR-002).
- Incluye emisoras personalizadas si están en el historial (clarificación 2026-09-10).
- Lista vacía ⇒ no se publica ningún acceso (FR-004).

### `ShortcutSpec` (plataforma, puro/testable)

Representación neutral de un acceso directo antes de tocar `ShortcutManagerCompat`.

| Campo | Tipo | Reglas |
|-------|------|--------|
| `id` | `String` | Determinista: `hist-<stationId>`; pequeña y estable entre publicaciones |
| `shortLabel` | `String` | Nombre recortado a un máximo (p. ej. 25 chars) preservando inicio del nombre (FR-014) |
| `longLabel` | `String` | Nombre completo recortado a un máximo mayor (p. ej. 60 chars) |
| `iconUrl` | `String?` | Favicon para intentar descargar; `null` ⇒ icono de app |
| `intentAction` | `String` | `com.izquierdojl.tolocharadio.OPEN_STATION` |
| `intentExtras` | `Map<String, String>` | `station_id` (obligatorio), `station_name` (opcional) |
| `rank` | `Int` | Igual que en `ShortcutStation` |

### `PendingShortcut` (transitorio, en memoria)

Petición de reproducción pendiente de resolver.

| Campo | Tipo | Reglas |
|-------|------|--------|
| `stationId` | `String?` | `null` = sin pendiente; se limpia al consumirse (Play/GoLogin/Unavailable) |
| `stationName` | `String?` | Solo para mensajes/snackbar; nunca se persiste |

**Ciclo de vida**: `MainActivity` (onCreate/onNewIntent) → `PendingShortcutHolder` → `TolochaNavGraph` lo consume una vez resuelto; no sobrevive a la muerte del proceso (limitación aceptada: si el proceso muere antes de consumir, el intent original se redespacha en el siguiente arranque).

### `ShortcutLaunchResolution` (dominio, sellado)

Resultado de `ResolveShortcutLaunchUseCase`.

| Variante | Datos | Efecto |
|----------|-------|--------|
| `Play` | `station: StationDto` | `playerVm.play(station)` + `playerVm.openFullPlayer()` (FR-005) |
| `GoLogin` | `reason: String?` | Navegar a `Routes.LOGIN` + snackbar en español (FR-007) |
| `Wait` | — | `AuthState.Loading`: no consumir el pendiente |
| `Unavailable` | `message: String` | Snackbar accionable; no abrir el reproductor (FR-011) |

## Entidades existentes reutilizadas

| Entidad | Ubicación | Uso en esta feature |
|---------|-----------|---------------------|
| `HistoryEntryDto` (`station: StationDto`, `playedAt: Long`) | `data/remote/dto/HistoryDtos.kt` | Fuente principal; `HistoryRepo.items` ya deduplicado |
| `CachedHistoryEntry` (`id`, `name`, `favicon`, `country`, `language`, `tagsCsv`, `playedAt`, `cachedAt`) | `data/local/HistoryCache.kt` | Fuente offline (FR-015). No persiste `isCustom`, dato irrelevante para publicar (solo id/nombre/icono) |
| `StationDto` (`id`, `name`, `favicon`, `isCustom`, …) | `data/remote/dto/StationDtos.kt` | Objeto que consume `PlayerViewModel.play()` |
| `AuthState` (`Loading`/`Authenticated`/`Unauthenticated(reason)`) | `core/session/SessionManager.kt` | Decide Play/GoLogin/Wait y dispara limpieza al cerrar sesión |
| `PlayerState` (sellado) | `feature/player/PlayerViewModel.kt` | Sin cambios; el full player visible se añade como estado independiente |

## Reglas de publicación y limpieza

| Evento | Acción | Requisito |
|--------|--------|-----------|
| Cambia `HistoryRepo.items` (nueva reproducción, borrado individual, limpieza) | Recalcular y `publish()` con ranks 0..N-1 | FR-009, FR-011, SC-004 |
| App pasa a primer plano con sesión | `historyRepo.list()` (red → caché si `Unavailable`) y `publish()` | FR-015, US3.4 |
| Sesión `Authenticated` (login/restore) | `historyRepo.list()` y `publish()` | FR-001 |
| Sesión `Unauthenticated` o lista vacía | `clear()` (elimina todos los dinámicos) | FR-004, FR-010 |
| Logout / cambio de instancia | `clearNow()` inmediato | FR-010, SC-003 |
| Launcher sin soporte (`maxSlots == 0`) | No publicar; no fallar | FR-012 |

## Sin cambios de esquema

- `TolochaDb` sigue en versión 6; no se añaden entidades, DAOs ni migraciones.
- No se añaden preferencias DataStore ni secretos.
- Los accesos directos del lanzador son estado del sistema operativo, no de la app: se reconstruyen al publicar y se eliminan con `clear()`.
