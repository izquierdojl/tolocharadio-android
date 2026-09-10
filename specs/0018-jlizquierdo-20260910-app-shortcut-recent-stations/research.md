# Research: Emisoras recientes en los accesos directos del icono

**Feature**: `0018-jlizquierdo-20260910-app-shortcut-recent-stations` | **Date**: 2026-09-10

Este documento resuelve las incógnitas técnicas del plan. No quedan `NEEDS CLARIFICATION`.

## R0. Bloqueo de tooling: `setup-plan.ps1` / `resolve-template.ps1`

- **Decision**: No parchear los scripts de Spec Kit. `plan.md` y los artefactos se crean manualmente desde las plantillas de `.specify/templates/`; se documenta el bug para arreglarlo por separado.
- **Rationale**: `Get-Python3Command` (`.specify/scripts/powershell/common.ps1:322`) devuelve `python3` primero; en este equipo `python3` es el alias de Microsoft Store que falla con exit 9009. El resolver interpreta el fallo como manifiesto inválido (`common.ps1:668`) aunque `preset.yml` es válido (verificado con `python` real + PyYAML 6.0.3). Parchear scripts generados se pierde en cada `specify upgrade`.
- **Alternatives**: editar `common.ps1` (se sobreescribe en upgrades); deshabilitar el alias de Windows (cambio de máquina, fuera del repo); cambiar el preset (no es la causa).

## R1. API de accesos directos dinámicos

- **Decision**: `ShortcutManagerCompat` (`androidx.core:core-ktx`, ya incluida) con `setDynamicShortcuts()` para publicar y `removeAllDynamicShortcuts()` para limpiar. Solo shortcuts dinámicos: sin `res/xml/shortcuts.xml` ni meta-data `android.app.shortcuts`.
- **Rationale**: `minSdk 26` ya soporta shortcuts nativos; `ShortcutManagerCompat` es el wrapper estable y no añade dependencias. `setDynamicShortcuts` reemplaza el conjunto completo de forma atómica, evitando elementos huérfanos al reordenar o reducir el historial. No se necesitan shortcuts estáticos (todo el contenido es dinámico).
- **Alternatives**: `ShortcutManager` directo (API 25+; innecesario); XML estático (contenido no estático); pinned shortcuts y `pushDynamicShortcut` (fuera de alcance).
- **Limits**: el máximo de slots por actividad es `getMaxShortcutCountPerActivity()` (típicamente 5; muchos lanzadores muestran 4). La app publica `N = min(maxSlots, 5)` y asigna `rank` 0..N-1 (menor = más prioritario). No hay rate-limiting relevante porque solo se publica con la app en primer plano (el rate limit aplica a updates en background).
- **IDs**: id determinista `hist-<stationId>` por emisora, para que republicar actualice el elemento correcto en lugar de duplicarlo.

## R2. Iconos (favicon) best-effort

- **Decision**: construir cada shortcut con `IconCompat`: se intenta descargar el `favicon` con Coil en `Dispatchers.IO` (timeout ~1,5 s por icono, en paralelo para los N slots); si falla o expira, se usa el icono de la app como fallback. Una sola pasada de publicación.
- **Rationale**: FR-008 pide imagen "cuando el lanzador lo permita" y FR-015/SC-004 no admiten bloqueos largos; Coil ya es dependencia. La descarga nunca bloquea el flujo de reproducción ni el hilo principal.
- **Alternatives**: icono de app siempre (degrada FR-008); carga secuencial bloqueante (riesgo SC-004); segunda publicación al terminar de descargar (complejidad y doble escritura innecesarias para v1).

## R3. Intent del shortcut y arranque de la app

