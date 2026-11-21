# Contract: Búsqueda en Suscripciones

**Feature**: `0047-izquierdojl-20261119-podcast-ux-polish` | **Extensión**: 2026-11-21

Contrato de la búsqueda de la pantalla de Suscripciones. No introduce una API nueva: documenta la capacidad del servidor que ya consume la web y el contrato interno de la UI.

---

## 1. API consumida (sin cambios)

`GET /api/v1/podcasts/episodes/search?q={term}&offset={offset}&limit={limit}`

- Requiere la sesión existente; no se añaden credenciales.
- `q` es obligatorio y se recorta; vacío → error de validación.
- `offset` ≥ 0; `limit` entre 1 y 100. La app usa `limit = 20`.
- Devuelve `{ items, pagination: { offset, limit, hasMore } }`.
- Cada item incluye los datos del episodio (título, fecha, duración, progreso, favorito) y el `podcast` embebido.
- El servidor limita la búsqueda a las suscripciones de la persona, por título, sin distinguir mayúsculas ni tildes, en orden de publicación descendente e incluyendo completados.
- La app no modifica el contrato ni añade parámetros.

**Errores**: los existentes de la app (`safeCall` → `DomainError`), presentados en español.

---

## 2. `SubscriptionSearchBar`

### Visibilidad

- Los controles están ocultos por defecto (`searchVisible = false`).
- Un botón de búsqueda en la cabecera, a la izquierda del orden de suscripciones, los despliega y los oculta.
- Al ocultarlos se limpian término, resultados y error, y el ámbito vuelve a `PODCASTS`; la lista se muestra completa.
- El botón usa «Mostrar búsqueda» / «Ocultar búsqueda» como descripción accesible.
- Sin suscripciones el botón no aparece.

### Entradas

| Entrada | Tipo | Descripción |
|---------|------|-------------|
| `term` | `String` | Texto actual |
| `scope` | `SubscriptionSearchScope` | `PODCASTS` o `EPISODES` |
| `searching` | `Boolean` | Búsqueda de episodios en curso |
| `onTermChange` | `(String) -> Unit` | Actualiza el término |
| `onScopeChange` | `(SubscriptionSearchScope) -> Unit` | Cambia el ámbito |
| `onSubmit` | `() -> Unit` | Confirma la búsqueda de episodios |
| `onClear` | `() -> Unit` | Limpia el término y los resultados |

### Composición

- `OutlinedTextField` de una línea:
  - Etiqueta «Buscar en tus podcasts» o «Buscar episodios en tus suscripciones» según el ámbito.
  - `ImeAction.Search`; al pulsar, ejecuta `onSubmit` solo en `EPISODES`.
  - Icono de limpiar visible cuando `term` no está vacío.
  - Acción de buscar visible solo en `EPISODES`, habilitada con término no vacío y sin búsqueda en curso.
- `SingleChoiceSegmentedButtonRow` con «Podcasts» y «Episodios», altura mínima 48 dp.
- Etiqueta visible «Buscar en» o descripción accesible equivalente para el grupo de ámbitos.

### Invariantes

- En `PODCASTS` no se lanza ninguna petición al escribir; el filtro es local.
- En `EPISODES` no se lanza ninguna petición hasta confirmar.
- El término se conserva al cambiar de ámbito y al volver con Atrás.
- Con término vacío o solo espacios, buscar no hace nada y no muestra resultados.
- El componente no conoce el repositorio ni el ViewModel; es stateless.

### Semántica para tests

Etiquetas «Buscar en tus podcasts», «Buscar episodios en tus suscripciones», «Podcasts», «Episodios», «Limpiar búsqueda», «Buscar episodios», «Mostrar búsqueda», «Ocultar búsqueda» y «Ámbito de búsqueda». El ámbito activo se anuncia como seleccionado.

---

## 3. `SubscriptionSearchResults`

### Entradas

