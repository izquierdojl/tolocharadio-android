# Research: Refinamientos de experiencia podcast

**Feature**: `0048-izquierdojl-20261119-podcast-ux-refinements` | **Date**: 2026-11-19

Este documento resuelve las incógnitas técnicas del plan. Las decisiones de producto se cerraron en la spec (sesión 2026-11-19) y las de implementación se justifican aquí.

---

## R1 — Datos y actualización de los tiempos

**Decision**: usar el estado ya existente del reproductor de episodios (`EpisodePlayerUi.positionMs` y `durationMs`) para el bloque superior de la cola y para la tarjeta inferior. No se crea un ticker nuevo.

- `EpisodePlaybackController` ya sondea la posición cada 1 s (`POSITION_POLL_MS = 1_000L`) y publica `State.Active` con `positionMs`/`durationMs`.
- `EpisodePlayerViewModel` copia esos valores a `EpisodePlayerUi`; la cola y la tarjeta son consumidores de ese `StateFlow`.
- El bloque superior se muestra si `episode != null`, esté reproduciendo o pausado; si no hay episodio, no se muestra.
- La tarjeta inferior ya existe y se oculta cuando no hay episodio. Para que el comportamiento sea real, `stop()` limpiará `EpisodePlayerUi` después de reportar el progreso final; el final sin siguiente hará lo mismo.
- No se consulta la cola ni el servidor para pintar el tiempo: el dato viene del reproductor local.

**Rationale**: el sondeo de 1 s ya existe y cumple el criterio de actualización de la spec (al menos una vez por segundo). Crear otro ticker duplicaría trabajo, podría desincronizar las cifras y añadiría batería. La cola solo necesita renderizar un estado que ya se está emitiendo.

**Alternatives considered**:
- Ticker propio en `QueueScreen` o `EpisodeMiniPlayer`: descartado por duplicar el sondeo y arriesgar desfases.
- Calcular el tiempo desde `PodcastEpisode.progress`: descartado porque el progreso del servidor se actualiza cada 30 s y no refleja la posición real de reproducción.
- Consultar `/progress` al abrir la cola: descartado por latencia y porque no aporta la posición viva.

---

## R2 — Formato de tiempo y minutos restantes

**Decision**: extraer las reglas a `domain/podcasts/EpisodeTime.kt` y el formato de reloj a `feature/podcasts/player/EpisodeTimeFormat.kt`.

- `remainingMs(positionMs, durationMs): Long?`:
  - `durationMs <= 0` → `null` (desconocido).
  - `positionMs` se acota a `0..durationMs`.
  - resultado = `durationMs - positionMs` (nunca negativo).
- `remainingWholeMinutes(positionSec, durationSec): Long?`:
  - `durationSec <= 0`, `positionSec <= 0` o `positionSec >= durationSec` → `null`.
  - resultado = `ceil((durationSec - positionSec) / 60.0)`; si es `<= 0`, `null`.
- Etiqueta de fila:
  - `null` → sin línea de restante.
  - `0` minutos restantes pero queda tiempo → `"Queda menos de 1 min"`.
  - `>= 1` → `"Quedan N min"`.
- `formatPlaybackTime(ms)`:
  - `m:ss` por debajo de una hora; `h:mm:ss` a partir de una hora.
  - Se usa en el bloque superior, la tarjeta inferior y el reproductor completo.
- `formatEpisodeDuration(totalSec)` existente se mantiene para la duración de la fila de cola y el reproductor completo; la duración efectiva de la fila es `episode.durationSec ?: progress.durationSec`, y se omite si no hay un valor positivo. `"--:--"` no se muestra en la línea secundaria.

**Rationale**: la spec fija minutos restantes en la cola y reloj compacto en la tarjeta. Una función pura evita repetir el redondeo en tres sitios y permite tests JVM sin Android. El techo evita anunciar menos tiempo del que queda.

