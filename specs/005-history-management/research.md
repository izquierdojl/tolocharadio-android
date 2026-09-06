# Research: Historial — lista, reproducción y gestión

**Date**: 2026-09-06 | **Feature**: 005-history-management

## Decision Log

### D1: Auto-refresh history after playback

**Decision**: Subscribe to PlayerViewModel's playing state; on transition to `Playing`, invalidate the history query after a 500ms delay (allows server to register the event via proxy).

**Rationale**: The web app uses `useEffect` subscribing to `isPlaying` with `setTimeout(() => queryClient.invalidateQueries(["history"]), 500)`. The Android equivalent is collecting `PlayerViewModel.ui` StateFlow, detecting `Idle/Buffering → Playing` transition, and calling `refresh()` with a delay.

**Alternatives considered**:
- Polling on interval: rejected — wasteful, unnecessary network calls
- WebSocket push: rejected — backend doesn't support it
- Optimistic local insert: rejected — `playedAt` must come from server to ensure correct ordering

### D2: Deduplication strategy

**Decision**: Client-side deduplication using `groupBy { it.station.id }` + `maxByOrNull { it.playedAt }`, matching the web app's `dedupeStations()` function.

**Rationale**: The backend may return multiple entries per station (one per listen event). The web app deduplicates by keeping only the most recent `playedAt` per `station.id`. This is a pure transformation on the API response — no server changes needed.

**Alternatives considered**:
- Server-side deduplication: rejected — backend contract doesn't support it; changing backend is out of scope
- First-occurrence dedup: rejected — web app uses last-occurrence (most recent), must match

### D3: Room migration strategy (version 2→3)

**Decision**: Create `MIGRATION_2_3` adding `history_cache` table with columns: `id TEXT PK`, `name`, `favicon`, `country`, `language`, `tagsCsv`, `playedAt INTEGER`, `cachedAt INTEGER`. Pattern identical to `MIGRATION_1_2` in `FavoritesCache.kt`.

**Rationale**: The project uses incremental Room migrations (not destructive fallback). `StorageModule.kt` chains migrations explicitly. Adding a new entity requires: new `@Entity`, new `@Dao`, abstract function in `TolochaDb`, version bump, and migration SQL.

**Alternatives considered**:
- `fallbackToDestructiveMigration()`: rejected — loses user data (favorites cache, stations cache)
- Single shared cache table for favorites+history: rejected — different schemas (history has `playedAt` not `addedAt`, no `sortIndex`)

### D4: Confirmation dialog for "Limpiar"

**Decision**: Material3 `AlertDialog` composable with title "¿Limpiar todo el historial?", body text, and two buttons: "Cancelar" (dismiss) and "Limpiar" (destructive, calls `DELETE /history`).

**Rationale**: Spec clarification Q2 chose dialog over immediate action. Follows Android Material3 guidelines for destructive actions. The dialog is a pure Compose component inside `HistoryScreen`, no navigation needed.

**Alternatives considered**:
- BottomSheet confirmation: rejected — overkill for a binary choice
- Snackbar with undo (like favorites remove): rejected — "Limpiar" is total deletion, undo is impractical after server call
- Immediate action + toast (web pattern): rejected by spec clarification

### D5: HistoryViewModel state structure

**Decision**: Sealed `HistoryUiState` with `Loading`, `Empty`, `Content(items, offline)`, `Error(message)`. Items are `List<HistoryItem>` where `HistoryItem` is a UI model wrapping `Station` + `playedAt` (deduplicated). Separate `MutableSharedFlow<String>` for one-shot snackbar messages (delete success/failure, clear success/failure).

**Rationale**: Follows `FavoritesViewModel` pattern exactly. Sealed state enables exhaustive `when` in Compose. SharedFlow for transient messages prevents re-showing on configuration change.

**Alternatives considered**:
- Single data class with nullable fields: rejected — doesn't enforce exhaustive handling
- Using `Result` type in state: rejected — leaks API layer details to UI

### D6: Offline cache behavior

**Decision**: `HistoryRepo` exposes `Flow<HistoryResult>` that first emits cached data (Room) if available, then fetches from API. On API success, updates cache. On API failure with cache available, emits cached data + `offline=true`. On API failure without cache, emits error.

**Rationale**: Matches `FavoritesRepo` pattern. The spec (FR-010) requires "última versión conocida marcada como offline" and "primer arranque sin caché MUST mostrar error con reintento". Room provides the persistent cache layer.

**Alternatives considered**:
- Network-only (no cache): rejected — spec requires offline support
- Memory-only cache: rejected — doesn't survive app restart
- Encrypted cache: rejected — history data is not sensitive (station names + timestamps)
