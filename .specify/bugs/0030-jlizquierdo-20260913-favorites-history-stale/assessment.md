# Bug Assessment: Favoritos e Historial no se refrescan al momento tras cambios

- **Slug**: 0030-jlizquierdo-20260913-favorites-history-stale
- **Created**: 2026-09-13
- **Source**: pasted text
- **Verdict**: likely valid, needs reproduction
- **Severity**: high

## Report (verbatim or summarized)

Reportado por el usuario (texto pegado), dos síntomas que se sospechan relacionados:

> - Cuando en explorar se busca una nueva emisora y se marca como favorita, no se recargan los favoritos para que aparezca. Tienes cerrar y volver a loguear para se abra, esto no debería ser así, tiene que aparecer inmediatamente.
> - Cuando se reproduce una emisora, no se actualiza el historial al momento, tienes que salir y entrar para que se recargue y vuelva a aparecer.

## Symptom

Dos listas de solo lectura no reflejan al momento cambios hechos desde otras pantallas:

1. **Favoritos**: una emisora marcada como favorita desde Explorar (corazón de la tarjeta o ficha) no aparece en la pestaña **Favoritos** hasta reiniciar/relanzar la app (recrear el ViewModel). Se espera que aparezca inmediatamente al abrir la pestaña.
2. **Historial**: una emisora recién reproducida (por el proxy local, que registra el historial server-side) no aparece en la pestaña **Historial** hasta salir y volver a entrar (o relanzar). Se espera que aparezca en primera posición al terminar de arrancar la reproducción.

## Reproduction

1. **Favoritos**
   1. Abrir **Explorar** y buscar una emisora que no esté en favoritos.
   2. Marcarla como favorita (corazón en la tarjeta o en la ficha de emisora).
   3. Ir a la pestaña **Favoritos** (ya visitada antes en la misma sesión).
   4. Observado: la emisora no aparece; tras cerrar y relanzar la app, sí. [NEEDS CLARIFICATION: ¿la pestaña se había abierto antes en esa sesión? Si era la primera visita, el ViewModel se crea de cero y la lista carga correctamente, lo que apunta al caso "ViewModel vivo".]
   5. Esperado: la emisora aparece inmediatamente en la lista.
2. **Historial**
   1. Reproducir una emisora desde Explorar, la ficha de emisora, Favoritos o el mini-player (reproducción local por proxy).
   2. Ir a la pestaña **Historial** (ya visitada antes en la misma sesión).
   3. Observado: la escucha no aparece; al salir y volver a entrar (o relanzar), sí. [NEEDS CLARIFICATION: ¿"salir y entrar" es cambiar de pestaña o cerrar/relanzar la app?]
   4. Esperado: la emisora aparece la primera, con hora relativa reciente, sin reiniciar.
   - [NEEDS CLARIFICATION: ¿la reproducción era local (proxy) o Cast? Con Cast (bug 0026) el receptor usa la URL pública y el servidor **no** registra historial.]

## Suspected Code Paths

**Favoritos**

- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/FavoritesRepo.kt:35-38` — `favoriteIds` es la "fuente única observada por todos los ViewModels" (FR-003), pero solo expone **ids**, no la lista completa.
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/FavoritesRepo.kt:65-81` — `add()` actualiza `_favoriteIds` y la caché Room; nadie que pinte la lista completa observa ese cambio.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModel.kt:85-92` y `179-193` — Explorar sí colecciona `favoriteIds` y hace toggle optimista; al añadir, el repo emite el id nuevo.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/StationDetail.kt:66-73` — la ficha también colecciona `favoriteIds` (coherencia FR-003).
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModel.kt:81-83,90-93,96-128` — la pantalla de Favoritos **no colecciona** `favoriteIds`: solo carga una vez en `init` y mediante `onForeground()`/`retry()`. Su `_ui` es una copia imperativa sin invalidación.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt:135-138` — el único disparador de recarga es `LifecycleEventEffect(ON_RESUME)` → `viewModel.onForeground()`. Ese evento depende del ciclo del `NavBackStackEntry` (no del dato) y puede no llegar al reentrar a la pestaña; además `onForeground()` descarta la recarga si `loading` está en curso (`FavoritesViewModel.kt:90-93`).
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt:238-244,303-310` — la bottom bar navega con `popUpTo(...){saveState=true}` + `restoreState=true`; el `hiltViewModel()` de la pantalla queda a ámbito de `NavBackStackEntry`, por lo que el ViewModel **sobrevive** al cambio de pestaña y no vuelve a ejecutar `init`.

**Historial**

- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/HistoryRepo.kt:32-35` — `items` se documenta como "fuente observada por el ViewModel", pero solo lo observa `ShortcutSyncCoordinator`.
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/HistoryRepo.kt:43-59` — `list()` (red→caché) es el único camino que actualiza `_items`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryViewModel.kt:60-62,72-75` — carga única en `init` y `onForeground()`; **no colecciona** `repo.items`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryViewModel.kt:161-178` — `onPlayTriggered()` refresca con 500 ms de retardo, pero **solo se invoca si la reproducción se lanzó desde la propia pantalla de Historial** (`HistoryScreen.kt:148-151`).
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryScreen.kt:111-114` — único disparador de recarga al reentrar: `LifecycleEventEffect(ON_RESUME)` (misma fragilidad que Favoritos).
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt:189-208,210-225` — `play()`/`startSource()` arrancan la reproducción local por proxy (que sí registra historial en el servidor) pero **no notifican a nadie** en el cliente.
- `specs/0021-jlizquierdo-20260912-proxy-only-playback/research.md:8` y `contracts/proxy-playback.md:60` — el servicio registra historial al consumir `GET /playback/:id`; no existe API de cliente para insertarlo (`data/remote/api/HistoryApi.kt` solo tiene GET/DELETE).
- `app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSyncCoordinator.kt:61-71` — precedente de diseño: el menú del icono **sí** observa `historyRepo.items` y refresca en foreground (`TolochaApp.kt:24,32-37`); la pantalla de Historial debería hacer lo mismo.

## Root Cause Hypothesis

**Confidence: high en la causa estructural (verificada en código); medium en el disparador exacto (requiere reproducción en dispositivo).**

Ambas pantallas cargan su estado una sola vez (`init` + `onForeground`) y **no observan los flujos compartidos que sus repos ya mantienen**. En Favoritos, `favoriteIds` propaga el alta desde Explorar/Ficha, pero `FavoritesViewModel` no lo colecciona y el repo no expone la lista completa; por eso el alta no llega a la UI. En Historial, el servidor es la verdad y registra la escucha al consumir el stream, pero el cliente no emite ninguna señal al arrancar la reproducción; `onPlayTriggered()` solo cubre las reproducciones lanzadas desde la propia pantalla de Historial, y `HistoryRepo.items` (que el `ShortcutSyncCoordinator` sí observa) no lo observa nadie que pinte la lista.

La única vía de recarga al reentrar a una pestaña es un evento de ciclo de vida (`ON_RESUME`) que depende del timing del `NavBackStackEntry` con `saveState/restoreState` y del guard `loading`, no del cambio de dato; cuando no se dispara (o se descarta), el estado obsoleto persiste hasta que se recrea el ViewModel (cerrar/relanzar) o se vuelve a primer plano la Activity con la pantalla activa. El síntoma del usuario ("cerrar y volver a loguear", "salir y entrar") encaja exactamente con ese patrón.

Nota de alcance: no es una regresión del fix 0029 (sesión/caché tras reposo), que añadió precisamente estos `onForeground()`; es una carencia de reactividad de datos previa que ese fix no cubría.

## Proposed Remediation

**Preferred**:

1. **Favoritos reactivo (una sola fuente de lista)**. Exponer en `FavoritesRepo` la lista completa observada (`favorites: StateFlow<List<FavoriteDto>>`, actualizada en `list()`, `add()`, `remove()`, `fromCache()` y `clearLocal()`; `reorder()` puede reflejar el orden confirmado) y que `FavoritesViewModel` la coleccione como fuente de verdad, manteniendo `pendingUndo`/`savingOrder` como estado local de UI (filtrando la entrada pendiente de deshacer). Así un alta desde Explorar aparece al instante, sin red adicional y con caché como respaldo.
   - Versión mínima alternativa dentro de la misma idea: `FavoritesViewModel` colecciona `repo.favoriteIds` y lanza un `refresh()` silencioso cuando los ids difieren de `confirmed` (evitando pisar `pendingUndo`). Más pequeña, pero añade red y carrera con el deshacer; la primera opción es la recomendada.
2. **Historial reactivo (señal de reproducción + observación)**.
   - `HistoryRepo`: nuevo método tipo `recordLocalPlay(station, now)` que deduplica por `station.id`, antepone la entrada con `playedAt = now`, actualiza `_items` y la caché Room (upsert). El servidor sigue siendo la verdad: el siguiente `list()` reconcilia.
   - `PlayerViewModel`: invocar `recordLocalPlay` cuando arranca el stream **local** por proxy (rama no-Cast de `startSource`), que es el embudo único de todas las reproducciones. Decidir explícitamente el caso Cast (no pasa por el proxy y no se registra server-side).
   - `HistoryViewModel`: coleccionar `repo.items` para pintar `Content` (conservando `refresh()`/`onForeground()` para errores, offline y reintento). Colateral positivo: el menú del icono (`ShortcutSyncCoordinator`) también se actualizará al instante (spec 0018, US3).

**Alternatives**:

- **Event bus de invalidación**: `SharedFlow<Unit>`/`changes` en cada repo emitido en mutaciones; los ViewModels coleccionan y llaman `refresh()`. Más sencillo que modelar la lista completa, pero mantiene latencia de red y no da actualización inmediata.
- **Historial sin inserción local**: coordinador app-scoped (estilo `ShortcutSyncCoordinator`) que observa una `SharedFlow<StationDto>` de `PlayerViewModel` y llama `historyRepo.list()` ~500 ms después de arrancar; `HistoryViewModel` observa `items`. Respeta al 100 % la verdad del servidor, a costa de un retardo perceptible y de una posible carrera con el registro server-side.
- **Parche de navegación**: `LaunchedEffect(Unit)` en cada pantalla para recargar en cada entrada. Mitiga los síntomas sin tocar la arquitectura, pero no es reactivo (una reproducción en curso no se ve hasta salir y entrar) y multiplica peticiones.

**Files likely to change**:

- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/FavoritesRepo.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModel.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/HistoryRepo.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryViewModel.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt`
- Posibles ajustes: `feature/favorites/FavoritesScreen.kt`, `feature/history/HistoryScreen.kt`
- Tests: `FavoritesRepoTest`, `FavoritesViewModelTest`, `HistoryRepoTest`, `HistoryViewModelTest`, `PlayerViewModelTest`

