# Data Model: Mejoras de experiencia podcast

**Feature**: `0047-izquierdojl-20261119-podcast-ux-polish` | **Date**: 2026-11-19

No se añaden entidades persistidas ni tablas nuevas. Los modelos siguientes son, en su mayoría, estados derivados y de presentación; solo la preferencia del potenciador de voz se persiste (DataStore).

---

## 1. Progreso y reanudación

### `EpisodePlaybackProgress` (DTO existente, sin cambios)

| Campo | Tipo | Regla |
|-------|------|-------|
| `positionSec` | `Long` | Posición guardada en segundos; negativa se considera inválida |
| `durationSec` | `Long?` | Duración conocida; puede faltar |
| `completed` | `Boolean` | Si es `true`, prevalece sobre la posición |
| `updatedAt` | `Long` | Marca temporal del servidor (informativa) |

### `ResumePoint` (derivado, no persistido)

| Campo | Tipo | Origen |
|-------|------|--------|
| `episodeId` | `Long` | Episodio solicitado |
| `positionMs` | `Long` | Resultado de la regla de reanudación |
| `source` | `EMBEDDED` \| `REMOTE` \| `NONE` | De dónde salió el progreso |

**Regla `resumePositionSec(progress, durationSec)`**

1. `progress == null` → `0` (`NONE`).
2. `progress.completed` → `0`.
3. `positionSec <= 0` → `0`.
4. `durationSec != null && positionSec >= durationSec` → `0`.
5. En cualquier otro caso → `positionSec`.

**Transiciones**

| Estado de entrada | Estado de salida |
|-------------------|------------------|
| Sin progreso | `NONE` → arranque en 0 |
| Progreso parcial válido | `EMBEDDED`/`REMOTE` → arranque en la posición |
| Completado | → arranque en 0, conservando la marca manual existente |
| Progreso fuera de rango | → arranque en 0 |

**Relaciones**: un `ResumePoint` pertenece a un episodio; la cola y el auto-avance comparten la misma regla.

---

## 2. Cola e insignia

### `QueueSnapshot` (estado existente `QueueState`)

| Campo | Tipo | Regla |
|-------|------|-------|
| `revision` | `Long` | Revisión optimista del servidor |
| `items` | `List<PodcastEpisode>` | Orden vigente; hasta el límite del servidor (200) |
| `limit` | `Int` | Capacidad máxima |

### `QueueBadge` (derivado, no persistido)

| Campo | Tipo | Regla |
|-------|------|-------|
| `pendingCount` | `Int` | `items.count { it.id != playingId }`, nunca negativo |
| `displayText` | `String?` | `null` si 0; `"99+"` si > 99; si no, el número |
| `contentDescription` | `String` | “N episodios en cola” / “1 episodio en cola” |

**Transiciones**

| Evento | Efecto |
|--------|--------|
| Añadir / quitar / reordenar / vaciar | `QueueRepo` publica un `QueueState` nuevo → insignia recalculada |
| Empieza a sonar un episodio de la cola | Se excluye de `pendingCount` |
| Termina / se detiene el episodio | Vuelve a contar si sigue en la cola |
| Cambio remoto | Se recalcula al recargar (`ON_RESUME` / entrar en modo Podcasts) |
| Cola vacía | `displayText = null` |

---

## 3. Descarga

### `DownloadProgress` (efímero, no persiste)

| Campo | Tipo | Regla |
|-------|------|-------|
| `episodeId` | `Long` | Clave |
| `bytesDownloaded` | `Long` | ≥ 0; `-1` del sistema se normaliza a 0 |
| `totalBytes` | `Long?` | `null` si el sistema no lo conoce o es ≤ 0 |
| `fraction` | `Float?` | `bytesDownloaded / totalBytes` acotado a `0f..1f`; `null` si no hay total |

### `DownloadUiState` (derivado para la UI)

| Estado | Condición | Presentación |
|--------|-----------|--------------|
| `NotDownloaded` | Sin registro o `FAILED` | Icono de descarga habilitado |
| `Downloading(fraction?)` | Registro `RUNNING` | Anillo de progreso + porcentaje; indeterminado si `fraction == null` |
| `Downloaded` | Registro `COMPLETE` con URI legible | Icono de descargado; sin porcentaje |
| `Failure(message)` | Registro `FAILED` | Vuelve a descargable + aviso en español |

**Transiciones**

```
NotDownloaded --pulsar--> Downloading(0) --avance--> Downloading(1) --> Downloaded
        ^                        |                                  |
        |                        +--fallo/cancelación--> Failure --> NotDownloaded (reintento)
        +----------------------------- borrar ----------------------+
```

