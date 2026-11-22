# Data Model: Tops de podcasts por país y género

**Feature**: `0053-izquierdojl-20261122-podcast-top-charts`  
**Date**: 2026-11-22  
**Plan**: [plan.md](./plan.md) | **Spec**: [spec.md](./spec.md)

## Entidades de dominio

### `TopCountry`

País admitido por el ranking de Apple. Lista fija y ordenada; España es el valor por defecto.

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `id` | `String` | Código ISO 3166-1 alpha-2 en minúsculas; único; dos letras. |
| `label` | `String` | Etiqueta en español para el selector; no vacía. |

Valores (en este orden):

| `id` | `label` |
| --- | --- |
| `es` | España |
| `mx` | México |
| `ar` | Argentina |
| `co` | Colombia |
| `cl` | Chile |
| `pe` | Perú |
| `us` | Estados Unidos |
| `gb` | Reino Unido |
| `fr` | Francia |
| `de` | Alemania |
| `it` | Italia |
| `pt` | Portugal |
| `br` | Brasil |

Reglas:
- `TopCountry.fromId(id)` devuelve el país cuyo `id` coincide; si es `null`, vacío o desconocido, devuelve `SPAIN`.
- El identificador persistido es `id`; la etiqueta no se persiste.

### `TopGenre`

Categoría de Apple Podcasts. Incluye la opción «Todas las categorías» (`ALL`) con `id = null`.

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `id` | `String?` | Identificador numérico de Apple (`^[0-9]{1,6}$`) o `null` para `ALL`; único. |
| `label` | `String` | Etiqueta en español; no vacía. |

Valores (en este orden):

| `id` | `label` |
| --- | --- |
| `null` | Todas las categorías |
| `1301` | Artes |
| `1321` | Negocios |
| `1303` | Comedia |
| `1304` | Educación |
| `1483` | Ficción |
| `1487` | Historia |
| `1512` | Salud y bienestar |
| `1305` | Niños y familia |
| `1502` | Ocio |
| `1310` | Música |
| `1489` | Noticias |
| `1314` | Religión y espiritualidad |
| `1533` | Ciencia |
| `1324` | Sociedad y cultura |
| `1545` | Deportes |
| `1318` | Tecnología |
| `1488` | Crímenes reales |
| `1309` | TV y cine |

Reglas:
- `TopGenre.fromId(id)` devuelve `ALL` para `null`, vacío o desconocido.
- El identificador persistido es `id`; `ALL` se persiste como cadena vacía o ausencia de clave.

### `TopChartsLoad`

Resultado de una carga de ranking ya resuelta por el dominio.

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `items` | `List<PodcastSearchResult>` | Orden de posición; puede estar vacía; sin duplicados por `feedUrl` (el servidor ya los deduplica). |
| `genreUnavailable` | `Boolean` | `true` solo cuando se pidió una categoría, falló y se sirvió el ranking general del país. |

Invariantes:
- `genreUnavailable = false` cuando la selección es `TopGenre.ALL`.
- `genreUnavailable = false` cuando la categoría responde correctamente, aunque la lista esté vacía.
- `genreUnavailable = true` solo si el error de la categoría es recuperable (`Unavailable`/`Unknown`) y el ranking general responde.
- No se aplica fallback ante `Unauthorized` (401/403) ni `NotFound` (404): se propaga el error para que la UI muestre «Editar servidor» o se oculte la sección, respectivamente.
- Si el ranking general también falla, no se construye `TopChartsLoad`: se propaga `ApiResult.Err`.

## Estado de presentación

### `TopChartsUi`

Estado inmutable expuesto por `TopChartsViewModel`.

