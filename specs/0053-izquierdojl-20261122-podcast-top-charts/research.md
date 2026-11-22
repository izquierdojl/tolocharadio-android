# Research: Tops de podcasts por país y género

**Feature**: `0053-izquierdojl-20261122-podcast-top-charts`  
**Date**: 2026-11-22  
**Sources**: repositorio web `izquierdojl/tolocharadio` (rama `feat/podcast-top-charts`, PR #54), código Android actual, constitución del proyecto y OpenAPI del backend.

## Contexto y hallazgos

- La web incorpora `GET /api/v1/podcasts/top` en la pantalla de búsqueda de podcasts. La sección «Populares» se muestra solo con la caja de búsqueda vacía y `features.podcastDiscovery === "itunes"`.
- El endpoint está autenticado, acepta `country` (ISO de dos letras, `es` por defecto), `genre` (ID numérico de Apple, opcional) y `limit` (1–100, 50 por defecto), y devuelve `{items:[PodcastSearchResult]}` en orden de posición.
- El backend usa el feed nuevo de Apple para el top general por país y el feed antiguo para el filtro por categoría; resuelve los `feedUrl` con `lookup` en lote y cachea por `country:genre:limit` durante ~3 h. Si el feed por categoría falla, la web mantiene el top general y muestra un aviso discreto.
- La app Android ya tiene `PodcastSearchResult`, `PodcastSearchRow`, `SubscribeButton`, `PodcastSubscriptionsRepo`, `SystemRepo`, `InstancePrefs` y el flujo de suscripción. No existe todavía `podcastDiscovery` en `PodcastFeaturesDto` ni un método `top()`.
- `AppConfigDto` usa `ignoreUnknownKeys`; un servidor nuevo no rompe la app, pero la app no conoce aún la capacidad de ranking. Sin el campo, la sección debe ocultarse.
- Los tests del proyecto no usan MockWebServer; mockean interfaces con MockK. Añadirlo sería una dependencia nueva no justificada.

## Decisiones

### D1. Contrato del endpoint y reutilización de DTOs

**Decision**: consumir `GET /api/v1/podcasts/top` con `country`, `genre` y `limit`; reutilizar `PodcastSearchPage` (`{items}`) y `PodcastSearchResult` como modelo de respuesta.

**Rationale**: el servidor devuelve exactamente la misma forma que la búsqueda; un DTO nuevo duplicaría campos y rompería la paridad. El envoltorio `PodcastSearchPage` ya existe y está probado.

**Alternatives considered**:
- Crear `PodcastTopPage`/`PodcastTopItem`: rechazado por YAGNI; no hay campos distintos.
- Resolver el ranking en el cliente contra Apple: rechazado; expondría la app a endpoints no documentados, sin caché ni normalización del backend.

### D2. Detección de capacidad

**Decision**: añadir `podcastDiscovery: String?` a `PodcastFeaturesDto` y `topCharts: Boolean` a `PodcastCapabilities`; `topCharts = podcasts && podcastDiscovery == "itunes"`. `SystemRepo` cachea la capacidad correcta de la sesión (`podcastCapabilities(forceRefresh = false)`) y `TopChartsViewModel` muestra `visible = true`/`loading` de inmediato mientras la comprueba.

**Rationale**: es la misma puerta que usa la web (`provider === "itunes"`). Evita una petición fallida y el parpadeo de una sección que no puede funcionar. Los servidores antiguos, `none` y `podcastindex` quedan ocultos sin error. El `/config` de `main` ya publica `podcastDiscovery` aunque el endpoint solo existe en la PR #54: un `404` al cargar el ranking se interpreta como servidor sin soporte y oculta la sección de forma silenciosa, sin estado de error. Un fallo **transitorio** al comprobar la capacidad (red/503) no se confunde con «no soportado»: se muestra error con reintento. La caché evita el `/config` duplicado que ya hizo `ModeViewModel` y reduce la latencia de SC-001.

**Alternatives considered**:
- Llamar siempre a `/podcasts/top` y ocultar tras un 400/403: rechazado; genera una petición inútil y un estado de error visible en cada apertura.
- Consultar el proveedor en cada pantalla sin caché: rechazado; duplica `/config` y retrasa el estado de carga.
- Exigir un campo nuevo `podcastTopCharts` en `/config`: rechazado; requeriría cambiar el backend fuera del alcance y el 404 ya resuelve la compatibilidad.
- Tratar cualquier `ApiResult.Err` de capacidad como «no soportado»: rechazado; ocultaría la sección en modo offline en vez de ofrecer reintento.

### D3. Ownership del estado

**Decision**: `TopChartsViewModel` dedicado para ranking, filtros y carga; `DiscoverViewModel` conserva búsqueda, RSS, OPML, mensajes y suscripción, y añade `submittedQuery` para decidir qué se muestra.

**Rationale**: mantiene cada ViewModel con una responsabilidad y reutiliza `toggleSubscribe`/`subscribedFeedUrls` sin duplicar la lógica de suscripción. `submittedQuery` replica el `submitted` de la web: el ranking se oculta al confirmar una búsqueda y reaparece al vaciar el campo.

**Alternatives considered**:
- Un único `DiscoverViewModel` con todo: rechazado; mezclaría dos flujos de carga y haría crecer un ViewModel ya usado por búsqueda, RSS y OPML.
- Un `TopChartsViewModel` que también gestione suscripciones: rechazado; duplicaría la lógica y los mensajes de `DiscoverViewModel`.

### D4. Catálogo fijo de países y categorías

**Decision**: enums de dominio `TopCountry` (13) y `TopGenre` (18 + `ALL`) con `id`, etiqueta y `fromId()` tolerante a valores desconocidos.

**Rationale**: la web usa listas fijas; no hay endpoint de catálogo de países/categorías de Apple. Los enums evitan cadenas mágicas, permiten exhaustividad y validan la persistencia. `TopGenre.ALL` con `id = null` simplifica el caso «Todas las categorías».

**Alternatives considered**:
- Listas en la capa UI: rechazado; la validación y el fallback son reglas de dominio y deben poder testearse sin Compose.
- Consultar Apple desde el cliente: rechazado; sin contrato estable, sin caché y con riesgo de CORS/red.

### D5. Persistencia de la selección

**Decision**: dos claves en `InstancePrefs` (`top_charts_country`, `top_charts_genre`) que guardan IDs; los flows exponen `TopCountry`/`TopGenre` validados y los setters persisten.

**Rationale**: DataStore es el mecanismo aprobado para ajustes y ya se usa para modo de vista, tema y arranque. Guardar IDs es estable ante cambios de etiqueta. Los valores corruptos caen a `SPAIN`/`ALL` sin error.

**Alternatives considered**:
- `SavedStateHandle`: rechazado; se pierde al morir el proceso y no cumple «recordar entre sesiones».
- Perfil del servidor: rechazado; no hay endpoint y la spec acota la preferencia al dispositivo.

### D6. Fallback por categoría

**Decision**: `LoadTopChartsUseCase` encapsula la regla: sin género → general; con género → filtrado y, si el error es recuperable (`Unavailable`/`Unknown`), general con `genreUnavailable = true`; si el general también falla → error. No se aplica fallback a `Unauthorized` (401/403) ni `NotFound` (404): se propaga para que la UI muestre «Editar servidor» o se oculte. La UI muestra el aviso con una acción «Reintentar» que vuelve a pedir la categoría.

**Rationale**: replica el comportamiento de la web y evita que un fallo del feed antiguo de Apple deje la sección vacía. La regla es lógica de negocio pura y se puede testear con un repositorio mockeado. Excluir auth/404 evita reintentos de autenticación duplicados y llamadas inútiles cuando la ruta no existe.

**Alternatives considered**:
- Dejar la sección vacía si falla la categoría: rechazado; peor experiencia y no es paridad.
- Servir datos caducados de otra combinación: rechazado; el backend no los expone y confundiría al usuario.
- Fallback ante cualquier error, como la web: rechazado; duplicaría el flujo de credenciales y ocultaría un 404 de ruta inexistente.

### D7. Concurrencia y última selección gana

**Decision**: cancelar el job de carga anterior y usar una generación antes de publicar el resultado; cambios rápidos de país/género solo aplican la última selección.

**Rationale**: el patrón ya existe en `SubscriptionsViewModel` (job + generación) y evita que una respuesta lenta pise una selección más reciente.

**Alternatives considered**:
- Solo cancelar el job: suficiente en la mayoría de casos, pero una respuesta ya en curso puede publicar si no se comprueba la generación.
- Serializar todas las peticiones: rechazado; añade latencia y complejidad innecesaria.

### D8. Composición de la UI

**Decision**: `TopChartsSection` sin estado, con título «Populares», selectores M3 (`OutlinedButton` + `DropdownMenu`), aviso de categoría con acción «Reintentar», estados de carga/error/vacío, acción «Editar servidor» para credenciales y `LazyColumn` con posición. `PodcastSearchRow` gana `position: Int? = null`. `DiscoverScreen` delega en un `DiscoverContent` sin estado para poder testear la exclusión ranking/búsqueda y los controles; `onEditServer` se cablea desde `TolochaNavGraph`.

**Rationale**: reutiliza la fila y el botón de suscripción, mantiene la sección y la pantalla testeables sin Hilt y evita scrolls anidados. La posición se muestra como número a la izquierda, como la web. El host sin estado permite cubrir con Compose los flujos críticos de búsqueda/ranking y error de credenciales.

**Alternatives considered**:
- Tarjeta nueva solo para rankings: rechazado; duplicaría la fila y el estado de suscripción.
- Sección dentro de la lista de resultados: rechazado; los estados son mutuamente excluyentes y complicaría el scroll.
- Probar solo la sección y dejar la pantalla a validación manual: rechazado; no cubre la exclusión ranking/búsqueda ni los controles no bloqueados.

### D9. Límite, paginación y caché

**Decision**: una petición de 50 elementos, sin paginación ni scroll infinito; el ViewModel conserva el último resultado en memoria; el servidor cachea ~3 h.

**Rationale**: paridad con la web y con la spec. El ranking es una lista corta y estable; paginar añadiría estado y complejidad sin valor.

**Alternatives considered**:
- Carga incremental: rechazada por YAGNI y porque la web no la ofrece.
- Caché local persistente del ranking: rechazada; el servidor ya cachea y los rankings cambian; no hay requisito offline de ranking.

### D10. Estrategia de tests

**Decision**: tests unitarios de dominio (catálogo, defaults, fallback, orden, exclusiones 401/403/404), DTO de capacidad y caché, repositorio, preferencias y ViewModels; Compose UI tests de los estados de `TopChartsSection` y de `DiscoverContent` (ranking/búsqueda, controles no bloqueados, «Editar servidor»); matriz de errores 400/401/403/503 y red lenta en quickstart. Sin MockWebServer.

**Rationale**: cumple la regla Red-Green y los gates sin dependencias nuevas. MockK sobre `PodcastsApi`/repositorios es el patrón existente (`PodcastSubscriptionsRepoTest`, `DiscoverViewModelTest`). El test de `DiscoverContent` cubre el flujo de pantalla que no se puede probar con `DiscoverScreen` por Hilt.

**Alternatives considered**:
- MockWebServer: rechazado; dependencia nueva no justificada y el contrato ya está cubierto con mocks.
- Solo tests manuales: rechazado; la constitución exige tests de dominio/data/ViewModel y Compose para flujos críticos.

### D11. Errores de credenciales y acción de servidor

**Decision**: `TopChartsUi` expone `isAuthError`; un `DomainError.Unauthorized` muestra el mensaje de credenciales y una acción «Editar servidor» cableada desde `TolochaNavGraph` (`editActiveServer`). No se ofrece «Reintentar» en ese estado.

**Rationale**: la constitución (Principio IV) exige que el reintento de credenciales abra la edición del servidor activo; es el patrón ya usado en Estadísticas, Favoritos e Historial (`isAuthError` + `onEditServer`). El flujo de refresh/re-login sigue actuando antes de mostrar el error.

**Alternatives considered**:
- Mostrar «Reintentar» también para credenciales: rechazado; viola la constitución y repite una petición que fallará.
- Ocultar la sección ante credenciales: rechazado; el usuario no sabría cómo corregir la sesión.

### D12. Caché de capacidad y latencia

**Decision**: `SystemRepo.podcastCapabilities(forceRefresh = false)` cachea en memoria la última capacidad correcta de la sesión; `TopChartsViewModel` pone `visible=true` y `loading=true` antes de comprobarla, y el reintento de capacidad usa `forceRefresh=true` (el de ranking/categoría solo repite la petición). El estado de carga aparece de inmediato y el `/config` no se repite si `ModeViewModel` ya lo obtuvo.

**Rationale**: SC-001 exige ver datos o carga en <2 s; dos peticiones secuenciales (`/config` + `/podcasts/top`) podían dejar la sección en blanco. La caché es de sesión y se reinicia con el proceso/servidor, por lo que no añade invalidación compleja. Un error no se cachea.

**Alternatives considered**:
- No cachear y aceptar el `/config` extra: rechazado; riesgo de superar SC-001 y llamada duplicada.
- Cachear con TTL complejo: rechazado por YAGNI; la sesión y el reinicio al cambiar de servidor son suficientes.

## Incógnitas resueltas

| Incógnita | Resolución |
| --- | --- |
| ¿Endpoint exacto y parámetros? | `GET /api/v1/podcasts/top` con `country`, `genre`, `limit`; ver contrato. |
| ¿Cómo saber si el servidor lo soporta? | `features.podcastDiscovery == "itunes"` y `features.podcasts == true`. |
| ¿Qué mostrar si falla la categoría? | Si el error es recuperable: ranking general + aviso con acción «Reintentar» que repite la categoría. |
| ¿Dónde persistir país/género? | DataStore (`InstancePrefs`), IDs validados por enums de dominio. |
| ¿Cuántos elementos y paginación? | 50, sin paginación. |
| ¿Qué pasa al buscar? | La sección se oculta; al vaciar la búsqueda reaparece con la selección previa. |
| ¿Qué pasa con servidores antiguos o `podcastindex`? | Sección oculta, sin petición y sin error. |
| ¿Qué pasa si falla la comprobación de capacidad? | Error con reintento; no se oculta como «no soportado». |
| ¿Qué pasa con credenciales inválidas? | `isAuthError`, mensaje y acción «Editar servidor»; no se reintenta el ranking. |
| ¿Cómo evitar respuestas obsoletas? | Cancelación del job + generación. |
| ¿Cómo evitar el `/config` duplicado y la latencia? | Caché de capacidad en `SystemRepo` + `visible/loading` inmediatos. |
| ¿Hacen falta dependencias nuevas? | No. |

No quedan incógnitas pendientes.