**Reglas**

- Sondeo cada 1 s solo mientras haya descargas `RUNNING`.
- `download()` no crea una segunda transferencia si la anterior sigue `RUNNING`.
- Al reiniciar la app, `refresh()` reconcilia el estado del sistema y relanza el monitor de las `RUNNING`.
- No hay migración de Room; la entidad `episode_downloads` no cambia.

---

## 4. Potenciador de voz

### `VoiceBoostPreference` (persistida en DataStore)

| Campo | Tipo | Regla |
|-------|------|-------|
| `enabled` | `Boolean` | Por defecto `false`; se conserva entre episodios y reinicios |
| `key` | `"voice_boost"` | Clave de preferencias |

### `VoiceBoostRuntime` (transitorio)

| Campo | Tipo | Regla |
|-------|------|-------|
| `episodeActive` | `Boolean` | `true` mientras haya un episodio cargado en local |
| `castConnected` | `Boolean` | `true` si la sesión remota está conectada |
| `effective` | `Boolean` | `preference && episodeActive && !castConnected` |

**Transiciones**

| Evento | Efecto |
|--------|--------|
| Activar/desactivar en el reproductor | Persiste y aplica en el siguiente búfer |
| Cambiar de episodio | Se mantiene la preferencia; el DSP se reinicia |
| Arrancar radio | `episodeActive = false` → bypass |
| Conectar Cast | `castConnected = true` → bypass local |
| Volver de Cast a episodio local | `castConnected = false` → se reaplica |

**Invariantes**

- El efecto nunca se aplica a radio en directo ni a la salida de Cast.
- El bypass no altera el audio (copia sin procesar).
- El limitador evita muestras fuera de rango.

---

## 5. Cabecera, cajón y barra inferior

### `TopBarState` (derivado)

| Campo | Tipo | Regla |
|-------|------|-------|
| `showLogoText` | `Boolean` | `false` si el ancho del título no alcanza el umbral |
| `podcastsEnabled` | `Boolean` | Gobierna la visibilidad del selector Radio \| Podcasts |
| `menuOpen` | `Boolean` | Estado del cajón |

### `DrawerEntry` (estático)

| `id` | Etiqueta | Acción |
|------|----------|--------|
| `TIMER` | Temporizador | Abre duraciones/cancelar; muestra minutos restantes si está activo |
| `SERVERS` | Servidores | Navega a `Routes.SERVERS` |
| `SETTINGS` | Configuración | Navega a `Routes.SETTINGS` |

### `BottomBarDestinations`

| Modo | Destinos | Configuración |
|------|----------|---------------|
| Radio | Explorar, Favoritos, Historial, Mis emisoras | Fuera de la barra |
| Podcast | Inicio, Descubrir, Suscripciones, Cola (+ insignia), Favoritos | Fuera de la barra |

**Reglas**

- El cajón está disponible en todas las pantallas con cabecera compartida (todas menos el formulario de servidor).
- Elegir una entrada cierra el cajón antes de navegar.
- Volver atrás con el cajón abierto lo cierra sin navegar.
- Configuración y Servidores siguen existiendo como rutas y siguen siendo alcanzables.

---

## 6. Tarjetas

### `CardPresentation` (derivado)

| Campo | Programa (`PodcastCard`) | Episodio (`EpisodeCard`) |
|-------|--------------------------|--------------------------|
| Portada | 1:1 | 1:1 |
| Título | `minLines = 2`, `maxLines = 2` | `minLines = 2`, `maxLines = 2` |
| Metadatos | Autor · episodios, 1 línea | Podcast · duración/estado, 1 línea |
| Acciones | Baja (si aplica), fila propia | Reproducir/pausar + favorito, fila propia |
| Altura | Estable por estructura | Estable por estructura |

**Reglas**

- La fila de acciones no reduce el ancho del título ni de los metadatos.
- Las tarjetas de una misma fila comparten altura porque el bloque de texto reserva las mismas líneas.
- Con letra al 200 % crece la tarjeta; no se solapa ni se recorta.
- Los `contentDescription` y tamaños táctiles existentes se conservan.

---

## 7. Búsqueda en Suscripciones (extensión 2026-11-21)

### `SubscriptionSearchScope` (presentación, no persiste)

| Valor | Significado |
|-------|-------------|
| `PODCASTS` | Filtra la lista de suscripciones ya cargada, sin red |
| `EPISODES` | Busca por título en las suscripciones al confirmar |

