# Research: Emisoras personalizadas — Mis emisoras

**Date**: 2026-09-06 | **Feature**: 006-custom-stations

Technical Context no contenía ningún `NEEDS CLARIFICATION` (stack, almacenamiento, testing y alcance se conocen por la constitución y el precedente 005). Las decisiones siguientes resuelven los puntos de diseño propios de esta feature, verificados contra el código web real (`CustomStations.tsx`, `api.ts`) y el código Android existente.

## Decision Log

### D1: Validación del formulario en un UseCase puro de dominio

**Decision**: Nuevo `ValidateCustomStationUseCase` (Kotlin puro, `@Inject`) con dos funciones: `name(raw): String?` (null si válido: no vacío tras `trim`; el límite 256 lo impone el servidor) y `streamUrl(raw): String?` (null si válido: parseable y esquema `http`/`https`). Los mensajes de error en español viven en el UseCase, igual que `ValidateAuthUseCase`.

**Rationale**: La web valida en el handler `submit` (nombre no vacío, `new URL()`, protocolo http/https). Llevarlo a `domain` lo hace testeable sin Android y reutiliza el patrón `ValidateAuthUseCase`. NO se reutiliza `NormalizeBaseUrlUseCase`: ese rechaza `http://` salvo hosts locales, pero un stream `http://` público es válido para una emisora (la web acepta cualquier `http(s)`).

**Alternatives considered**:
- Validación inline en el ViewModel: rechazada — mezcla reglas de negocio con estado UI y es el antipatrón que la constitución prohíbe (I).
- Reutilizar `NormalizeBaseUrlUseCase`: rechazada — semántica distinta (base de instancia vs URL de stream) y rechazaría streams http públicos legítimos.
- Validar longitud máxima 256 en cliente: rechazada — la web no lo hace; el servidor responde 422 con `details` y el cliente lo muestra por campo (FR-010).

### D2: Reutilización de DTOs y forma del contrato Retrofit

**Decision**: `GET /custom-stations` reutiliza `StationListDto` (`{items: List<StationDto>}`) — el `StationDto` existente ya tiene `isCustom` y la respuesta web es `{items: Station[]}`. Solo se añaden `CreateCustomStationBody(name, url)` y `CustomStationResultDto(station)`. La interfaz `CustomStationsApi` sigue el patrón `FavoritesApi`/`HistoryApi` (mismo `baseUrl` `/api/v1`, `Response<T>`, `OkResult` para el DELETE).

**Rationale**: El contrato web confirma las tres formas (`{items}`, `{station}`, `{ok:true}`). `StationListDto` ya modela `{items: StationDto}` y evita un wrapper duplicado de un solo uso (YAGNI). `Response<T>` permite mapear códigos (401→refresh, 422→detalles por campo) en el repo como hacen `FavoritesRepo`/`HistoryRepo`.

**Alternatives considered**:
- Nuevo `CustomStationListDto` dedicado: rechazado — idéntico a `StationListDto`, duplicación sin beneficio.
- Devolver `StationDto` directamente en el POST: rechazado — el contrato devuelve `{station}` envoltorio; el test de serialización lo verificaría y fallaría.

### D3: Caché Room con migración 3→4

**Decision**: Nueva entidad `CachedCustomStation` en tabla `custom_stations_cache` con columnas `id TEXT PK`, `name`, `streamUrl`, `cachedAt INTEGER`. Sin columnas de favicon/país/idioma: las personalizadas se muestran siempre con el emblema local (no tienen imagen propia). `MIGRATION_3_4` con `CREATE TABLE`, patrón idéntico a `MIGRATION_2_3` en `HistoryCache.kt`. `replaceAll` transaccional tras cada `GET` correcto.

**Rationale**: La spec exige última-lista-conocida offline (FR-011). El esquema mínimo cubre lo que la UI muestra (nombre + reproducir por id + URL para depuración). Guardar campos que nunca se leen violaría YAGNI.

