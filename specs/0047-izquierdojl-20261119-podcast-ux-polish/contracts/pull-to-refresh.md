# Contract: Actualización por arrastre

**Feature**: `0047-izquierdojl-20261119-podcast-ux-polish` | **Extensión**: 2026-11-21

Contrato interno del gesto de recarga de las pantallas de Inicio y Suscripciones. No introduce una API nueva: reutiliza las consultas que ya alimentan ambas pantallas.

---

## 1. API consumida (sin cambios)

| Pantalla | Consulta existente | Uso |
|----------|--------------------|-----|
| Inicio | Portada agregada de podcasts (`EpisodesRepo.home()`) | Recargar continuar escuchando, novedades, programas y resumen de cola |
| Suscripciones | Lista de suscripciones (`PodcastSubscriptionsRepo.list()`) | Recargar altas y bajas |

- No se añaden parámetros, endpoints, credenciales ni DTOs.
- En Suscripciones con búsqueda de episodios activa se reutiliza la búsqueda existente (`SearchSubscriptionEpisodesUseCase`) con el término vigente.
- Los errores se presentan con el mapeo existente (`userMessage()`), en español y sin detalles técnicos.

---

## 2. `RefreshableContent`

Contenedor stateless compartido por Inicio y Suscripciones.

### Entradas

| Entrada | Tipo | Obligatoria | Descripción |
|---------|------|-------------|-------------|
| `refreshing` | `Boolean` | Sí | Hay una recarga manual en vuelo |
| `onRefresh` | `() -> Unit` | Sí | Solicita la recarga; se invoca una vez por gesto válido |
| `modifier` | `Modifier` | No | Modificador del contenedor |
| `content` | `@Composable () -> Unit` | Sí | Lista o cuadrícula desplazable |

### Composición

- `PullToRefreshBox` de Material 3 con `rememberPullToRefreshState()`.
- El indicador se sitúa en el borde superior del área desplazable y solo es visible mientras `refreshing` es `true`.
- No se envuelve la cabecera de sección ni la barra superior; solo el contenido que se recarga.
- El contenedor no conoce repositorios, ViewModels ni el estado de la pantalla; es stateless.

### Invariantes

- `onRefresh` solo se invoca con la lista al principio y tras superar el umbral del gesto.
- Con la lista desplazada, el arrastre se comporta como scroll normal; al llegar al principio, continuar arrastrando activa la recarga.
- Si `refreshing` ya es `true`, un nuevo gesto no invoca `onRefresh`.
- El contenido permanece visible y conserva su posición de scroll y su estado durante la recarga.
- El indicador desaparece al terminar, tanto en éxito como en error.
- El contenedor funciona igual en modo lista y en modo tarjetas.

### Accesibilidad

- El indicador expone una descripción de estado comprensible («Actualizando…», «Actualizado») y una región viva educada para anunciar el inicio y el fin.
- El contenido expone una acción personalizada de accesibilidad «Actualizar» que invoca `onRefresh` cuando `refreshing = false`; así la recarga no depende del gesto de arrastre con TalkBack.
- La acción no añade ningún control visible ni modifica la cabecera; reutiliza el mismo callback que el gesto.
- El gesto no mueve el foco ni la posición de lectura del lector de pantalla.
- El contenido sigue siendo navegable con TalkBack durante la recarga.
- El estado de error se anuncia mediante el mensaje visible de la pantalla, no solo por color o icono.

### Semántica para tests

Etiquetas `home_pull_refresh`, `subscriptions_pull_refresh` y `pull_refresh_indicator`; descripciones «Actualizando…», «Actualizado» y acción «Actualizar». El test del contenedor verifica que el gesto invoca `onRefresh`, que el indicador aparece con `refreshing = true`, que no hay invocación duplicada si ya está en curso y que la acción accesible «Actualizar» invoca `onRefresh` solo cuando no está refrescando.

---

## 3. `PodcastHomeViewModel.refreshManually()`

### Entradas y estado

| Elemento | Tipo | Regla |
|----------|------|-------|
| `refreshing` | `StateFlow<Boolean>` | Estado de la recarga manual |
| `message` | `StateFlow<String?>` | Error accionable pendiente de mostrar |
| `refreshManually()` | `() -> Unit` | Inicia la recarga si no hay otra en vuelo |

