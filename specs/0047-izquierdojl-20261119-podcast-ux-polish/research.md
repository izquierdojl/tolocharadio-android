# Research: Mejoras de experiencia podcast

**Feature**: `0047-izquierdojl-20261119-podcast-ux-polish` | **Date**: 2026-11-19

Este documento resuelve las incógnitas técnicas del plan. No hay `NEEDS CLARIFICATION` pendientes: las decisiones de producto se cerraron en la spec y las de implementación se justifican aquí.

---

## R1 — Posición de reanudación desde la cola

**Decision**: antes de arrancar un episodio, calcular la posición con una regla pura de dominio y, si el episodio no trae progreso, consultar `GET /podcasts/episodes/{id}/progress`. Se aplica tanto a la selección manual desde la cola como al avance automático al terminar el anterior.

- El progreso embebido en `PodcastEpisode` se usa si existe (respuesta de cola/portada).
- Si `progress == null`, `EpisodePlayerViewModel` llama a `ProgressRepo.load(episodeId)` (ya inyectado) y reutiliza el resultado.
- Regla `resumePositionSec(progress, durationSec)`:
  - `progress == null` → `0`.
  - `progress.completed` → `0`.
  - `positionSec <= 0` → `0`.
  - `durationSec != null && positionSec >= durationSec` → `0`.
  - resto → `positionSec`.
- `onEnded` deja de llamar a `startPlayback(..., startMs = 0)` y pasa por el mismo resolvedor.
- La latencia extra solo ocurre cuando falta el progreso; en ese caso se muestra el estado de carga existente.

**Rationale**: el contrato de la spec 0046 declara progreso en las listas, pero la cola puede devolverlo ausente o desactualizado. El respaldo por episodio es una petición pequeña y ya existe. Corregir en cliente evita tocar el backend (FR-052) y cubre también el auto-avance.

**Alternatives considered**:
- Cambiar `/podcasts/queue` para incluir siempre progreso: descartado por FR-052 y porque exige desplegar servidor.
- Guardar el progreso localmente en Room: descartado por duplicar la fuente de verdad y no resolver el caso de otro dispositivo.
- Arrancar siempre con lo embebido: descartado porque es justo el fallo reportado.

---

## R2 — Retroceso y avance en la notificación multimedia

**Decision**: usar las preferencias de botones de la `MediaSession` con los iconos integrados de Media3 y los comandos de seek del player.

- `ExoPlayer.Builder.setSeekBackIncrementMs(15_000)` y `setSeekForwardIncrementMs(30_000)` en `PlaybackPlayerFactory`, compartiendo constantes con la interfaz (`EPISODE_SKIP_BACK_SEC`, `EPISODE_SKIP_FORWARD_SEC`).
- Cuando hay un episodio activo, `RadioPlaybackService` aplica:
  - `CommandButton.Builder(CommandButton.ICON_SKIP_BACK_15).setPlayerCommand(Player.COMMAND_SEEK_BACK).setDisplayName("Retroceder 15 segundos")`, con slot `SLOT_BACK`.
  - `CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30).setPlayerCommand(Player.COMMAND_SEEK_FORWARD).setDisplayName("Avanzar 30 segundos")`, con slot `SLOT_FORWARD`.
- `DefaultMediaNotificationProvider` coloca los botones de los slots back/forward en la vista compacta (3 acciones: atrás, play/pausa, adelante) y sustituye a “anterior/siguiente”, que no aportan con un solo episodio.
- En radio, las preferencias siguen siendo stop/mute (y retry si hay fallo recuperable), con slot `SLOT_OVERFLOW` para no desplazar play/pausa.
- La preferencia se recalcula al cambiar `EpisodePlaybackController.state` (Idle ↔ Active) y al cambiar el estado de Cast.
- Si el contenido no admite seek, el player no expone `COMMAND_SEEK_BACK/FORWARD` y la acción no se ofrece; además, en radio no se publican esos botones aunque el player los soporte.
- Los saltos se limitan solos: `seekBack()` a 0 y `seekForward()` al final; el final dispara el flujo normal de completado.