**Alternatives considered**:
- `fallbackToDestructiveMigration()`: rechazada — borraría cachés de favoritas/historial (precedente D3 de 005).
- Reutilizar `history_cache` o tabla genérica: rechazada — esquemas distintos (`playedAt` vs nada, orden por inserción vs por fecha); mezclar dominios complica invalidación.
- Sin caché (solo red): rechazada — FR-011 exige offline con última lista conocida.

### D4: Invalidación cruzada de Favoritos tras eliminar

**Decision**: Tras un `DELETE /custom-stations/:id` con éxito, `CustomStationsRepo` (o el ViewModel vía `FavoritesRepo.refresh()`) invalida la caché de favoritos y emite refresh, espejo del `queryClient.invalidateQueries(["favorites"])` de la web. El mecanismo concreto es llamar al punto de refresco público de `FavoritesRepo` que ya usa `HistoryViewModel`/`FavoritesViewModel` para recargas.

**Rationale**: Una personalizada puede ser favorita; al eliminarla debe desaparecer también de Favoritos (US-4, escenario 3). La web lo hace invalidando la query de favoritos; el equivalente Android es forzar refresh del repo de favoritos.

**Alternatives considered**:
- No invalidar (esperar a reabrir Favoritos): rechazada — la spec lo exige explícitamente (FR-007) y dejaría un favorito fantasma navegable.
- Borrar en cascada en Room local: rechazado — la verdad es el servidor; el refresh trae el estado correcto sin lógica de cascada duplicada.

### D5: Estructura de estado del ViewModel (lista + formulario)

**Decision**: `CustomStationsUiState` sellado con `Loading`, `Empty`, `Content(items, offline)`, `Error(message)` (patrón `HistoryViewModel`), más un `CustomStationFormState(name, url, nameError, urlError, submitting)` separado en el mismo ViewModel. Los errores de campo vienen de `ValidateCustomStationUseCase`; `submitting=true` deshabilita el botón Añadir (FR-012). `MutableSharedFlow<String>` para snackbars de un disparo (añadida/eliminada/error).

**Rationale**: La pantalla tiene dos responsabilidades (lista + formulario) pero un solo propietario de estado evita sincronización entre ViewModels. El formulario siempre visible (FR-003) se modela como estado independiente de la lista, así sigue operativo aunque la lista esté en `Error`. Sigue el patrón sellado + SharedFlow de `FavoritesViewModel`/`HistoryViewModel`.

**Alternatives considered**:
- Dos ViewModels (lista + formulario): rechazado — el formulario muta la lista (añadir optimista); dos propietarios exigirían un bus de eventos entre ellos.
- Errores de campo como strings en el estado de lista: rechazado — mezcla dominios y re-renderiza la lista al escribir en el formulario.

### D6: Navegación — sustituir el placeholder existente

**Decision**: En `TolochaNavGraph.kt` (líneas 195-197) sustituir `HomeScreen(...)` del destino `CUSTOM_STATIONS` por `CustomStationsScreen(onStation/onExplore, player)` envuelto en el mismo guard `if (authState is Authenticated) ... else LoginScreen` del bloque HISTORY. Sin cambios en `Routes.kt`, `AUTH_REQUIRED` ni `BOTTOM_DESTS` (ya incluyen Mis emisoras).

**Rationale**: El trabajo de navegación previo (spec 002 + 005) ya registró ruta, icono y guardia; el destino apunta a un placeholder temporal. El diff es de 3 líneas y reutiliza el patrón probado de HISTORY/FAVORITES. La reproducción usa `player.play(station)` compartido (patrón `HistoryScreen` línea 104), por lo que la continuidad al navegar y el registro de historial vía proxy vienen gratis.

**Alternatives considered**:
- Nueva ruta o renombrar `CUSTOM_STATIONS`: rechazado — paridad web exige la ruta existente y el bottom bar ya la referencia.
- Pantalla con su propio player: rechazado — violaría spec 004 (un único `PlayerViewModel` a ámbito de Activity).
