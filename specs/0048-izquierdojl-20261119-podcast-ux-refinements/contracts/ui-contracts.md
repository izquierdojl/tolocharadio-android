# UI Contracts: Refinamientos de experiencia podcast

**Feature**: `0048-izquierdojl-20261119-podcast-ux-refinements` | **Date**: 2026-11-19

Contratos internos de presentación. **No hay API externa nueva**: se reutilizan los DTO y endpoints de las specs 0041/0046/0047. Estos contratos fijan qué debe poder observar y hacer la persona usuaria y qué semántica deben exponer los tests.

## Terminología

- **Detalle del episodio / reproductor completo**: la hoja “now playing” (`EpisodeFullPlayerSheet`).
- **Tarjeta inferior / mini player**: `EpisodeMiniPlayer`.
- **Bloque en curso / cabecera de cola**: `QueueNowPlayingCard`.

---

## 1. Cola — bloque del episodio en curso

### `QueueNowPlayingCard`

| Entrada | Tipo | Obligatoria | Descripción |
|---------|------|-------------|-------------|
| `episode` | `PodcastEpisode` | Sí | Episodio en curso |
| `podcast` | `PodcastSummary?` | No | Programa para título/portada |
| `positionMs` | `Long` | Sí | Posición viva |
| `durationMs` | `Long` | Sí | Duración conocida; 0 si no |
| `playing` | `Boolean` | Sí | Reproduciendo o pausado |
| `onOpen` | `() -> Unit` | Sí | Abre el reproductor completo |

### Composición

- Portada cuadrada (48–56 dp).
- Programa en estilo terciario, una línea.
- Título del episodio, hasta dos líneas.
- Línea de tiempo: transcurrido a la izquierda y duración total a la derecha (`m:ss`/`h:mm:ss`).
- Barra de avance cuando `durationMs > 0`; sin barra ni porcentaje inventado si es desconocida.
- Sin acciones de reordenar ni quitar; no es un pendiente.

### Invariantes

- Aparece aunque el episodio no forme parte de la cola.
- Si el episodio forma parte de la cola, no se duplica en la lista inferior.
- Se muestra en reproducción y en pausa; no se muestra si no hay episodio.
- Permanece fijo sobre la lista de pendientes; no forma parte del scroll.
- El tiempo se actualiza con el sondeo de 1 s del reproductor.
- Al tocar el bloque se abre el reproductor completo sin alterar la reproducción.
- Con la cola sin pendientes, se muestra el bloque y un mensaje de "no hay más episodios en cola".

### Semántica para tests

- Tag: `TolochaTestTags.QUEUE_NOW_PLAYING`.
- Descripción accesible del bloque: "En reproducción: {título}. {transcurrido} de {total}".
- La barra de avance tiene descripción "Avance del episodio".

---

## 2. Cola — metadatos de cada fila

### `QueueEpisodeMeta`

| Zona | Contenido | Regla |
|------|-----------|-------|
| Línea principal | Título | Una línea, elipsis |
| Línea secundaria | Duración y restante | `labelSmall`, una línea |
| Acciones | Arrastrar, quitar | Sin cambios |

### Reglas de presentación

- Duración: `formatEpisodeDuration(episode.durationSec ?: progress.durationSec)` si el valor es `> 0`.
- Restante: solo si `positionSec > 0`, no completado y `positionSec < durationSec`.
- Redondeo: hacia arriba al minuto entero.
- Menos de un minuto: "Queda menos de 1 min".
- Sin duración: no se muestra duración ni restante (nunca `"--:--"` en esta línea).
- Sin empezar o completado: solo duración.
- La línea se actualiza al volver a la cola (refresco silencioso) o al refrescar.

### Semántica para tests

- Tag: `TolochaTestTags.QUEUE_EPISODE_META`.
- Descripción accesible: "Duración {duración}. Quedan {N} minutos" o "Duración {duración}".
- En filas con restante, el texto visible incluye "Quedan" o "Queda menos de 1 min".

---

## 3. Reproductor completo — acciones secundarias

### `EpisodeFullPlayerSheet`

| Acción | Presentación | Semántica |
|--------|--------------|-----------|
| Detener | `IconButton` con icono `Close`, mismo tamaño que los controles | `contentDescription = "Detener"`, sin texto visible |
| Favorito | `FavoriteButton` con corazón lleno/vacío | "Añadir a favoritos" / "Quitar de favoritos" |

### Composición

- Las acciones se sitúan en una fila propia bajo los controles de transporte, con separación uniforme.
- Detener conserva el comportamiento actual: para la reproducción y cierra la hoja.
- Favorito alterna sin pausar, reiniciar ni cerrar.
- El estado del favorito es el vivo: si la lista de favoritos ya cargó, manda `favoriteIds`; si no, el flag del episodio.

### Invariantes

- No queda ningún `TextButton("Detener")` visible.
- El icono de detener tiene área táctil estándar y está alineado con el resto.
- El favorito se refleja en la ficha, las listas y la pantalla de favoritos en ≤ 2 s desde que la operación se confirma.
- Las tarjetas y listas reciben `isFavorite` ya superpuesto desde `favoriteIds`; los tests de `EpisodeCard` y `EpisodeRow` cubren el icono y la superposición se mantiene en Inicio, Suscripciones y Favoritos.
- Un fallo conserva el estado anterior y muestra el mensaje accionable existente.