**Rationale**: Media3 1.11 ya trae `ICON_SKIP_BACK_15` e `ICON_SKIP_FORWARD_30`, `CommandButton.setSlots` y `MediaSession.setMediaButtonPreferences`. El proveedor por defecto respeta los slots back/forward en la vista compacta, así que no hace falta una notificación propia ni iconos personalizados. Los incrementos del player garantizan que la notificación y la UI hagan exactamente el mismo salto.

**Alternatives considered**:
- Notificación completamente personalizada: descartada por coste, mantenimiento y riesgo de regresión en radio/Cast.
- `CommandButton` con comandos de sesión propios: descartado porque los comandos de player ya existen y funcionan con Bluetooth/Cast.
- Dejar que el proveedor muestre “anterior/siguiente”: descartado porque con un único `MediaItem` no aporta y no cumple “adelantar y atrasar”.

---

## R3 — Potenciador de voz

**Decision**: compresor de rango dinámico + ganancia de compensación + limitador suave, implementado como `AudioProcessor` de Media3 y aplicado solo a episodios locales.

- `VoiceBoostDsp`: clase Kotlin pura, sin Android, que procesa `ShortArray` estéreo:
  - Detector de envolvente con ataque ~10 ms y liberación ~150 ms.
  - Umbral orientativo −24 dBFS, ratio 3:1, makeup +6 dB.
  - Limitador con techo −1 dBFS para evitar clipping.
  - Estado interno (envolvente y ganancia suavizada) conservado entre búferes; `reset()` al cambiar de pista.
- `VoiceBoostAudioProcessor`: extiende `BaseAudioProcessor`; acepta PCM 16 bits, delega en `VoiceBoostDsp` y hace bypass copiando el búfer cuando la política está desactivada.
- `VoiceBoostController` (`@Singleton`): preferencia persistida en `InstancePrefs` (`voiceBoost: Flow<Boolean>`, por defecto `false`), estado `episodeActive` por sesión y `castConnected`; `effective = preference && episodeActive && !castConnected`.
- `PlaybackPlayerFactory` crea un `VoiceBoostAudioProcessor` nuevo por cada `ExoPlayer` (evita reutilizar el mismo procesador entre el player viejo y el nuevo durante un cambio de perfil) y lo inyecta con un `DefaultRenderersFactory` que sobrescribe `buildAudioSink` para construir `DefaultAudioSink` con `setAudioProcessors`.
- `EpisodePlaybackController` marca `episodeActive = true` al arrancar un episodio y `false` al parar o al ceder el player a la radio; `PlayerViewModel` lo desactiva al arrancar radio. Cast no usa el sink local, así que no recibe el efecto.
- La UI (hoja del reproductor) muestra un `FilterChip`/interruptor con etiqueta “Potenciador de voz”, estado visible y descripción accesible; el cambio se aplica en el siguiente búfer y se persiste al instante.

**Rationale**: es la única forma de mejorar voces bajas sin subir ruido ni afectar a la radio. El procesador es pequeño, determinista y testeable en JVM separando `VoiceBoostDsp` del envoltorio Android. No añade dependencias.

**Alternatives considered**:
- `LoudnessEnhancer` sobre el `audioSessionId`: descartado porque sube todo (ruido incluido), no comprime y complica el ciclo de vida de la sesión de audio.
- Ecualizador gráfico: descartado por no mejorar la inteligibilidad de voces bajas y por más UI/ajustes.
- Procesar solo en la reproducción descargada: descartado porque el usuario espera el efecto también en streaming.

---

## R4 — Insignia de cola

**Decision**: `QueueBadgeViewModel` a ámbito de actividad que observa `QueueRepo.queue` y publica el número de pendientes.

- `pendingCount(items, playingId)` = `items.count { it.id != playingId }`.
- `playingId` llega desde `EpisodePlayerViewModel.ui.episode?.id`.
- El VM expone `count: StateFlow<Int>` y `refresh()`, que llama a `QueueRepo.load()`.
- Se refresca al entrar en modo Podcasts y en `ON_RESUME`; las mutaciones locales (añadir, quitar, reordenar, vaciar) ya publican un `QueueState` nuevo en el repo y actualizan la insignia al instante.
- `0` → sin insignia; `> 99` → texto “99+”; descripción accesible “N episodios en cola”.
- Si `load()` falla, se conserva el último valor conocido y no se muestra error (FR-035).