**Tests to add or update**:

- `FavoritesRepoTest`: el flujo de lista se actualiza al añadir/quitar/listar y al hidratar desde caché; el orden se refleja tras `reorder`.
- `FavoritesViewModelTest`: un cambio externo de `favoriteIds` (alta desde Explorar) aparece en `Content` sin recrear el ViewModel; quitar con deshacer no se pisa por la reconciliación; el error de credenciales sin caché sigue ofreciendo "Editar servidor" (FR-006).
- `HistoryRepoTest`: `recordLocalPlay` deduplica por emisora, la mueve a la primera posición con `playedAt` nuevo, actualiza caché y no duplica una entrada existente.
- `HistoryViewModelTest`: coleccionar `repo.items` refleja la nueva escucha al momento; `Empty` con lista vacía; no rompe los estados `Error`/`offline` ni el refresco del fix 0029.
- `PlayerViewModelTest`: reproducción local con precheck OK → notifica historial una sola vez; precheck fallido → no notifica; caso Cast → comportamiento decidido y cubierto.

## Risks & Considerations

- **Deshacer (10 s) y reordenación**: la lista observada no debe reinsertar/clobberar la entrada pendiente de deshacer ni el orden optimista mientras `savingOrder` está activo.
- **Bucles o tormentas de refresco**: si se combina observación de ids con `refresh()`, comparar contra `confirmed` y evitar reaccionar a emisiones originadas por el propio ViewModel.
- **Entradas "fantasma" de historial**: una inserción local optimista podría mostrarse en la app sin estar en el servidor (p. ej. reproducción en Cast, que no pasa por el proxy; bug 0026). Decidir alcance/UX y reconciliar con `list()`.
- **Bandera `offline`**: ni `HistoryRepo.items` ni un eventual `favorites` flow llevan `offline`; conservar `refresh()` o el estado correspondiente para el banner "Mostrando caché sin conexión".
- **Compatibilidad con 0029**: no retirar `refresh()`/`onForeground()` (sesión y caché tras reposo); la observación reactiva se suma.
- **Restricciones de la constitución**: sin nuevas dependencias, HTTPS-only, sin PII en logs (solo `code`/`status`/stationId anonimizado) y tests de `data`/`ViewModel` en rojo-verde.
- **Rendimiento**: la inserción local de historial es O(n) sobre una lista pequeña (decenas/cientos); sin impacto.

## Open Questions

- [NEEDS CLARIFICATION: ¿La pestaña de Favoritos/Historial ya se había abierto antes en esa sesión? Determina si el ViewModel estaba vivo (caso del bug) o si era la primera visita.]
- [NEEDS CLARIFICATION: ¿La reproducción era local (proxy) o Cast? Con Cast (0026) el servidor no registra historial.]
- [NEEDS CLARIFICATION: ¿El fallo se reproduce con el build actual (post-0029, `1.7.6-fix0029`) o con un APK anterior? Este análisis asume el código de `main` actual.]
- [NEEDS CLARIFICATION: ¿"Salir y entrar" significa cambiar de pestaña o cerrar/relanzar la app? Cambia qué mecanismo de recarga está fallando.]
- [NEEDS CLARIFICATION: ¿Se acepta que la reproducción aparezca en Historial al instante con una inserción local optimista, o se prefiere esperar ~1 s a que el servidor la registre (solo refresco)?]