**Regla**: es único por pantalla; el valor por defecto es `PODCASTS`.

**Visibilidad del buscador**: `searchVisible` es estado local de la pantalla (`rememberSaveable`), no del ViewModel. Empieza en `false`; al ocultarlo se limpian el término, los resultados y el ámbito vuelve a `PODCASTS`.

### `SubscriptionEpisodeResult` (derivado, no persiste)

| Campo | Tipo | Origen |
|-------|------|--------|
| `episode` | `PodcastEpisode` | Mapeado desde `PodcastEpisodeSearchItem` |
| `podcast` | `PodcastSummary` | Programa embebido en el resultado |

**Regla**: `episode.id` es la clave de lista y de deduplicación. El favorito mostrado se deriva de `favoriteIds`; el resto de estado (en reproducción, descarga, completado) viene del player/acciones o de la respuesta.

### `SubscriptionSearchUi` (estado de presentación)

| Campo | Tipo | Regla |
|-------|------|-------|
| `term` | `String` | Texto actual del campo; se conserva al cambiar de ámbito y al volver con Atrás |
| `submittedTerm` | `String` | Último término confirmado; gobierna el mensaje de vacío y el encabezado de resultados |
| `scope` | `SubscriptionSearchScope` | `PODCASTS` por defecto |
| `results` | `List<SubscriptionEpisodeResult>` | Página 1 + páginas cargadas, sin ids duplicados |
| `loading` | `Boolean` | Primera página en curso |
| `loadingMore` | `Boolean` | Página siguiente en curso |
| `hasMore` | `Boolean` | `pagination.hasMore` del servidor |
| `error` | `String?` | Error accionable en español; `null` si no hay |
| `showResults` | `Boolean` | `true` cuando hay una búsqueda confirmada (éxito, vacío o error) |

**Transiciones**

| Evento | Efecto |
|--------|--------|
| Mostrar el buscador | `searchVisible = true`; se muestran campo y ámbitos |
| Ocultar el buscador | Limpia término, resultados, error y `showResults`; vuelve a `PODCASTS`; `searchVisible = false` |
| Escribir en `PODCASTS` | La lista se filtra localmente; no cambia el estado remoto |
| Escribir en `EPISODES` | No consulta; si el término queda vacío, limpia resultados y `showResults` |
| Confirmar búsqueda | `loading = true`, `showResults = true`; al terminar, resultados o error |
| Éxito con 0 coincidencias | `results = []`, `showResults = true`, mensaje con el término |
| Error | `error` informado, `showResults = true`, botón de reintento |
| Cargar más | `loadingMore = true`; al terminar, append sin duplicados y nuevo `hasMore` |
| Cambiar de ámbito | Cancela la búsqueda, limpia resultados/error/`showResults` y `submittedTerm`, y conserva `term` |
| Vaciar el término | Cancela la búsqueda y limpia resultados/`showResults`/`submittedTerm`; el término vacío no consulta |
| Atrás con resultados | Limpia resultados/error/`submittedTerm`, conserva `term` y activa `PODCASTS` |
| Baja de suscripción | Elimina de `results` los episodios del podcast dado y actualiza la lista |
| Sin suscripciones | La UI no muestra el buscador; el estado vacío existente prevalece |

**Invariantes**

- `results` nunca contiene episodios de podcasts no suscritos.
- `submittedTerm` vacío implica `showResults = false`.
- `submittedTerm` se limpia al cambiar de ámbito, vaciar el término o cerrar con Atrás.
- Una respuesta de una generación anterior nunca modifica el estado.
- Los resultados incluyen episodios completados, en curso y pendientes, con su estado visible.
- Los resultados sustituyen a la lista mientras `showResults = true`; no se muestran ambas a la vez.
- El término es único y compartido entre ámbitos.

**Persistencia**: ninguna. El estado vive en el `ViewModel` de la pantalla, por lo que sobrevive a rotación, cambio de tema y vuelta desde segundo plano; la muerte de proceso no se cubre en esta iteración.

---

## Resumen de persistencia

| Dato | Dónde | Cambia el esquema |
|------|-------|-------------------|
| Progreso de escucha | Servidor (fuente de verdad) | No |
| Cola | Servidor + `QueueRepo` en memoria | No |
| Estado/URI de descarga | Room `episode_downloads` | No |
| Progreso de descarga | Memoria (`DownloadsRepo`) | No |
| Preferencia de voz | DataStore | No (nueva clave) |
| Estado del cajón/temporizador | Memoria | No |
| Búsqueda en Suscripciones | Memoria (`SubscriptionsViewModel`) | No |