**Rationale**: el contador del endpoint `/queue/count` incluye el episodio en curso y no coincide con la lista visible de la pantalla de cola; el usuario eligió “solo pendientes”. Cargar la cola es aceptable (≤ 200 episodios) y ya se hace al abrir Inicio/Cola.

**Alternatives considered**:
- Usar `/queue/count` menos uno: descartado porque no siempre hay un episodio en curso dentro de la cola.
- Encuesta periódica: descartada por batería; `ON_RESUME` + mutaciones cubre el caso real.
- Nueva API de conteo de pendientes: descartada por FR-052.

---

## R5 — Progreso de descarga

**Decision**: progreso efímero en `DownloadsRepo`, sin migración de Room.

- `queryStatus` pasa a devolver `status`, `uri`, `bytesDownloaded` y `totalBytes` leyendo `COLUMN_BYTES_DOWNLOADED_SO_FAR` y `COLUMN_TOTAL_SIZE_BYTES` (columnas ausentes o `-1` → desconocido).
- `DownloadsRepo` expone `progress: StateFlow<Map<Long, DownloadProgress>>` con `fraction: Float?` (0..1) calculada solo si `totalBytes > 0`.
- `monitor()` actualiza el mapa en cada sondeo (1 s) y lo limpia al completar o fallar.
- `refresh()` relanza el monitor de los registros `RUNNING` que quedaron tras cerrar la app (con un set de monitors activos para no duplicar).
- `download()` comprueba el estado real antes de encolar: si ya está `RUNNING`, devuelve el id existente y no crea otra transferencia.
- La UI deriva tres estados: `descargable`, `descargando(fraction?)`, `descargado`.

**Rationale**: el `DownloadManager` ya ofrece los bytes; persistirlos en Room obligaría a una migración y a mantener datos que el sistema es dueño. El sondeo de 1 s solo existe mientras hay descargas activas.

**Alternatives considered**:
- Añadir columnas a `episode_downloads`: descartado por migración y por duplicar estado del sistema.
- Barra indeterminada siempre: descartado porque el requisito pide porcentaje.
- Descarga propia con OkHttp: descartada por YAGNI y por perder la integración con Descargas de Android.

---

## R6 — Tarjetas de programa y episodio

**Decision**: altura estable por estructura, no por intrínsecos.

- `PodcastCard`: portada 1:1; bloque de texto con título `minLines = 2`, `maxLines = 2` y autor/episodios a 1 línea; fila de acciones propia debajo (baja de suscripción cuando aplica).
- `EpisodeCard`: portada 1:1; título `minLines = 2`, `maxLines = 2`; metadatos a 1 línea con el nombre del podcast; fila inferior con reproducir/pausar y favorito (se retiran los overlays sobre la portada).
- La fila de acciones tiene altura fija y no reduce el ancho del texto.
- `minLines` garantiza la misma altura en tarjetas de la misma fila sin medir intrínsecos en la cuadrícula.
- Se mantienen los `contentDescription` existentes y el tamaño táctil ≥ 48 dp.

**Rationale**: `IntrinsicSize` dentro de `LazyVerticalGrid` es costoso y frágil; reservar dos líneas de título y una de metadatos da un alto uniforme y predecible con cualquier título.

**Alternatives considered**:
- `Modifier.height(IntrinsicSize.Max)` por fila: descartado por coste de medida.
- Altura fija en dp: descartada porque rompe con el tamaño de letra del sistema al 200 %.
- Dejar los botones sobre la portada: descartado porque el requisito pide las acciones debajo y un título con más espacio.

---

## R7 — Cabecera con hamburguesa y logotipo adaptable

**Decision**: `TopAppBar` con `navigationIcon` de hamburguesa y título con logotipo que se reduce a emblema.

- `TolochaLogo(showText: Boolean)`: mantiene emblema + texto o solo emblema con `contentDescription`.
- En el título, `BoxWithConstraints` decide `showText` según el ancho disponible (umbral orientativo 150 dp); si no hay cota, se muestra el texto.
- Acciones: selector Radio | Podcasts (si procede) y Cast. Nada más.
- El estado del cajón vive en `TolochaNavGraph`; `ModalNavigationDrawer` envuelve el `Scaffold` y se cierra al navegar o al tocar fuera.