| Campo | Tipo | Default | Reglas |
| --- | --- | --- | --- |
| `visible` | `Boolean` | `false` | `true` mientras se comprueba la capacidad, está soportada o falló de forma transitoria; `false` solo si la capacidad es definitivamente no soportada (`none`, `podcastindex`, ausente) o el endpoint responde 404. |
| `country` | `TopCountry` | `TopCountry.SPAIN` | Selección vigente; validada por el enum. |
| `genre` | `TopGenre` | `TopGenre.ALL` | Selección vigente; `ALL` = sin filtro. |
| `items` | `List<PodcastSearchResult>` | `emptyList()` | Último ranking de la combinación vigente; orden de posición. |
| `loading` | `Boolean` | `false` | `true` durante la comprobación de capacidad, la primera carga y cada recarga de filtros/reintento. |
| `error` | `String?` | `null` | Mensaje accionable en español de capacidad o ranking; solo si no hay ranking general que mostrar. |
| `isAuthError` | `Boolean` | `false` | `true` solo para `DomainError.Unauthorized`; la acción principal es «Editar servidor», no «Reintentar». |
| `genreUnavailable` | `Boolean` | `false` | Aviso discreto de fallback con acción «Reintentar»; se limpia al cargar otra combinación con éxito. |

Derivados para la UI:
- `showEmpty = visible && !loading && error == null && items.isEmpty()`.
- `showList = visible && !loading && error == null && items.isNotEmpty()`.
- `showError = visible && !loading && error != null`.
- `showAuthAction = showError && isAuthError`.
- `showRetry = showError && !isAuthError`.
- `showGenreNotice = visible && !loading && genreUnavailable`.

### `DiscoverUi` (cambio)

Se añade un campo para decidir qué contenido principal se muestra.

| Campo | Tipo | Default | Reglas |
| --- | --- | --- | --- |
| `submittedQuery` | `String` | `""` | Término de la última búsqueda confirmada. Vacío = se muestra «Populares» si está disponible. |

Reglas:
- `onQueryChange("")` o solo espacios limpia `submittedQuery` (el ranking reaparece).
- `onQueryChange` con texto no cambia `submittedQuery` hasta que se pulsa Buscar.
- `search()` con texto no vacío fija `submittedQuery` al término recortado antes de lanzar la petición.
- `search()` con texto vacío no modifica `submittedQuery` y muestra el aviso existente.

## DTOs y contrato

### `PodcastFeaturesDto` (ampliación)

| Campo | Tipo | Default | Reglas |
| --- | --- | --- | --- |
| `podcastDiscovery` | `String?` | `null` | Valores esperados: `itunes`, `podcastindex`, `none`; ausente = desconocido. |

### `AppConfigDto`

Se añade la derivación:

- `podcastTopChartsEnabled() = features?.podcasts == true && features?.podcastDiscovery == "itunes"`.

### `PodcastCapabilities` (ampliación)

| Campo | Tipo | Default | Reglas |
| --- | --- | --- | --- |
| `topCharts` | `Boolean` | `false` | Deriva de `podcastTopChartsEnabled()`. |

`SystemRepo.podcastCapabilities(forceRefresh: Boolean = false)` cachea la última capacidad correcta durante la sesión para evitar un `/config` extra al abrir Descubrir. `forceRefresh = true` se usa al reintentar una capacidad fallida o al refrescar el modo; un error no se cachea. La caché vive en memoria y se reinicia con el proceso/servidor (la app reinicia al cambiar de servidor).

### Respuesta de `GET /api/v1/podcasts/top`

Se reutiliza `PodcastSearchPage`:

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `items` | `List<PodcastSearchResult>` | Orden de posición; puede ser vacía. |

`PodcastSearchResult` existente: `title`, `author?`, `feedUrl`, `imageUrl?`, `categories[]`, `description?`. `feedUrl` es obligatorio y válido; los elementos sin feed válido no los devuelve el servidor.

## Persistencia local

Claves de `InstancePrefs` (DataStore Preferences):

| Clave | Tipo | Default | Reglas |
| --- | --- | --- | --- |
| `top_charts_country` | `String` | `es` | Se guarda `TopCountry.id`; al leer se valida con `fromId`. |
| `top_charts_genre` | `String` | `""` | Se guarda `TopGenre.id` o cadena vacía para `ALL`; al leer se valida con `fromId`. |

