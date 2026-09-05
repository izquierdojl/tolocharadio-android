# Research: Favoritos — lista, marcado y navegación

**Feature**: `specs/003-favorites-management/spec.md` | **Date**: 2026-09-05

No quedaban `NEEDS CLARIFICATION` técnicos abiertos (spec + 5 aclaraciones 2026-09-05). Esta fase consolida decisiones verificadas contra el repo (código y `gradle/libs.versions.toml`).

## R-01: Reutilizar `FavoritesApi`/`FavoritesRepo`/`ToggleFavoriteUseCase` y añadir solo `list` consumida + `PUT order`

- **Decision**: extender `data/remote/api/FavoritesApi.kt` con `PUT("favorites/order")` (`ReorderBody(ids)`) y consumir el `GET list()` ya declarado; ampliar `FavoritesRepo` con `list()/observe()/reorder()`; crear `ObserveFavoritesUseCase` + `ReorderFavoritesUseCase`; reutilizar `ToggleFavoriteUseCase` sin cambios para el corazón global.
- **Rationale**: el repo ya declara `list/add/remove` y el toggle optimista con rollback (ver `FavoritesRepo.kt`, `ToggleFavoriteUseCase.kt`, `ExploreViewModel.toggleFavorite`). Añadir solo lo que falta minimiza churn y respeta Clean por capas (constitución I/V).
- **Alternatives considered**: nuevo `FavoritesService` separado (rechazado: duplica repo sin segundo consumidor); mover el toggle al VM de favoritos (rechazado: rompería el marcado coherente en Explorar/ficha).

## R-02: Fuente única de `favoriteIds` compartida entre Explorar y Favoritos

- **Decision**: `FavoritesRepo` expone `Flow<Set<String>>` de ids favoritas (refresco tras `list/add/remove`); `ExploreViewModel` y `FavoritesViewModel` la observan en lugar de mantener sets locales divergentes.
- **Rationale**: la spec exige marcado coherente en el 100 % de pantallas (SC-003, FR-003). Hoy `ExploreViewModel.favoriteIds` es local y nunca se hidrata desde `list()` (líneas 61/134-148) — de ahí la incoherencia actual.
- **Alternatives considered**: hidratación puntual por pantalla (rechazada: ventanas de inconsistencia al navegar); event-bus de toggles (rechazado: más piezas móviles que un `StateFlow` compartido).

## R-03: Caché Room de solo lectura (`CachedFavorite`) con verdad = servidor

- **Decision**: nueva entidad `CachedFavorite(id, name, favicon, country, language, tagsCsv, addedAt, sortIndex, cachedAt)` + DAO (`replaceAll`, `loadOrdered`, `clear`); `TolochaDb` sube a versión 2 con migración probada; ante `GET` OK se sobrescribe; sin red y con caché se emite marcada `offline=true`; sin caché se emite `Error` con reintento.
- **Rationale**: replica el patrón existente `CachedStation`/`StationsCacheDao` (`data/local/StationsCache.kt`) y cumple FR-010 + aclaración "mostrar caché". Solo-lectura exigido por constitución II y spec.
- **Alternatives considered**: DataStore para la lista (rechazado: no es queryable/ordenable); sin caché (rechazado: contradice la aclaración aceptada por el usuario).

## R-04: Drag & drop con Compose Foundation (sin dependencias nuevas)

- **Decision**: `LazyColumn` + `rememberLazyListState` con asa de arrastre por fila (long-press o asa táctil según prueba manual), reorden local inmediato y `PUT` automático al soltar con indicador de guardado no bloqueante; ante error, reversión al último orden confirmado + `Snackbar` con reintento.
- **Rationale**: Compose BOM `2025.01.00` ya incluye Foundation/Material3 — cero dependencias nuevas (YAGNI, constitución V). Cumple la aclaración drag & drop + autoguardado.
- **Alternatives considered**: botones subir/bajar o menú contextual (rechazados: el usuario eligió explícitamente drag & drop); librería externa de reorder (rechazada: dependencia nueva sin justificar).

## R-05: Deshacer 10 s con `Snackbar` + re-guardado con posición

- **Decision**: al quitar en Favoritos, quitar optimista + `Snackbar` M3 con acción "Deshacer" visible 10 s; deshacer = `POST /favorites` y reinserción local en su índice previo; si expira, el estado queda "no favorita".
- **Rationale**: implementa FR-005 + aclaración 10 s con componente estándar M3, coherente con la dirección Pocket Casts (spec 002).
- **Alternatives considered**: diálogo de confirmación previa (rechazado: fricción extra, peor SC-002); undo infinito (rechazado: retiene estado y confunde fuente de verdad).

## R-06: Conflicto de orden "gana servidor"

- **Decision**: si `PUT order` falla por divergencia (o el `GET` posterior difiere), descartar el orden local, mostrar el del servidor + aviso breve, sin diálogo de fusión.
- **Rationale**: implementa la aclaración aceptada; el servidor exige permutación exacta, así que la fusión cliente es frágil. Simple y predecible (YAGNI).
- **Alternatives considered**: reintento ciego del orden local (rechazado: puede pisar al otro dispositivo en bucle); resolución manual (rechazada: complejidad sin caso de uso probado).

## R-07: Fecha relativa sin dependencias nuevas

- **Decision**: helper `RelativeTime` con `android.text.format.DateUtils.getRelativeTimeSpanString` (o `java.time` en unit tests con desugaring ya disponible por AGP/JVM17 — verificar en implementación; preferir `DateUtils` por ser API 26-safe sin configuración extra).
- **Rationale**: cumple "hace 2 días" (FR-001) con APIs del SDK; cero dependencias (constitución V). `addedAt` ya existe como `Long` en `FavoriteDto`.
- **Alternatives considered**: librería de formateo (rechazada: injustificable para un "hace X").

## R-08: `ReorderBody` y validación de permutación exacta en dominio

- **Decision**: `ReorderUseCase` valida que los ids enviados sean exactamente el conjunto actual (mismo tamaño, sin duplicados ni ausentes) antes del `PUT`; el repo serializa `{stationIds: [...]}` (nombre de campo a confirmar contra `openapi.json`/prueba de integración en tasks; el servidor lo exige como lista completa).
- **Rationale**: evita 422 predecibles y cumple el contrato "permutación exacta" citado en la spec y la constitución.
- **Alternatives considered**: validar solo en UI (rechazado: la regla de negocio debe vivir en `domain` según constitución I).