### Comportamiento

- Si ya hay una recarga en vuelo, la llamada se ignora.
- Mantiene el `PodcastHomeUi.Content` actual mientras recarga; no publica `Loading`.
- Al terminar con éxito, sustituye el `Home` por el nuevo (incluido el resumen de cola) y publica `refreshing = false`.
- Si el nuevo resultado está vacío, pasa al estado `Empty` existente.
- Con error y contenido previo: conserva el contenido, publica `message` con el error y `refreshing = false`.
- Con error sin contenido previo: mantiene el estado `Error` existente con reintento.
- No modifica el reproductor, la cola, las descargas ni el progreso local, aunque la respuesta ya no incluya el episodio o programa en reproducción: este desaparece de la lista y sigue sonando con su título y posición.

---

## 4. `SubscriptionsViewModel.refreshManually()`

### Entradas y estado

| Elemento | Tipo | Regla |
|----------|------|-------|
| `refreshing` | `StateFlow<Boolean>` | Estado de la recarga manual |
| `message` | `StateFlow<String?>` | Error accionable pendiente de mostrar |
| `refreshManually()` | `() -> Unit` | Inicia la recarga si no hay otra en vuelo |

### Comportamiento

- Si ya hay una recarga en vuelo, la llamada se ignora.
- Mantiene el `SubscriptionsUiState.Content` actual mientras recarga.
- Recarga la lista con `PodcastSubscriptionsRepo.list()`.
- Si `searchUi.showResults` y el ámbito es `EPISODES`, relanza la búsqueda con el término confirmado (`searchUi.submittedTerm`) desde la primera página; el término, el ámbito y `submittedTerm` se conservan y las respuestas tardías se descartan con la generación existente. No se usa el texto escrito sin confirmar.
- Si el ámbito es `PODCASTS`, la lista nueva se filtra localmente con el término vigente al publicarse.
- Con error y contenido previo: conserva el contenido, publica `message` y `refreshing = false`.
- Con error sin contenido previo: mantiene el estado `Error` existente con reintento.
- No modifica el orden manual, el modo de vista, el reproductor, la cola ni las descargas, aunque la respuesta ya no incluya el podcast o episodio en reproducción: este desaparece de la lista y sigue sonando con su título y posición.

---

## 5. Matriz de estados

| Pantalla | Estado previo | Gesto | Resultado |
|----------|---------------|-------|-----------|
| Inicio | `Content` | Arrastre al principio | Indicador; contenido intacto; al terminar, datos nuevos o mensaje de error |
| Inicio | `Loading` | Arrastre | Ignorado; se mantiene la carga inicial |
| Inicio | `Empty` | Arrastre | No exigido; se mantiene el estado vacío con «Descubrir podcasts» |
| Inicio | `Error` | Arrastre | No exigido; se mantiene el error con «Reintentar» |
| Suscripciones | `Content` sin búsqueda | Arrastre al principio | Indicador; lista recargada; orden y modo conservados |
| Suscripciones | `Content` con «Podcasts» y término | Arrastre | Lista recargada y filtro local reaplicado |
| Suscripciones | `Content` con resultados de episodios | Arrastre | Búsqueda confirmada (`submittedTerm`) repetida desde la primera página |
| Suscripciones | `Loading` / `Empty` / `Error` | Arrastre | No exigido; se mantienen los estados y acciones existentes |
| Cualquiera | Recarga en vuelo | Segundo arrastre | Ignorado; no se duplica la consulta |
| Cualquiera | Recarga en vuelo | Error de red | Contenido conservado; mensaje accionable; indicador oculto |
| Cualquiera | Reproducción activa y respuesta sin el elemento en curso | Arrastre o acción «Actualizar» | El elemento desaparece de la lista; la reproducción continúa con título y posición |
| Cualquiera | TalkBack activo | Acción «Actualizar» | Recarga iniciada sin gesto; inicio/fin anunciados y foco conservado |

---

## 6. Fuera de contrato

- Actualización automática periódica o al volver del segundo plano.
- Notificaciones push o sincronización en tiempo real.
- Recarga por arrastre en pantallas distintas de Inicio y Suscripciones.
- Persistencia del estado de recarga o del último error.
- Cambios en la API, los DTOs, la autenticación o el esquema de datos.