**Alternatives considered**:
- Reutilizar el `formatMillis` privado de `EpisodeFullPlayerSheet`: descartado porque no es accesible y mezclaría formato con una pantalla concreta.
- Mostrar segundos exactos en la cola: descartado porque la persona pidió minutos y la fila es compacta.
- Redondear al minuto más cercano o hacia abajo: descartado porque puede mostrar "0 min" con tiempo pendiente.

---

## R3 — Favorito vivo en el detalle del episodio

**Decision**: el favorito del reproductor completo usa el conjunto vivo de `EpisodeActionsViewModel.favoriteIds` y un método explícito `toggleFavorite(episodeId, isFavorite)`; se añade un indicador de carga para no confundir "sin favoritos" con "favoritos aún no cargados".

- `EpisodeFavoritesRepo` ya mantiene `favoriteIds` actualizado al listar, añadir y quitar. Se añade `loaded: StateFlow<Boolean>` que pasa a `true` tras un `list()` correcto y tras una mutación satisfactoria.
- Se extiende `EpisodeFavoritesRepoTest` con `loaded=false` antes de listar, `true` tras un `list()` correcto y sin falso positivo tras un fallo.
- `EpisodeActionsViewModel` expone `favoritesLoaded` y:
  - `toggleFavorite(episode)` se mantiene para las llamadas existentes.
  - nuevo `toggleFavorite(episodeId: Long, isFavorite: Boolean)` que no depende del flag embebido en el episodio.
- `EpisodeFullPlayerSheet`:
  - recibe `EpisodeActionsViewModel` a ámbito de actividad (mismo patrón que la ficha);
  - calcula `isFavorite = if (favoritesLoaded) episode.id in favoriteIds else episode.isFavorite`;
  - muestra `FavoriteButton` y llama al nuevo método con el estado mostrado.
- `EpisodeSheet` (ficha) usa el mismo criterio vivo para que el estado no quede obsoleto tras cambiar el favorito en el detalle.
- Si el toggle falla, el estado mostrado no cambia y se reutiliza el mensaje accionable existente del ViewModel (`_message`).

**Rationale**: el objeto `PodcastEpisode` que llega al detalle puede tener un `isFavorite` antiguo; el conjunto vivo es la fuente de verdad de la sesión. Un booleano explícito evita que el toggle haga lo contrario de lo que muestra la pantalla cuando el estado ha cambiado en otra vista.

**Alternatives considered**:
- Usar solo `episode.isFavorite`: descartado por quedar obsoleto tras cambiar el favorito en otra pantalla.
- Usar solo `favoriteIds` sin indicador de carga: descartado porque antes del primer `list()` el conjunto está vacío y un episodio favorito se mostraría como no favorito.
- Crear un ViewModel nuevo para el detalle: descartado por duplicar el estado y romper la coherencia con la ficha y las listas.

---

## R4 — Pantalla de arranque de podcast

**Decision**: nueva preferencia local `PodcastStartScreen`, independiente de la de radio, resuelta por una función pura.

- `core/ui/navigation/PodcastStartScreen.kt`:
  - `HOME`, `DISCOVER`, `SUBSCRIPTIONS`, `QUEUE`, `FAVORITES`.
  - `label()` en español y `route()` hacia `Routes.PODCAST_*`.
- `InstancePrefs`:
  - `podcastStartScreen: Flow<PodcastStartScreen>` con clave `podcast_start_screen`.
  - Valor por defecto `HOME`; nombre corrupto o desconocido → `HOME`.
  - `setPodcastStartScreen`.
- `SettingsViewModel`:
  - `SettingsUi.podcastStartScreen`, colector de la preferencia y `onPodcastStartScreenChange`.
- `SettingsScreen`:
  - renombra la sección existente a "Pantalla de arranque en radio";
  - añade "Pantalla de arranque en podcast" con un desplegable (patrón de "Retención de descargas") para que quepan las cinco etiquetas;
  - texto de ayuda: se aplica cuando la sección inicial es Podcasts y el servidor ofrece podcasts.