- **Decision**: action propia `com.izquierdojl.tolocharadio.OPEN_STATION`; extras `station_id` (obligatorio) y `station_name` (opcional, solo texto de UI, información pública). Intent explícito hacia `MainActivity` (`launchMode="singleTask"` ya existente). `onCreate` (arranque en frío) y `onNewIntent` (ya en memoria) parsean con un parser puro y publican en `PendingShortcutHolder` (`StateFlow`).
- **Rationale**: los shortcuts dinámicos usan intents explícitos; no se necesita `intent-filter` ni deep links web. Pasar solo id/nombre evita filtrar datos personales en el lanzador. El holder desacopla la Activity (que no conoce el grafo) del `PlayerViewModel` activity-scoped, que solo existe dentro de la composición.
- **Alternatives**: URI de deep link `tolocharadio://station/<id>` (sin necesidad de web linking); serializar el `StationDto` completo en extras (frágil, sobredimensionado).

## R4. Resolución del stationId al pulsar

- **Decision**: caso de uso puro `ResolveShortcutLaunchUseCase(stationId, authState, historialConocido) → Play(station) | GoLogin(reason) | Unavailable`:
  1. `AuthState.Loading` → esperar (no consumir el pendiente).
  2. `AuthState.Unauthenticated` → `GoLogin(reason)` (incluye "expired").
  3. `Authenticated`: buscar la emisora en el historial en memoria (`HistoryRepo.items`); si no está, leer la caché Room (`loadOrdered()`); si tampoco, `StationsRepo.detail(id)` (caso huérfano: se limpió el historial pero el shortcut aún no se republicó); si el remoto falla/404 → `Unavailable`.
- **Rationale**: el precheck de disponibilidad y los errores de reproducción ya viven en `PlayerViewModel.play()`, que solo necesita un `StationDto`; este caso de uso decide únicamente el arranque y es testeable sin Android. Cubre la carrera "historial limpiado en otro dispositivo → shortcut obsoleto" sin sacrificar FR-015 (offline usa caché).
- **Alternatives**: consultar siempre al servidor (rompe offline); confiar solo en memoria/caché (fallaría con shortcuts obsoletos legítimos hasta la siguiente sincronización).

## R5. Sincronización: triggers y fuente offline

- **Decision**: `ShortcutSyncCoordinator` (`@Singleton`, `@ApplicationScope`, `Mutex` para serializar):
  - Observa `HistoryRepo.items` → `BuildShortcutStationsUseCase` → `publisher.publish()`.
  - Observa `SessionManager.authState`: `Authenticated` → `historyRepo.list()` (refresco; el repo ya cae a caché con `fromCache()` ante `Unavailable`) y publica; `Unauthenticated` → `publisher.clear()`.
  - `onForeground()` invocado desde un `DefaultLifecycleObserver` de `ProcessLifecycleOwner` registrado en `TolochaApp`: si hay sesión, `historyRepo.list()` (esto cubre US3.4: cambios hechos en otro dispositivo se reflejan al volver a primer plano). Con `Mutex` no hay carreras; sin throttle adicional en v1 (YAGNI; reevaluar si aparecen quejas de red/batería).
  - `clearNow()` invocado explícitamente en `LogoutUseCase` (tras `logout()`) y en `ServerRepository.switchTo()` (antes/durante el cambio de instancia), para cumplir FR-010 aun si el flujo de `authState` tarda o el proceso está por reiniciarse con ProcessPhoenix.
- **Rationale**: reutiliza flujos ya existentes (`items` se actualiza en `list()/remove()/clear()`), cumple FR-009/FR-010/FR-015 y SC-003/SC-004 sin jobs, tablas ni WorkManager. SC-004 (≤5 s) se cumple porque la reproducción llama a `HistoryViewModel.onPlayTriggered()` → `list()` → emisión de `items` → publicación.
- **Alternatives**: WorkManager en background (la plataforma limita las actualizaciones fuera de foreground y añade complejidad); polling periódico (innecesario).

## R6. Reproductor a pantalla completa al pulsar

