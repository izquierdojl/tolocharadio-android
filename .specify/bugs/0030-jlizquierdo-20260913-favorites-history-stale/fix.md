# Bug Fix: Favoritos e Historial no se refrescan al momento tras cambios

- **Slug**: 0030-jlizquierdo-20260913-favorites-history-stale
- **Fixed**: 2026-09-13
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Las pantallas de Favoritos e Historial mantenían una copia de una sola carga y no observaban los flujos compartidos de sus repos, por lo que un alta desde Explorar o una reproducción desde cualquier pantalla no se reflejaba hasta recrear el ViewModel (reiniciar). Ahora `FavoritesRepo` publica la lista completa observada, `FavoritesViewModel`/`HistoryViewModel` la coleccionan reconciliando con el estado transitorio de la UI, y `PlayerViewModel` registra localmente (optimista) la escucha al arrancar el stream por proxy.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/FavoritesRepo.kt` | modified | Nuevo `favorites: StateFlow<List<FavoriteDto>>` alimentado por `list()`/`add()`/`remove()`/`fromCache()`/`clearLocal()` mediante `publish()`. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModel.kt` | modified | Colecciona `repo.favorites` (`onFavoritesChanged`) y aplica la lista preservando `pendingUndo`, `savingOrder` y `offline`. Se elimina el alias `retry()` (ver Deviations). |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt` | modified | El botón de reintento usa `viewModel::refresh` en lugar del alias eliminado. |
| `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/HistoryRepo.kt` | modified | Nuevo `recordLocalPlay(station, now)`: deduplica por emisora, la deja en primera posición y hace upsert en la caché Room. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryViewModel.kt` | modified | Colecciona `repo.items` (`onHistoryChanged`) preservando `pendingDeletes` y la bandera `offline`. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` | modified | Inyecta `HistoryRepo` y llama a `recordLocalPlay` al arrancar el stream **local** (rama no-Cast, tras el precheck). |
| `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/FavoritesRepoTest.kt` | tests added/updated | Flujo completo observado en list/add/remove/cache y alta al final. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModelTest.kt` | tests added/updated | Alta/baja externa sin recrear el ViewModel; stubs del flujo. |
| `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/HistoryRepoTest.kt` | tests added | Dedupe/primera posición/caché/id en blanco de `recordLocalPlay`. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/history/HistoryViewModelTest.kt` | tests added/updated | Escucha nueva en `repo.items` aparece al momento; stubs del flujo. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` | tests added | Registro en local, no registro si no es playable y no registro en Cast. |

## Diff Highlights

```kotlin
// FavoritesRepo: la lista completa pasa a ser fuente observada (FR-003)
private val _favorites = MutableStateFlow<List<FavoriteDto>>(emptyList())
val favorites: StateFlow<List<FavoriteDto>> = _favorites.asStateFlow()

private fun publish(items: List<FavoriteDto>) {
    _favorites.value = items
    _favoriteIds.value = items.map { it.station.id }.toSet()
}
```

```kotlin
// FavoritesViewModel: reconciliación reactiva sin pisar el deshacer ni el orden
viewModelScope.launch { repo.favorites.collect(::onFavoritesChanged) }
// onFavoritesChanged: si hay pendingUndo conserva Content.copy(items = items)
```

```kotlin
// HistoryRepo: escucha optimista al momento (el servidor sigue siendo la verdad)
suspend fun recordLocalPlay(station: StationDto, now: Long = System.currentTimeMillis()) {
    if (station.id.isBlank()) return
    val entry = HistoryEntryDto(station = station, playedAt = now)
    _items.value = listOf(entry) + _items.value.filterNot { it.station.id == station.id }
    db.historyCache().upsertAll(listOf(entry).toCached(now))
}
```

```kotlin
// PlayerViewModel: solo la reproducción local pasa por el proxy (Cast usa la URL pública, 0026)
history.recordLocalPlay(station)
```

## Tests Added or Updated

