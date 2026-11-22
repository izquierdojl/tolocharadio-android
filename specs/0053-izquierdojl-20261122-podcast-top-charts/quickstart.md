# Quickstart: Tops de podcasts por país y género

**Feature**: `0053-izquierdojl-20261122-podcast-top-charts`  
**Plan**: [plan.md](./plan.md) | **Spec**: [spec.md](./spec.md)

Guía de validación end-to-end. No contiene implementación; los detalles de diseño están en [data-model.md](./data-model.md) y [contracts/](./contracts/).

## Prerrequisitos

- JDK 17 y Android SDK con `compileSdk` 37.
- Emulador o dispositivo con `minSdk` 26.
- Una instancia TolochaRadio con:
  - El backend de la rama `feat/podcast-top-charts` (PR #54) desplegado, con `GET /api/v1/podcasts/top` disponible.
  - `PODCAST_DISCOVERY_PROVIDER=itunes` y podcasts habilitados.
  - Un usuario con credenciales válidas configurado en la app.
- Para validar la ocultación: una segunda instancia con `PODCAST_DISCOVERY_PROVIDER=none` o `podcastindex`.

## Comandos

Windows (PowerShell 7):

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat detekt ktlintCheck lintDebug
.\gradlew.bat connectedDebugAndroidTest   # requiere dispositivo/emulador
```

Linux/macOS:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew detekt ktlintCheck lintDebug
./gradlew connectedDebugAndroidTest
```

## Validación automática

Ejecutar primero los tests focalizados (Red-Green):

```bash
./gradlew testDebugUnitTest \
  --tests "com.izquierdojl.tolocharadio.domain.podcasts.TopChartsTest" \
  --tests "com.izquierdojl.tolocharadio.domain.podcasts.LoadTopChartsUseCaseTest" \
  --tests "com.izquierdojl.tolocharadio.data.remote.dto.PodcastCapabilitiesDtoTest" \
  --tests "com.izquierdojl.tolocharadio.data.repo.PodcastSubscriptionsRepoTest" \
  --tests "com.izquierdojl.tolocharadio.data.local.InstancePrefsTest" \
  --tests "com.izquierdojl.tolocharadio.feature.podcasts.TopChartsViewModelTest" \
  --tests "com.izquierdojl.tolocharadio.feature.podcasts.DiscoverViewModelTest"
```

Resultado esperado: todos verdes. Casos mínimos cubiertos:

| Área | Caso |
| --- | --- |
| Catálogo | 13 países y 18 categorías con IDs únicos; `fromId` desconocido cae a `SPAIN`/`ALL`. |
| Fallback | Categoría OK; categoría falla con error recuperable y general OK → `genreUnavailable = true`; ambos fallan → `ApiResult.Err`. |
| Capacidad | `podcastDiscovery` ausente/`none`/`podcastindex` → `topCharts = false`; `itunes` + podcasts → `true`. |
| Repositorio | `top()` envía país/género/límite y mapea `{items}`; propaga errores. |
| Preferencias | Default España/ALL; round-trip; valor corrupto cae a default. |
| ViewModel | Carga inicial, cambio de filtro, última selección gana, capacidad cacheada, error + reintento, `isAuthError`, visibilidad. |
| Búsqueda | `submittedQuery` se fija al buscar y se limpia al vaciar el campo. |
| Errores | Matriz `Unavailable`/`Unknown`/`Unauthorized`/`NotFound` y fallback solo en errores recuperables. |

Compose UI tests:

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.izquierdojl.tolocharadio.feature.podcasts.discover.TopChartsSectionTest

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.izquierdojl.tolocharadio.feature.podcasts.discover.DiscoverContentTest
```

Deben cubrir: contenido, carga, vacío, error con reintento, aviso de categoría con reintento, «Editar servidor», exclusión ranking/búsqueda y controles no bloqueados.

## Smoke del contrato (manual)

Con una sesión válida, sin pegar tokens en la terminal compartida ni en logs:

```bash
curl -sS \
  -H "Authorization: Bearer $TOLOCHA_TOKEN" \
  "https://TU-INSTANCIA/api/v1/podcasts/top?country=es&limit=5"
```

Comprobar:
- HTTP 200 y `items` con hasta 5 elementos.
- Orden de posición y `feedUrl` presente en cada elemento.
- `country=mx` devuelve México.
- `genre=1303` devuelve la categoría Comedia.
- `country=xyz` devuelve 400 `VALIDATION_ERROR`.
- Sin `Authorization` devuelve 401.

## Validación manual en la app

Abrir **Podcasts → Descubrir** con la caja de búsqueda vacía.

| # | Escenario | Pasos | Resultado esperado |
| --- | --- | --- | --- |
| 1 | Ranking por defecto | Abrir Descubrir | Sección «Populares», país España, género «Todas las categorías», hasta 50 elementos numerados desde 1. |
| 2 | Cambio de país | Elegir México | El ranking se recarga para México; el género se conserva; el selector muestra México. |
| 3 | Cambio de género | Elegir Comedia | El ranking se recarga con `genre=1303`; el país se conserva. |
| 4 | Persistencia | Cerrar la app (force stop), reabrir y volver a Descubrir | Se mantienen México y Comedia. |
| 5 | Búsqueda tiene prioridad | Escribir un término y pulsar Buscar | «Populares» desaparece y se muestran los resultados. |
| 6 | Volver al ranking | Borrar el texto de búsqueda | «Populares» reaparece con México/Comedia y sin recarga forzada. |
| 7 | Suscripción | Pulsar «Suscribirse» en un elemento | El botón pasa a «Suscrito»; el programa aparece en Suscripciones; no se duplica. |
| 8 | Fallback de categoría | Con el ranking general visible, simular un fallo recuperable (503/red) solo en peticiones con `genre` (proxy o mock) y elegir una categoría | Se mantiene el ranking general del país y aparece el aviso «El ranking por categoría no está disponible ahora…» con una acción «Reintentar» que vuelve a pedir la categoría. |
| 9 | Error total | Activar modo avión y pulsar Reintentar | Estado de error con «Reintentar»; búsqueda, RSS y OPML siguen operativos; sin crash. |
| 10 | Sin proveedor Apple | Configurar la instancia con `PODCAST_DISCOVERY_PROVIDER=none` y abrir Descubrir | No aparece «Populares»; no hay peticiones a `/podcasts/top`; búsqueda y RSS funcionan. |
| 11 | Proveedor no soportado | `PODCAST_DISCOVERY_PROVIDER=podcastindex` | Igual que el caso 10. |
| 12 | Vacío | Usar el Compose test con `items = emptyList()` o una combinación sin resultados | «No hay ranking disponible para esta selección.» (no el «Sin resultados» de la búsqueda). |
| 13 | Accesibilidad | Activar TalkBack y tamaño de letra grande | Título, país, categoría, aviso, posición, título/autor y estado de suscripción se anuncian en el orden esperado; los textos largos no desbordan. |
| 14 | Cambios rápidos | Alternar país/género varias veces seguidas | Solo se muestra el ranking de la última selección; no hay parpadeos de resultados antiguos ni crash. |
| 15 | Servidor antiguo | Instancia sin `features.podcastDiscovery` | Sección oculta; sin error visible. |
| 16 | Servidor con capacidad pero sin ruta | Instancia de `main` con `podcastDiscovery=itunes` pero sin el endpoint de la PR #54 | Sección oculta tras el 404; sin error visible ni reintento automático. |
| 17 | Credenciales inválidas | Invalidar la sesión/credenciales y abrir Descubrir | Mensaje de credenciales y acción «Editar servidor»; no se muestra «Reintentar» para el ranking; el botón abre la edición del servidor activo. |
| 18 | Fallo transitorio de capacidad | Hacer que `/api/v1/config` falle (proxy/mock o modo avión al abrir) | La sección muestra error con «Reintentar»; no se oculta como «no soportado»; al recuperar la red, reintentar carga el ranking. |
| 19 | Red lenta y tiempos | Limitar la red (p. ej. 3G lento) y medir con cronómetro | El estado de carga de «Populares» aparece en < 2 s; el ranking de una combinación se actualiza en < 3 s; suscribirse refleja el estado en < 1 s. |

## Criterios de aceptación de la validación

- [ ] `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck` y `lintDebug` en verde.
- [ ] Tests focalizados y Compose UI tests (`TopChartsSectionTest`, `DiscoverContentTest`) en verde.
- [ ] Escenarios 1–19 verificados manualmente; el 8 puede completarse con el test unitario/proxy.
- [ ] Fallback del escenario 8 verificado con test unitario y, si es posible, con proxy/mock.
- [ ] Sin peticiones a `/podcasts/top` cuando la sección está oculta por capacidad definitivamente no soportada.
- [ ] Sin credenciales ni PII en logs.
- [ ] Sin cambios en los flujos existentes de búsqueda, RSS, OPML y suscripciones.

## Troubleshooting

| Síntoma | Causa probable | Acción |
| --- | --- | --- |
| No aparece «Populares» | Servidor sin `podcastDiscovery=itunes`, podcasts desactivados o fallo transitorio de capacidad | Revisar `/api/v1/config`; si el fallo es de red, debe aparecer error con reintento, no ocultarse. |
| Error 401/403 | Sesión caducada o podcasts desactivados | La app renueva/re-loguea; si falla, «Editar servidor» abre la edición del servidor activo. |
| Error 503 | Apple no responde o feed caído | Reintentar; si era una categoría, debe mantenerse el general con aviso y acción «Reintentar». |
| Ranking vacío inesperado | Combinación sin resultados o feed de Apple vacío | Probar otro país/categoría; el estado vacío es válido. |
| La selección no persiste | DataStore no escribió o se borraron datos de la app | Revisar logs de DataStore; repetir el cambio y reabrir. |
| Cambio de filtro muestra datos viejos | Respuesta obsoleta | Debe estar cubierto por el test de última-selección-gana; revisar cancelación/generación. |