- `MainActivity` colecta la nueva preferencia y la pasa a `TolochaNavGraph`.
- `StartDestination.kt`:
  - `resolveStartDestination(startScreen, defaultSection, podcastStartScreen, podcastsEnabled)`.
  - Si `defaultSection == PODCAST && podcastsEnabled` → ruta de `podcastStartScreen`.
  - En caso contrario → ruta de `startScreen` (radio), sin errores.
- `PodcastRouteEffects` incluye `podcastStartScreen` en las claves del `LaunchedEffect` de arranque.

**Rationale**: separar el enum evita que las opciones de podcast aparezcan en el selector de radio y mantiene la compatibilidad del valor persistido de radio. Una función pura permite probar todas las combinaciones sin navegación real.

**Alternatives considered**:
- Añadir los destinos de podcast al enum `StartScreen` existente: descartado porque el selector de radio mostraría opciones que no le corresponden y mezclaría dos preferencias.
- Mantener siempre Inicio en podcast: descartado porque la persona pide explícitamente la preferencia.
- Guardar la preferencia en Room: descartado por YAGNI; DataStore ya aloja las preferencias locales.

---

## R5 — Gráficas según el modo activo

**Decision**: la entrada "Gráficas" de Configuración elige la ruta según `podcastMode`; el guard de modo intercambia las rutas de estadísticas si el modo cambia con la pantalla abierta.

- `podcastMode = modeUi.podcastsEnabled && modeUi.mode == ContentMode.PODCAST` ya existe en `TolochaNavGraph`.
- `Routes.statsRoute(podcastMode)` encapsula la elección (`PODCAST_STATS` o `STATS`) y se prueba en JVM.
- La selección de destino al cambiar de modo se encapsula en `routeAfterModeChange(currentRoute, podcastMode, podcastsEnabled)` y se prueba en JVM.
- `SettingsScreen(onStats = { navController.navigate(Routes.statsRoute(podcastMode)) })`.
- `PodcastRouteEffects`:
  - `route == Routes.STATS && podcastMode` → `Routes.PODCAST_STATS`.
  - `route == Routes.PODCAST_STATS && (!podcastsEnabled || !podcastMode)` → `Routes.STATS`.
  - Las reglas existentes para el resto de rutas se mantienen.
- No se añade un selector dentro de Gráficas ni pestañas; la elección sigue al selector global, tal como se confirmó en la clarificación 2026-11-19.
- `PodcastStatsScreen` recibe `onEditServer` para el error de credenciales, igual que `StatsScreen`.
- Los encabezados se distinguen: "Tus estadísticas de radio" y "Tus estadísticas de podcast".

**Rationale**: el guard existente ya protege el resto de rutas de contenido; ampliarlo a las dos rutas de estadísticas evita que un cambio de modo deje visible el conjunto anterior. Intercambiar `STATS` ↔ `PODCAST_STATS` es más útil que expulsar a Inicio y no requiere una pantalla nueva.

**Alternatives considered**:
- Un único route con selector interno: descartado en la clarificación; duplicaría el selector global.
- Dos pestañas dentro de Gráficas: descartado en la clarificación; amplía el alcance y rompe la dependencia del modo.
- No tocar el guard y dejar que el cambio de modo muestre datos obsoletos: descartado por FR-052.

---

## R6 — Paridad visual de las gráficas de podcast

**Decision**: extraer primitivas compartidas de los bloques de radio y reutilizarlas en una vista de podcast con los datos de podcast.

- `feature/stats/blocks/ChartPrimitives.kt` (nuevo):
  - `RankingBars(title, items: List<RankingItem>)` con barra proporcional, puesto y duración; `RankingItem(label, totalMs)`.
  - `ShareBars(title, entries: List<ShareItem>)` con barra de color; `ShareItem(label, totalMs)`.
  - `RecentActivityList(title, items: List<RecentItem>)`; `RecentItem(label, startedAt, durationMs)`.
  - `SummaryCard(label, value, hint)` público (o `StatsSummaryCard`).