**Rationale**: conserva la identidad visual pedida y elimina el solape sin depender de anchos de pantalla concretos. El cajón es el patrón Material para un menú de hamburguesa y da espacio a etiquetas largas.

**Alternatives considered**:
- Ocultar siempre el logotipo: descartado por la clarificación 2026-11-19 (se mantiene completo y se reduce si no cabe).
- `DropdownMenu` anclado: descartado por menor accesibilidad y por competir con el contenido.
- Barra inferior con más destinos: descartado por el objetivo de ganar espacio.

---

## R8 — Temporizador dentro del cajón

**Decision**: reutilizar `SleepTimerViewModel` y `SleepTimerDuration` en una fila del cajón.

- `SleepTimerDrawerItem`: fila con etiqueta “Temporizador”, estado activo (minutos restantes) y menú de duraciones/cancelar.
- El contenido del menú se extrae del actual `SleepTimerButton` a un composable reutilizable; el botón-icono de la cabecera se elimina al quedar sin uso.
- El temporizador sigue siendo transitorio (no persiste), como hoy.

**Rationale**: evita duplicar la lógica de duraciones y mantiene el estado visible en el único punto de acceso. Elimina código muerto (constitución V).

**Alternatives considered**:
- Mantener el icono en la cabecera: contradice “el resto al menú de hamburguesa”.
- Duplicar el menú en el cajón: descartado por duplicación.

---

## R9 — Estrategia de pruebas

**Decision**:

- **JVM**: `EpisodeResumeTest`, `QueueBadgeTest`, `VoiceBoostPolicyTest`, `VoiceBoostDspTest`, `QueueBadgeViewModelTest`, `EpisodePlayerViewModelTest` (MockK para controller/progress/queue/use cases) y ampliación de `DownloadsRepoTest` (cursor simulado, fracción, duplicados, re-monitor).
- **Compose (`androidTest`)**: `TolochaTopBarTest` (hamburguesa, logo, modo, Cast, ancho estrecho), `TolochaDrawerTest` (entradas y temporizador), `TolochaNavigationBarTest` (4 destinos + insignia), `PodcastCardsTest` (dos líneas, acciones debajo, altura uniforme), `DownloadProgressButtonTest` (porcentaje/indeterminado) y `VoiceBoostToggleTest` (estado y accesibilidad).
- **Red-Green**: cada regla nueva se acompaña del test que falla antes del cambio, según la constitución III.

**Rationale**: cubre las capas exigidas (domain/data/ViewModel) y los flujos críticos de UI sin depender del servidor. El DSP puro se prueba en JVM; el `AudioProcessor` se valida en el dispositivo con el escenario manual de quickstart.

**Alternatives considered**:
- Solo tests instrumentados: descartado por lentitud y por la regla de tests unitarios obligatorios.
- Robolectric: no está en el proyecto y añadirlo viola YAGNI.

---

## Resumen de riesgos

| Riesgo | Mitigación |
|--------|------------|
| El proveedor de notificación no respeta los slots back/forward en la versión 1.11.0 | Test instrumentado en API 26/33/37 y, si falla, subclase de `DefaultMediaNotificationProvider` que fije los índices compactos (alternativa documentada) |
| Cambio de player (perfil de búfer) reutiliza el procesador de audio | Una instancia de procesador por player; el estado vive en el controlador |
| La cola no trae progreso y añade latencia al pulsar | Solo se consulta cuando falta; se muestra el estado de carga ya existente |
| Descarga en curso tras cerrar la app no se monitoriza | `refresh()` relanza el monitor de registros `RUNNING` con guarda de duplicados |
| El cajón tapa el mini-player o rompe atrás | `ModalNavigationDrawer` gestiona atrás; la reproducción vive fuera del grafo y no se reinicia |

---

# Extensión 2026-11-21 — Búsqueda en Suscripciones

## R10 — Filtro local de podcasts

**Decision**: normalizar el término y el título con NFD, eliminar las marcas diacríticas y comparar en minúsculas con coincidencia parcial; aplicar el filtro después de ordenar y conservar el orden resultante.

- `normalizeSearchText(value)`:
  - `Normalizer.normalize(value, Normalizer.Form.NFD)`.
  - Eliminar el bloque `\p{M}+`.
  - `lowercase(Locale.ROOT)`.
