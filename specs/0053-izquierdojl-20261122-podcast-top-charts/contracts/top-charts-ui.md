# Contract: sección «Populares» (UI)

**Feature**: `0053-izquierdojl-20261122-podcast-top-charts`  
**Tipo**: contrato de composición y estado para `TopChartsSection`  
**Consumidor**: `DiscoverScreen`

## Visibilidad

La sección se compone solo si se cumplen las dos condiciones:

1. `DiscoverUi.submittedQuery` está vacío (no hay búsqueda confirmada).
2. `TopChartsUi.visible == true`. `visible` es `true` mientras se comprueba la capacidad, está soportada o falló de forma transitoria; es `false` solo si la capacidad es definitivamente no soportada (`none`, `podcastindex`, ausente) o el endpoint responde 404.

Si la visibilidad es falsa, la sección no se compone, no se muestra y no se solicita ningún ranking adicional. Si la comprobación de capacidad falla de forma transitoria, la sección sí se compone y muestra error con reintento.

## Entradas

| Entrada | Tipo | Reglas |
| --- | --- | --- |
| `ui` | `TopChartsUi` | Estado completo de la sección. |
| `subscribedFeedUrls` | `Set<String>` | Feeds suscritos; determina el texto `Suscrito`/`Suscribirse`. |
| `onCountryChange` | `(TopCountry) -> Unit` | Persiste y recarga. |
| `onGenreChange` | `(TopGenre) -> Unit` | Persiste y recarga. |
| `onToggleSubscribe` | `(PodcastSearchResult) -> Unit` | Delega en el flujo existente de Descubrir. |
| `onRetry` | () -> Unit | Reintenta la combinación vigente (capacidad, ranking o categoría). |
| `onEditServer` | () -> Unit | Abre la edición del servidor activo; solo se usa si `ui.isAuthError`. |

## Estados renderizados

| Estado | Condición | Contenido |
| --- | --- | --- |
| Cargando | `ui.loading` | Indicador + texto «Cargando ranking…». Cubre la comprobación de capacidad y la carga del ranking. |
| Contenido | `!loading && error == null && items.isNotEmpty()` | Lista numerada desde 1 con portada, título, autor, categorías y acción de suscripción. |
| Vacío | `!loading && error == null && items.isEmpty()` | Mensaje «No hay ranking disponible para esta selección.». |
| Error de red/ranking | `!loading && error != null && !isAuthError` | Mensaje de error + botón «Reintentar». |
| Error de credenciales | `!loading && error != null && isAuthError` | Mensaje de credenciales + botón «Editar servidor»; no se muestra «Reintentar». |
| Aviso de categoría | `!loading && genreUnavailable` | Texto discreto «El ranking por categoría no está disponible ahora. Mostrando el ranking general.» + acción «Reintentar» que vuelve a pedir la categoría. Debe convivir con el contenido general. |

Reglas:
- El aviso de categoría no sustituye al ranking general; se muestra encima de la lista y su acción usa `onRetry`.
- El error total no debe mostrar el estado vacío ni una lista vacía.
- Un error de credenciales no debe ofrecer «Reintentar»: la acción principal es «Editar servidor».
- El estado de carga no debe bloquear los controles de búsqueda, RSS u OPML de la pantalla.

## Cabecera y selectores

- Título: «Populares».
- Selector de país: etiqueta accesible «País del ranking»; valor visible = `TopCountry.label`; 13 opciones en el orden del data model.
- Selector de género: etiqueta accesible «Categoría del ranking»; valor visible = `TopGenre.label`; «Todas las categorías» + 18 opciones.
- Cambiar un selector actualiza el valor visible de inmediato y dispara una recarga.
- Los selectores siguen operativos durante la carga; un cambio rápido cancela la carga anterior.
- Los menús deben ser desplazables si no caben en pantalla.

## Fila de ranking

Reutiliza `PodcastSearchRow` con `position`:

| Elemento | Regla |
| --- | --- |
| Posición | Número `index + 1`, alineado a la izquierda, no anunciado como parte del título. |
| Portada | Coil; sin contenido semántico si es decorativa. |
| Título | Máximo 1 línea con elipsis. |
| Autor/categoría | Máximo 2 líneas con elipsis; tolera nulos. |
| Acción | `Suscrito` si `feedUrl ∈ subscribedFeedUrls`; `Suscribirse` en caso contrario. |
| Accesibilidad | La fila debe anunciar título, autor y estado del botón; el número de posición puede ir en la descripción semántica de la fila. |

## Accesibilidad

- Los estados de carga, error, vacío y aviso de categoría deben anunciarse con `liveRegion = LiveRegionMode.Polite` cuando cambian.
- El botón de reintento debe tener texto visible «Reintentar»; el de credenciales, «Editar servidor».
- Los selectores deben exponer `contentDescription`/etiqueta y `stateDescription` con el valor actual.
- La sección debe funcionar con tamaños de letra grandes y modo oscuro; los textos largos usan elipsis.
- El orden de foco debe ser: título, país, categoría, aviso (si existe), lista, acción de reintento o «Editar servidor» (si existe).

## Textos exactos

| Uso | Texto |
| --- | --- |
| Título | `Populares` |
| País por defecto | `España` |
| Género por defecto | `Todas las categorías` |
| Cargando | `Cargando ranking…` |
| Vacío | `No hay ranking disponible para esta selección.` |
| Aviso de categoría | `El ranking por categoría no está disponible ahora. Mostrando el ranking general.` |
| Reintentar | `Reintentar` |
| Editar servidor | `Editar servidor` |
| Suscribirse | `Suscribirse` |
| Suscrito | `Suscrito` |

## Test tags

Añadir a `TolochaTestTags`:

| Tag | Elemento |
| --- | --- |
| `TOP_CHARTS_SECTION` | Contenedor de la sección. |
| `TOP_CHARTS_COUNTRY` | Selector de país. |
| `TOP_CHARTS_GENRE` | Selector de categoría. |
| `TOP_CHARTS_LIST` | Lista de resultados. |
| `TOP_CHARTS_STATUS` | Estado anunciado (carga/error/vacío/aviso). |
| `TOP_CHARTS_RETRY` | Botón de reintento. |
| `TOP_CHARTS_AUTH_ACTION` | Botón «Editar servidor» para error de credenciales. |
| `TOP_CHARTS_NOTICE` | Aviso de fallback de categoría con su acción de reintento. |
| `TOP_CHARTS_EMPTY` | Estado vacío. |

## Composición de pantalla (`DiscoverContent`)

Para poder probar la pantalla sin Hilt, `DiscoverScreen` delega en un composable sin estado `DiscoverContent`:

| Entrada | Regla |
| --- | --- |
| `discoverUi` | `DiscoverUi` (búsqueda, resultados, `submittedQuery`, mensaje). |
| `topChartsUi` | `TopChartsUi` de la sección. |
| `subscribedFeedUrls` | Set compartido con la búsqueda y el ranking. |
| `onToggleSubscribe`, `onRetry`, `onEditServer`, `onCountryChange`, `onGenreChange` | Callbacks delegados a los ViewModels. |

Reglas:
- Con `submittedQuery` vacío y `topChartsUi.visible`, muestra «Populares».
- Con `submittedQuery` no vacío, muestra carga/resultados/vacío de búsqueda.
- Los controles de búsqueda, RSS y OPML están siempre presentes y operativos, también durante carga/error del ranking.
- `onEditServer` se cablea desde `TolochaNavGraph` al servidor activo.