- `TopStationsChart` pasa a mapear `StatsTopListDto` a `RankingBars`; `GenreCountryCharts` mapea a `ShareBars`; `RecentList` mapea a `RecentActivityList`; `StatsSummaryCards` usa `SummaryCard`. La salida visual de radio no cambia.
- `PodcastStatsMappers.kt` (nuevo):
  - `PodcastTimelineDto` → `StatsTimelineDto` (misma forma `bucket`/`totalMs`) para reutilizar `StatsTimelineChart`.
  - `PodcastHabits` → `List<StatsHabitEntryDto>` para reutilizar `buildHabitsMatrix` y `HabitsHeatmap`.
  - `PodcastTopPodcasts` → `List<RankingItem>`; `PodcastTopEpisodes` → `List<RankingItem>`.
  - `PodcastCategories` → `List<ShareItem>`.
  - `PodcastRecents` → `List<RecentItem>` (episodio nulo → "Episodio").
  - Resumen → `List<SummaryItem>` o composable propio con `SummaryCard`.
- `PodcastStatsScreen`:
  - encabezado "Tus estadísticas de podcast" y subtítulo de privacidad;
  - `LazyColumn` con resumen, evolución, programas, episodios, hábitos, categorías y actividad reciente;
  - estados `Loading`, `Empty`, `Error(message, isAuthError)` y `Content`;
  - error con "Reintentar" y, si es de credenciales, "Editar servidor".
- `PodcastStatsViewModel`:
  - carga los 7 bloques en paralelo;
  - si falla el resumen **o cualquier bloque**, publica `Error` (sin sustituir por datos de radio);
  - construye `habitsMatrix` y ordena recientes por `startedAt` descendente;
  - estado `Empty` si no hay tiempo, episodios empezados ni recientes;
  - `onForeground()`/`retry()` como la vista de radio;
  - el error se registra con el patrón existente (`StatsErrorLogger`), sin PII.
- Se reutiliza `StatsFormat` para duraciones y etiquetas de fechas; no se duplica el formateo.

**Rationale**: la paridad visual exige que podcast use los mismos componentes, no una lista de texto. Extraer primitivas con dos consumidores reales cumple YAGNI y evita que radio y podcast se separen con el tiempo. Los DTO de podcast ya contienen todo lo necesario (timeline, hábitos, categorías, rankings y recientes).

**Alternatives considered**:
- Duplicar los bloques de gráficas para podcast: descartado por divergencia visual y mantenimiento duplicado.
- Adaptar los DTO de podcast a los DTO de radio con emisoras ficticias: descartado porque `StatsTopEntryDto` exige una `StationDto` y semánticamente sería incorrecto.
- Mantener las listas de texto actuales: descartado en la clarificación 2026-11-19; la sección se llama Gráficas.
- Añadir una librería de gráficos: descartado por YAGNI; los bloques actuales ya dibujan con `Canvas`/composables.

---

## R7 — Estrategia de pruebas

**Decision**: cubrir las reglas puras en JVM y los flujos críticos en Compose, reutilizando los patrones existentes.