- **Decision**: elevar el estado a `PlayerViewModel`: `fullPlayerVisible: StateFlow<Boolean>`, `openFullPlayer()`, `closeFullPlayer()`. `MiniPlayer` deja de usar `var showFullSheet by remember` y renderiza `FullPlayerSheet` según el VM. El gesto actual sobre la identidad del panel (abre `StationInfoSheet`) se mantiene sin cambios.
- **Rationale**: FR-005 exige mostrar el reproductor a pantalla completa; hoy `FullPlayerSheet` es código inalcanzable (`showFullSheet` nunca pasa a `true`). El `PlayerViewModel` ya es activity-scoped y compartido, así que es el punto natural.
- **Alternatives**: ruta de navegación `player` (la app no tiene ruta de player y el sheet ya existe: YAGNI); estado local en `TolochaNavGraph` (el disparador viene de fuera de la composición y debe sobrevivir a recreaciones).

## R7. Sesión caducada al pulsar

- **Decision**: camino `GoLogin` → `navController.navigate(Routes.LOGIN)` + snackbar "Tu sesión ha caducado. Inicia sesión de nuevo." y limpiar el pendiente. Añadir consumidor para `reason == "expired"` (hoy `SessionManager.logout("expired")` no tiene consumidor). La renovación transparente la sigue haciendo `TokenAuthenticator` (refresh + reintento único) y `SessionRestorer` al arrancar.
- **Rationale**: FR-007; reutiliza mecanismos existentes sin duplicar lógica de refresh. El pendiente no se consume mientras `AuthState.Loading`, de modo que un arranque en frío con sesión restaurable reproduce sin pasar por login.
- **Alternatives**: forzar re-login antes del precheck (bloquea renovaciones válidas); silenciar el fallo (incumple FR-007).

## R8. Estrategia de tests (constitución III)

- **Decision**:
  - Puros: `BuildShortcutStationsUseCaseTest` (dedupe por id conservando `playedAt` máximo, orden desc, cap N, lista vacía, incluye personalizadas, id estable), `ResolveShortcutLaunchUseCaseTest` (play desde historial, desde caché, fetch remoto, 404→Unavailable, Loading→wait, Unauthenticated→GoLogin), `ShortcutIntentsTest` (parseo de action/extras, id ausente/blank), `ShortcutSpecFactoryTest` (id `hist-<id>`, rank, etiquetas recortadas, label máximo).
  - `ShortcutSyncCoordinatorTest`: `ShortcutPublisher` fake + `HistoryRepo`/`SessionManager` MockK + `StandardTestDispatcher`: publica al cambiar items, limpia en logout, no publica sin sesión, usa la lista cacheada offline, llama `clearNow()` en switch.
  - `PlayerViewModelTest`: `openFullPlayer/closeFullPlayer` actualizan el estado.
  - `AndroidShortcutPublisher` queda como wrapper delgado (llamadas a `ShortcutManagerCompat`); la lógica testeable se extrae a `ShortcutSpecFactory`. Sin Robolectric (no está en el stack); verificación instrumentada/manual en quickstart.
- **Rationale**: cubre las reglas críticas con tests rápidos y deterministas en `testDebugUnitTest` (CI), sin introducir dependencias de test nuevas.

## Resolved unknowns

| Incógnita | Resolución |
|-----------|------------|
| Máximo/número de shortcuts | `min(getMaxShortcutCountPerActivity(), 5)` con ranks 0..N-1 (R1) |
| Iconos | Favicon best-effort con timeout + fallback a icono de app (R2) |
| Cómo arranca la app desde el shortcut | Intent explícito con action/extras + `PendingShortcutHolder` (R3) |
| Cómo resolver una emisora ausente de memoria | Caché Room → `StationsRepo.detail(id)` → error accionable (R4) |
| Cuándo sincronizar y fuente offline | `items` + foreground + auth; caché Room vía `HistoryRepo.list()`/`fromCache()` (R5) |
| Cómo abrir el reproductor completo | `fullPlayerVisible` en `PlayerViewModel` (R6) |
| Sesión caducada | `GoLogin` + snackbar, refresh transparente existente (R7) |
| Cómo testear sin Robolectric | Lógica pura extraída + fakes/MockK (R8) |