- `filterSubscriptionsByTitle(items, term)`:
  - `term.trim()` vacío → devuelve la lista completa (copia).
  - Si no → `items.filter { normalizeSearchText(it.title).contains(normalizeSearchText(term)) }`.
- El filtro se aplica en la pantalla sobre el resultado de `sortSubscriptions`, de modo que se respeta el orden elegido (recientes, nombre, manual).
- La normalización es una función pura de `domain`, sin Android, testeable en JVM.

**Rationale**: replica `textMatch.ts` de la web y cumple FR-060/SC-012 sin red ni cambios de servidor. `String.contains(ignoreCase = true)` no ignora tildes y fallaría con «camion»/«camión».

**Alternatives considered**:
- Filtrar en el servidor: no existe un endpoint de búsqueda de suscripciones y añadirlo contradice FR-069.
- Usar `Collator`: sirve para ordenar, pero no para coincidencia parcial.
- Normalizar solo a minúsculas: no resuelve los acentos.

---

## R11 — Búsqueda de episodios y carreras

**Decision**: reutilizar `EpisodesRepo.searchEpisodes` a través de un caso de uso de dominio, con página de 20, deduplicación por id y una guarda de generación para descartar respuestas tardías.

- `SearchSubscriptionEpisodesUseCase(term, offset, limit)`:
  - `term.trim()` vacío → `ApiResult.Err(DomainError.Validation)` sin llamar a red.
  - Delegar en `EpisodesRepo.searchEpisodes(term, offset, limit)`.
- El ViewModel guarda `submittedTerm` y `results`; al confirmar:
  - Cancela la búsqueda anterior (`searchJob?.cancel()`).
  - Incrementa `searchGeneration`.
  - Llama al caso de uso con `offset = 0`, `limit = 20`.
  - Solo publica el resultado si la generación sigue vigente.
- `loadMore()` usa `offset = results.size`, añade con `distinctBy { episode.id }` y actualiza `hasMore` desde `pagination.hasMore`.
- El servidor ya limita la búsqueda a las suscripciones de la persona, por título, sin distinguir mayúsculas ni tildes, en orden de publicación descendente e incluyendo completados. No se filtra por estado en cliente.
- La guarda de generación es necesaria porque `safeCall` captura `Exception` (incluida `CancellationException`) y podría publicar un error de una búsqueda cancelada. No se modifica `safeCall` en esta extensión para no ampliar el radio de cambio.

**Rationale**: el endpoint y el DTO ya existen y la web los consume; el caso de uso mantiene la regla de validación en `domain` y el ViewModel solo orquesta estado. La generación evita resultados obsoletos sin tocar la capa de red.

**Alternatives considered**:
- Añadir un endpoint de búsqueda de suscripciones: descartado por FR-069 y porque el backend ya ofrece la búsqueda de episodios suscritos.
- Descargar todos los episodios y filtrar en cliente: descartado por tráfico y memoria.
- Corregir `safeCall` para relanzar `CancellationException`: buena práctica, pero cambio transversal; se deja como mejora futura.

---

## R12 — Composición de la pantalla y navegación

**Decision**: buscador bajo la cabecera, visible solo cuando hay suscripciones; resultados en lugar de la lista; Atrás vuelve a «Podcasts» conservando el término.

- El buscador se compone de:
  - Botón de búsqueda en la cabecera, a la izquierda del orden de suscripciones; los controles están ocultos por defecto.
  - `OutlinedTextField` de una línea con etiqueta según el ámbito y `ImeAction.Search`.
  - Botón de limpiar cuando el término no está vacío.
  - `SingleChoiceSegmentedButtonRow` con «Podcasts» y «Episodios», altura mínima 48 dp.
  - Acción de buscar visible solo en «Episodios»; en «Podcasts» el filtro es inmediato.