- Unitarias JVM:
  - `EpisodeTimeTest`: restante válido, duración desconocida, posición 0, completado, posición ≥ duración, redondeo hacia arriba, menos de un minuto.
  - `EpisodeTimeFormatTest`: `m:ss`, `h:mm:ss` y restante.
  - `StartDestinationTest`: radio/podcast, podcasts no disponibles, cada destino de podcast, preferencia por defecto, ruta de Gráficas y cambio de modo.
  - `PodcastStatsViewModelTest`: éxito con matriz, error de resumen, error de cualquier bloque, vacío, credenciales, reintento.
  - `PodcastStatsMappersTest`: timeline, hábitos, rankings, categorías y recientes.
  - `SettingsViewModelTest`: valor por defecto, colector y persistencia.
  - `InstancePrefsTest`: `podcastStartScreen` por defecto, valor corrupto y persistencia.
  - `EpisodeFavoritesRepoTest`: `loaded` antes/después de listar y tras fallo.
  - `EpisodeActionsViewModelTest`: toggle con estado explícito y fallo conservando estado.
  - `EpisodePlayerViewModelTest`: `stop()` limpia el estado y reporta la duración capturada.
  - `QueueViewModelTest`: `refreshSilently` conserva el contenido y el render excluye el episodio en curso.
- Compose:
  - `QueueScreenTest`: bloque en curso con tiempos, no duplicado, cola vacía con episodio, metadatos de duración/restante.
  - `EpisodeMiniPlayerTest`: transcurrido/restante, pausa, cambio de episodio, duración desconocida.
  - `EpisodeFullPlayerSheetTest`: detener es icono sin texto, etiqueta "Detener", favorito alterna sin cerrar.
  - `PodcastCardsTest`: corazón lleno/vacío de `EpisodeCard` y `EpisodeRow` según `isFavorite`.
  - `SettingsPodcastStartTest`: cinco opciones, valor por defecto, selección.
  - `StatsEntryFlowTest`: encabezado de radio actualizado y selección de modo (si se testea a nivel de grafo).
  - `PodcastStatsScreenTest`: encabezado, bloques de gráficas, vacío, error y reintento.
- Tests existentes a actualizar:
  - `StatsEntryFlowTest` espera "Tus estadísticas de radio".
  - `SettingsViewModelTest` añade el mock de `podcastStartScreen`.
  - `SettingsLabelsTest` cubre las etiquetas de `PodcastStartScreen`.
  - Cualquier test de `SettingsContent` que dependa del orden de las secciones.
- No se añaden tests instrumentados para el temporizador ni para radio; el cambio no los toca.

**Rationale**: las reglas de tiempo y arranque son puras y baratas de testear; los flujos visuales críticos se cubren con Compose. La combinación detecta regresiones sin multiplicar tests frágiles.

**Alternatives considered**:
- Solo tests de UI: descartado por lentitud y por mezclar reglas con renderizado.
- Solo tests unitarios: descartado porque no verifican que la cola no duplique el episodio ni que el detener sea un icono.

---

## R8 — Rendimiento, dependencias y cierre limpio

**Decision**: no añadir dependencias ni trabajo de red; reutilizar el sondeo y los datos existentes; limpiar el estado al detener.

- El sondeo de 1 s ya está en `EpisodePlaybackController`; la cola y la tarjeta solo observan.
- La cola se refresca de forma silenciosa al volver a primer plano: `QueueViewModel.refreshSilently()` conserva el contenido actual y actualiza los progresos; no muestra un spinner que tape la lista.
- Las estadísticas de podcast mantienen las 7 consultas en paralelo; no se añade caché ni persistencia.
- `stop()` captura `episodeId`, `durationMs` y `positionMs` antes de limpiar; reporta con la duración capturada y después deja `EpisodePlayerUi` sin episodio. El final sin siguiente aplica el mismo cierre.
- Sin migración de Room, sin permisos nuevos, sin cambios de `AndroidManifest`.

**Rationale**: el rendimiento no se degrada porque no hay nuevos ciclos de sondeo ni peticiones. El cierre limpio evita una tarjeta con un botón de play que no puede reanudar y cumple FR-035.

**Alternatives considered**:
- Dejar el episodio en `EpisodePlayerUi` al detener: descartado porque la tarjeta queda visible y el play no funciona.
- Persistir el tiempo restante o el estado del episodio: descartado por YAGNI y porque el servidor ya guarda el progreso.
- Añadir un `WorkManager` o un servicio para refrescar la cola: descartado por simplicidad.