### Semántica para tests

- Nodo con `contentDescription = "Detener"`.
- No debe existir un nodo de texto exacto "Detener".
- Tras pulsar favorito, el nodo cambia entre "Añadir a favoritos" y "Quitar de favoritos".

---

## 4. Tarjeta de reproducción inferior — tiempos

### `EpisodeMiniPlayer`

| Zona | Contenido | Regla |
|------|-----------|-------|
| Fila 1 | Portada, programa, título | Sin cambios |
| Fila 2 | Atrás 15 s, play/pausa, adelante 30 s, detener | Sin cambios |
| Fila 3 | Transcurrido a la izquierda, restante a la derecha | `labelSmall` |
| Fila 4 | Barra de avance | Solo si `durationMs > 0` |

### Reglas

- Transcurrido: `formatPlaybackTime(positionMs)`.
- Restante: `"-" + formatPlaybackTime(remainingMs)` cuando la duración es válida.
- Se actualiza al menos una vez por segundo mientras reproduce; queda fijo al pausar.
- Refleja de inmediato seeks, saltos y cambios de episodio.
- Duración desconocida o cero: solo transcurrido; sin restante ni barra.
- Detener o no haber episodio: la tarjeta no se muestra; no quedan cifras residuales.
- El toque en la tarjeta sigue abriendo el reproductor completo.

### Semántica para tests

- Tag: `TolochaTestTags.EPISODE_TIME_ROW`.
- Descripción accesible: "Transcurrido {m:ss}. Quedan {m:ss}" o solo "Transcurrido {m:ss}".
- El texto del restante incluye el signo menos.

---

## 5. Configuración — pantalla de arranque de podcast

### `SettingsContent`

| Sección | Control | Valores |
|---------|---------|---------|
| Pantalla de arranque en radio | Segmented buttons existente | Explorar, Favoritos, Historial |
| Pantalla de arranque en podcast | Desplegable | Inicio, Descubrir, Suscripciones, Cola, Favoritos |

- La sección de radio se renombra para distinguirla de la de podcast.
- La de podcast se coloca después de la de radio y antes de "Sección inicial".
- Valor por defecto: Inicio.
- Texto de ayuda: se aplica al arrancar con la sección inicial Podcasts y un servidor con podcasts.
- Si el servidor no ofrece podcasts, la preferencia sigue visible pero no tiene efecto; el arranque usa radio.
- Los dos selectores son independientes.

### Semántica para tests

- Tag: `TolochaTestTags.PODCAST_START_SCREEN`.
- Etiquetas visibles: "Pantalla de arranque en radio", "Pantalla de arranque en podcast".
- Etiqueta actual del desplegable: "Inicio" por defecto.
- Descripción accesible: "Pantalla de arranque de podcast: {valor}".

---

## 6. Gráficas — selección por modo y paridad

### Entrada

- La fila "Gráficas" de Configuración abre:
  - `Routes.STATS` si el modo activo es Radio o el servidor no ofrece podcasts.
  - `Routes.PODCAST_STATS` si el modo activo es Podcasts y hay podcasts.
- No hay selector Radio | Podcasts dentro de la pantalla ni pestañas.

### Encabezados

| Modo | Título |
|------|--------|
| Radio | "Tus estadísticas de radio" |
| Podcast | "Tus estadísticas de podcast" |

### Vista de podcast

| Bloque | Fuente | Componente |
|--------|--------|------------|
| Resumen | `PodcastStatsSummaryDto` | Tarjetas de resumen |
| Evolución | `PodcastTimelineDto` | `StatsTimelineChart` |
| Programas más escuchados | `PodcastTopPodcasts` | `RankingBars` |
| Episodios más escuchados | `PodcastTopEpisodes` | `RankingBars` |
| Hábitos | `PodcastHabits` | `HabitsHeatmap` |
| Categorías | `PodcastCategories` | `ShareBars` |
| Actividad reciente | `PodcastRecents` | `RecentActivityList` |

### Estados

| Estado | Presentación |
|--------|--------------|
| Carga | Indicador centrado |
| Vacío | "Todavía no hay escucha de podcast. Reproduce un episodio y vuelve aquí." |
| Error | Mensaje en español + "Reintentar" |
| Error de credenciales | Mensaje + "Editar servidor" |
| Contenido | Bloques anteriores, en ese orden |

### Invariantes

- No se muestran datos de radio en la vista de podcast ni viceversa.
- Un cambio de modo con Gráficas abierta intercambia la ruta; no deja contenido obsoleto. La selección se resuelve con la función pura `routeAfterModeChange(...)`.
- Sin podcasts, Gráficas muestra radio.
- Un fallo de cualquier bloque no se sustituye silenciosamente por el otro modo.
- La privacidad se mantiene: solo la cuenta activa, sin PII en logs.

### Semántica para tests

- Título de pantalla visible.
- Títulos de bloques: "Evolución de la escucha", "Programas más escuchados", "Episodios más escuchados", "Tus hábitos de escucha", "Categorías", "Actividad reciente".
- Estado vacío y error con reintento.

---

## 7. Contrato externo

- Sin cambios en `GET /podcasts/stats/*`, `GET /stats/*`, `GET /podcasts/queue` ni en el modelo de episodios.
- Sin credenciales, tokens ni permisos nuevos.
- La app sigue funcionando contra servidores compatibles con el OpenAPI actual.