- Al ocultar los controles se limpian término, resultados y ámbito (vuelve a «Podcasts»), de modo que la lista nunca queda filtrada sin un control visible.
- Mientras `showResults` es `true`, los resultados (`LazyColumn` con `EpisodeRow`) sustituyen a la lista y se ocultan las acciones de orden y vista de la cabecera.
- `BackHandler(enabled = showResults)` llama a `closeEpisodeSearch()`: limpia resultados y error, conserva el término y activa «Podcasts». El primer Atrás con teclado abierto lo oculta (comportamiento del sistema); el siguiente vuelve a la lista.
- Sin suscripciones (`SubscriptionsUiState.Empty`) no se muestra el buscador; se mantiene el estado vacío con «Descubrir podcasts».
- Si la baja deja la lista vacía, la pantalla pasa al estado vacío y el buscador desaparece.
- Los resultados usan los ViewModels de actividad `EpisodePlayerViewModel` y `EpisodeActionsViewModel` (mismo patrón que Inicio/Favoritos) para reproducir, favorito, cola, descarga y ficha. El favorito se deriva de `favoriteIds` y, tras `progressChanged`, se actualiza localmente el estado de escucha del episodio.

**Rationale**: cumple las clarificaciones 2026-11-21 y evita dos listas compitiendo en una pantalla pequeña. Reutilizar los ViewModels de actividad mantiene una única sesión de reproducción y un único estado de acciones.

**Alternatives considered**:
- Mantener la lista debajo de los resultados: descartado por la clarificación Q1 y por espacio.
- Pantalla o panel separado: descartado por coste de navegación y porque el buscador debe seguir visible para cambiar de término.
- Mostrar el buscador sin suscripciones: descartado por la clarificación Q5.

---

## R13 — Estrategia de pruebas

**Decision**:

- **Dominio**: `SubscriptionSearchTest` (mayúsculas, tildes, parcial, vacío, orden preservado) y `SubscriptionSearchUseCaseTest` (término vacío sin red, delegación con offset/límite).
- **ViewModel**: `SubscriptionsViewModelTest` con MockK + Turbine: búsqueda correcta, vacío, error y reintento, paginación, respuesta tardía descartada, cambio de ámbito, vaciado del término, cierre con Atrás y baja de suscripción.
- **Compose**: `SubscriptionSearchBarTest` (etiquetas, ámbitos, limpiar, buscar, estado seleccionado, 48 dp) y `SubscriptionSearchResultsTest` (carga, vacío, error, reintento, cargar más).
- **Manual**: quickstart 3.9 con emulador, TalkBack y letra al 200 %.

**Rationale**: cubre las capas exigidas por la constitución III y los flujos críticos sin depender del servidor. Los componentes stateless permiten tests de UI rápidos y estables.

**Alternatives considered**:
- Test instrumentado de la pantalla completa con Hilt: más lento y frágil; se reserva para la validación manual.
- Robolectric: no está en el proyecto y añadirlo viola YAGNI.

## R14 — Gesto de actualización y conservación del contenido

**Decision**: usar `PullToRefreshBox` y `rememberPullToRefreshState` de Material 3 dentro de un componente compartido `PullToRefreshContent`; el indicador se muestra solo durante la recarga manual y el contenido visible se mantiene.

- El componente envuelve únicamente el área desplazable (lista o cuadrícula), no la cabecera de sección.
- `refreshing` es un estado independiente del `UiState`; la carga inicial y el reintento desde error siguen mostrando el spinner o el estado existentes.
- El gesto se activa con la lista al principio; con la lista desplazada, el mismo arrastre se consume como scroll normal.
- El ViewModel no cambia a `Loading` durante la recarga manual: mantiene el `Content` y publica el resultado cuando termina.
- El contenedor es stateless: recibe `refreshing`, `onRefresh` y el contenido; no conoce repositorios ni ViewModels.
- El contenedor expone una acción personalizada de accesibilidad «Actualizar» que invoca `onRefresh` cuando no hay recarga en vuelo. Es la alternativa para TalkBack, donde el gesto de arrastre no es descubrible ni fiable; no añade ningún botón visible ni altera la cabecera.

**Rationale**: Material 3 ya está en el proyecto (BOM 2026.05.00, Material 3 1.4.0), el gesto respeta el comportamiento nativo de listas y se evita duplicar código y semántica entre Inicio y Suscripciones. La acción de accesibilidad reutiliza el mismo callback y mantiene la paridad funcional sin romper la restricción de acciones de la cabecera (FR-014).