| Entrada | Tipo | Descripción |
|---------|------|-------------|
| `state` | `SubscriptionSearchUi` | Resultados y estado de la consulta |
| `favoriteIds` | `Set<Long>` | Favoritos vigentes |
| `playerUi` | `EpisodePlayerUi` | Episodio en reproducción y estado |
| `downloads` | `Map<Long, DownloadRecord?>` | Descargas completadas |
| `downloadProgress` | `Map<Long, DownloadProgress>` | Progreso de descarga |
| `downloadsEnabled` | `Boolean` | Si el servidor permite descargas |
| `onPlayPause` | `(PodcastEpisode, PodcastSummary) -> Unit` | Reproducir/pausar |
| `onOpenDetail` | `(PodcastEpisode, PodcastSummary) -> Unit` | Abrir ficha |
| `onToggleFavorite` | `(PodcastEpisode) -> Unit` | Favorito |
| `onQueueNext` / `onQueueEnd` | `(Long) -> Unit` | Cola |
| `onDownload` | `(PodcastEpisode) -> Unit` | Descarga |
| `onTogglePlayed` | `(PodcastEpisode) -> Unit` | Marcado manual |
| `onLoadMore` | `() -> Unit` | Página siguiente |
| `onRetry` | `() -> Unit` | Reintentar búsqueda |

### Estados

| Estado | Condición | Presentación |
|--------|-----------|--------------|
| `Loading` | `loading == true` | Indicador de progreso y «Buscando episodios…» |
| `Empty` | `showResults && results.isEmpty() && error == null` | «Sin resultados para «término». Prueba con otro título.» |
| `Error` | `error != null` | Mensaje en español + «Reintentar» |
| `Content` | `results.isNotEmpty()` | `LazyColumn` de `EpisodeRow` + «Cargar más» si `hasMore` |
| `LoadingMore` | `loadingMore == true` | Indicador al final, sin duplicar resultados |

### Invariantes

- Los resultados sustituyen a la lista de suscripciones; no se muestran ambas a la vez.
- Solo aparecen episodios de podcasts suscritos.
- Cada fila muestra título, programa, fecha, duración y estado de escucha.
- Reproducir, favorito, cola, descarga y ficha funcionan con los mismos estados que en Inicio/Favoritos.
- «Cargar más» solo aparece si `hasMore`; al cargar, no se repiten ids ni se pierde la posición.
- Las acciones tienen descripción accesible y área táctil ≥ 48 dp.
- El contenedor de resultados usa `liveRegion = Polite` para anunciar cambios con TalkBack.

### Semántica para tests

«Buscando episodios…», «Sin resultados para …», «Reintentar», «Cargar más», títulos de episodio, «Reproducir», «Pausar», «Añadir a favoritos», «Quitar de favoritos», «Cancelar suscripción».

---

## 4. Matriz de la pantalla

| Estado de la pantalla | Buscador | Contenido |
|-----------------------|----------|-----------|
| Cargando suscripciones | No | Indicador de progreso |
| Error de suscripciones | No | Mensaje + reintentar |
| Sin suscripciones | No | Estado vacío + «Descubrir podcasts» |
| Lista con suscripciones, `PODCASTS` | Oculto por defecto; botón de búsqueda en la cabecera | Lista completa |
| Buscador desplegado, `PODCASTS` | Sí | Lista ordenada, filtrada por el término si lo hay |
| Buscador desplegado, `EPISODES`, sin confirmar | Sí | Lista de suscripciones (sin resultados) |
| Resultados de episodios | Sí (obligatorio para editar o cerrar) | Resultados en lugar de la lista; orden/vista ocultos |
| Baja que deja la lista vacía | No | Estado vacío |

---

## 5. Navegación y accesibilidad

- `BackHandler` activo solo con resultados: limpia resultados, conserva el término y activa «Podcasts».
- Con el teclado abierto, el primer Atrás lo oculta; el siguiente aplica la regla anterior.
- Los cambios de resultados se anuncian con `liveRegion = Polite` sin mover el foco.
- Con letra al 200 %, el buscador y la fila de ámbitos crecen en altura y no se solapan con la cabecera ni con los resultados.
- Todas las cadenas visibles están en español.