Flows expuestos:
- `topChartsCountry: Flow<TopCountry>` con fallback a `SPAIN`.
- `topChartsGenre: Flow<TopGenre>` con fallback a `ALL`.

Setters:
- `setTopChartsCountry(country: TopCountry)`.
- `setTopChartsGenre(genre: TopGenre)`.

Sin migración de Room ni cambios en `EncryptedSharedPreferences`.

## Transiciones de estado de la sección

```text
[Init]
  │  visible = true; se leen país/género
  ▼
[Loading] ──(capacidad definitiva no soportada / 404)──► [Hidden]
  │  │
  │  └──(error transitorio de capacidad o ranking)──► [Error] ──(retry)──► [Loading]
  │
  └──(éxito)──► [Content] ──(categoría recuperable + general OK)──► [Content + genreUnavailable]
                    │
                    └──(búsqueda confirmada)──► oculta la sección; al vaciar, vuelve el último estado
```

Reglas de transición:
- `Init → Loading` con `visible = true`: se leen país/género persistidos, se comprueba la capacidad y se carga el ranking. Así el estado de carga aparece de inmediato (SC-001).
- `Loading → Hidden` solo si la capacidad es definitivamente no soportada (`none`, `podcastindex`, ausente) o el endpoint responde 404; no se hace ninguna petición de ranking en el primer caso.
- `Loading → Content` con la lista recibida (posiblemente vacía; la UI deriva el estado vacío de `items.isEmpty()`).
- `Loading → Error` si falla la comprobación de capacidad o el ranking por un error transitorio; se conserva la selección para reintentar.
- `Content → Loading` al cambiar país/género o reintentar; se descarta cualquier respuesta de una carga anterior.
- `Content → Content + genreUnavailable` cuando falla la categoría con un error recuperable y el general responde.
- `Error → Loading` al reintentar; si el error era de capacidad, el reintento fuerza la comprobación (`forceRefresh = true`); si era de ranking/categoría, solo vuelve a pedir el ranking.
- La búsqueda confirmada no destruye el estado del ranking: solo lo oculta; al vaciar la búsqueda se reutiliza el último resultado en memoria (no se fuerza una nueva petición).
- Un error `Unauthorized` deja `visible = true`, `isAuthError = true` y la acción «Editar servidor»; no se reintenta la petición.

## Validaciones

- `country`: siempre uno de los 13 IDs; el selector y la persistencia usan el enum. Nunca se envía un valor libre.
- `genre`: `null` (ALL) o uno de los 18 IDs numéricos; nunca se envía un valor libre.
- `limit`: constante 50; dentro del rango 1–100 del contrato.
- `feedUrl`: no vacío; la fila se muestra solo con resultados devueltos por el servidor. No se inventan URLs.
- Preferencia corrupta: fallback silencioso a `SPAIN`/`ALL`.
- Capacidad definitivamente ausente o proveedor distinto: sección oculta, sin petición y sin mensaje de error.
- Fallo transitorio al comprobar la capacidad: `visible = true`, `loading = false`, error con reintento; no se oculta.
- Error de credenciales: `isAuthError = true`, acción «Editar servidor»; no se aplica fallback ni reintento de ranking.

## Relaciones

- `TopChartsUi.country` + `TopChartsUi.genre` forman la clave de selección que se envía al endpoint.
- `TopChartsLoad` alimenta `TopChartsUi.items` y `genreUnavailable`.
- `TopChartsUi.error` + `TopChartsUi.isAuthError` deciden entre «Reintentar» y «Editar servidor».
- `DiscoverUi.submittedQuery` controla la visibilidad de la sección, no su estado interno.
- `PodcastSubscriptionsRepo.subscribedFeedUrls` (ya existente) determina el estado `Suscrito` de cada fila; la acción de alternar sigue en `DiscoverViewModel`.
- `PodcastCapabilities.topCharts` controla `TopChartsUi.visible`.