**Alternatives considered**:
- `SwipeRefresh` de Accompanist: requiere dependencia nueva y su API está en desuso.
- `nestedScroll` propio: más código, más estados que probar y peor integración con accesibilidad.
- Botón visible de recarga en la cabecera: descartado porque FR-014 limita las acciones directas de la cabecera; la acción de accesibilidad cubre TalkBack sin añadir ruido visual.
- Reutilizar el `Loading` existente: descartado porque sustituiría el contenido por un spinner y perdería la posición de la lista.

## R15 — Concurrencia, búsqueda activa y pruebas

**Decision**:

- Cada ViewModel guarda el job de recarga y descarta una segunda solicitud mientras la primera está en vuelo.
- Inicio recarga la portada agregada; el resumen de cola y las secciones se sustituyen al publicar el resultado.
- Suscripciones recarga la lista. Con `showResults` activo:
  - En «Podcasts», la lista nueva se filtra localmente con el término vigente.
  - En «Episodios», se relanza la búsqueda con el término confirmado (`submittedTerm`), lo que limpia los resultados y pide la primera página; la generación existente descarta respuestas tardías. No se usa el texto que la persona haya podido escribir sin confirmar.
- Si la recarga falla y hay contenido, se conserva y se emite un mensaje accionable; si no lo hay, se mantiene el estado `Error` con reintento.
- La recarga no toca `EpisodePlayerViewModel`, `EpisodeActionsViewModel`, la cola ni las descargas.
- **Pruebas**: `PodcastHomeViewModelTest` (éxito, error con contenido, error sin contenido, segunda solicitud ignorada), ampliación de `SubscriptionsViewModelTest` (lista, error, búsqueda en ambos ámbitos, concurrencia) y `PullToRefreshContentTest` (indicador, callback, semántica y acción accesible «Actualizar»). La supervivencia de la reproducción cuando la respuesta ya no incluye el elemento en curso se valida en quickstart 3.10 porque el reproductor es independiente de estos ViewModels.

**Rationale**: cubre las reglas de la spec sin introducir lógica de dominio nueva; la guarda y la generación existente evitan carreras y resultados obsoletos.

**Alternatives considered**:
- Recargar también los resultados de episodios en paralelo con la lista: descartado porque duplicaría consultas y podría publicar dos estados incoherentes.
- Cancelar la búsqueda al recargar: descartado por FR-079; el término y el ámbito deben conservarse.
- Test instrumentado de la pantalla completa con Hilt: más lento y frágil; se reserva para quickstart.

## Riesgos de la extensión

| Riesgo | Mitigación |
|--------|------------|
| Una respuesta tardía pisa la búsqueda vigente | Cancelar el job y comprobar la generación antes de publicar |
| El teclado abierto consume el primer Atrás | Se acepta el comportamiento del sistema (FR-071) y se valida en quickstart |
| El buscador se solapa con la cabecera al 200 % | Componente en columna, etiquetas cortas y prueba manual con letra máxima |
| Los resultados muestran estado de favorito/escucha obsoleto | Favorito derivado de `favoriteIds`; refresco tras `progressChanged` |
| La baja deja resultados de un podcast no suscrito | Eliminar sus resultados al confirmar la baja y recargar la lista |
| La pantalla supera la complejidad de Detekt | Extraer buscador, resultados y mapeador a archivos propios |
| Un segundo arrastre lanza una consulta duplicada | Guarda de job en cada ViewModel y `refreshing` como estado de la UI |
| La recarga deja la pantalla en blanco o pierde el scroll | Mantener `Content` y envolver solo el área desplazable; no usar `Loading` en la recarga manual |
| La recarga pisa el término o los resultados de la búsqueda | Reaplicar el filtro local o repetir `searchEpisodes()` con el término vigente |
| El indicador queda atascado tras un error | `finally`/publicación de `refreshing = false` en éxito y error, con test específico |
| El gesto interfiere con el reordenado manual de suscripciones | El asa de arrastre consume el gesto; la recarga solo se activa desde el borde superior de la lista |
| TalkBack no puede ejecutar el arrastre | Acción accesible «Actualizar» en el contenido, con el mismo callback y test de semántica |
| La respuesta ya no incluye el episodio en reproducción y se corta el audio | La recarga solo sustituye el estado de la lista; no toca el player. Se valida en quickstart 3.10 |