- `FavoritesRepoTest::list OK publica ids deduplica y cachea en orden` (actualizado) — el flujo `favorites` refleja la lista deduplicada.
- `FavoritesRepoTest::list 503 con cache devuelve cache offline` (actualizado) — el flujo se hidrata desde caché.
- `FavoritesRepoTest::add OK suma el id al flujo` (actualizado) — el flujo incluye la nueva favorita.
- `FavoritesRepoTest::add OK agrega la favorita al final de la lista observada` — orden de inserción coherente con la caché.
- `FavoritesRepoTest::remove OK resta el id del flujo` (actualizado) — el flujo excluye la quitada.
- `FavoritesViewModelTest::alta externa aparece al momento sin recrear el ViewModel` — un id añadido al repo pasa a `Content` sin reinstanciar el VM.
- `FavoritesViewModelTest::baja externa vacia la lista` — una lista vacía en el repo deja la UI en `Empty`.
- `FavoritesViewModelTest::quitar con deshacer no se pisa al confirmar el repo` — la reconciliación conserva `pendingUndo` mientras el repo confirma la baja.
- `HistoryRepoTest::recordLocalPlay deduplica y deja la escucha en primera posicion` — dedupe, `playedAt` nuevo y upsert en caché.
- `HistoryRepoTest::recordLocalPlay sin lista previa deja solo la escucha` — funciona sin `list()` previo.
- `HistoryRepoTest::recordLocalPlay con id en blanco no hace nada` — no toca estado ni caché.
- `HistoryViewModelTest::escucha nueva en el repo aparece al momento` — `repo.items` actualiza la UI sin recargar.
- `PlayerViewModelTest::play playable local registra la escucha en el historial` — se llama una vez tras el precheck OK.
- `PlayerViewModelTest::play no playable no registra historial` — sin precheck OK no hay registro.
- `PlayerViewModelTest::reproduccion en Cast no registra historial local` — Cast no pasa por el proxy (0026).

## Local Verification

- Tests dirigidos: `.\gradlew.bat testDebugUnitTest --tests "…FavoritesRepoTest" --tests "…HistoryRepoTest" --tests "…FavoritesViewModelTest" --tests "…HistoryViewModelTest" --tests "…PlayerViewModelTest"` → 52 tests, 0 fallos (dos iteraciones: un mock de flujo y el reloj virtual del Deshacer, corregidas).
- Gates completos: `.\gradlew.bat assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` → **BUILD SUCCESSFUL**, 315 tests, 0 fallos, 0 errores. Primera pasada de detekt falló por `TooManyFunctions` (11/11) en `FavoritesViewModel`; resuelto eliminando el alias `retry()`.
- Manual checks: no ejecutados (requiere dispositivo/emulador y servidor real); pendiente en `/speckit.bug.test`.
- Rama: `0030-jlizquierdo-20260913-favorites-history-stale` (creada antes del fix con nombre exacto vía preset tolocha-naming).

## Deviations from Assessment

- Se implementó la opción preferida "flow de lista completa" en `FavoritesRepo` (en lugar de observar ids + `refresh()`), como recomendaba el assessment.
- Historial: la inserción local optimista se limita a la reproducción **local** por proxy; en Cast (que usa la URL pública, bug 0026) no se registra, tal como pedía el assessment ("decidir explícitamente"). Anotado en Follow-ups.
- `recordLocalPlay` antepone la entrada en lugar de ordenar por reloj del dispositivo, para garantizar "primera posición al momento"; el siguiente `list()` reconcilia `playedAt`/orden del servidor.
- Se eliminó el alias `retry()` de `FavoritesViewModel` y `FavoritesScreen` pasa `viewModel::refresh`: el gate obligatorio de detekt marcó `TooManyFunctions` (11/11, peso 2) al añadir `onFavoritesChanged`. `FavoritesScreen.kt` estaba listado como posible ajuste en el assessment.

## Follow-ups

- Verificar en dispositivo (`/speckit.bug.test`): favorito desde Explorar → aparece en Favoritos al momento; reproducir → aparece en Historial al momento; deshacer (10 s) y reordenar siguen correctos; el menú del icono (accesos directos) se actualiza también al reproducir.
- Decidir si una reproducción en **Cast** debería registrarse en el historial: el receptor usa la URL pública y el servidor no la registra (0026); hoy no aparece localmente ni en otros dispositivos.
- Entrada "fantasma" posible si el stream falla justo después del precheck OK (se inserta localmente y el servidor no la registra); se reconcilia en el siguiente `list()`. Valorar umbral (esperar a `STATE_READY`) si genera ruido.
- Confirmar el orden al re-insertar una favorita con Deshacer: el flujo del repo la coloca al final (igual que la caché, `nextSortIndex`) mientras que el VM la reponía de forma optimista en su índice original. Si se quiere conservar la posición visual, reflejar el índice en `FavoritesRepo.add`.
