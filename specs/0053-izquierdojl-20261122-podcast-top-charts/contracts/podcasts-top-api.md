# Contract: `GET /api/v1/podcasts/top`

**Feature**: `0053-izquierdojl-20261122-podcast-top-charts`  
**Type**: API HTTP consumida por la app Android  
**Source of truth**: PR #54 del repositorio `izquierdojl/tolocharadio` (rama `feat/podcast-top-charts`)

## Request

```http
GET /api/v1/podcasts/top?country=es&genre=1303&limit=50
Authorization: Bearer <access-token>
Accept: application/json
```

| Parámetro | Ubicación | Obligatorio | Tipo | Default | Reglas |
| --- | --- | --- | --- | --- | --- |
| `country` | query | No | string | `es` | ISO 3166-1 alpha-2; el servidor normaliza a minúsculas y valida `^[a-z]{2}$`. |
| `genre` | query | No | string | ausente | Identificador numérico de Apple (`^[0-9]{1,6}$`); ausente = ranking general. |
| `limit` | query | No | integer | `50` | 1–100; el servidor recorta al rango. |

Reglas del cliente:
- Enviar siempre `country` y `limit=50`.
- Enviar `genre` solo cuando la selección no sea «Todas las categorías».
- No enviar valores libres: la app usa los 13 IDs y 18 IDs de `TopCountry`/`TopGenre`.
- La sesión y el `Authorization` los añade el interceptor existente; la app no gestiona tokens aquí.

## Response 200

```json
{
  "items": [
    {
      "title": "Entiende tu mente",
      "author": "Entiende tu mente",
      "feedUrl": "https://www.ivoox.com/feed_fg_f1123456_filtro_1.xml",
      "imageUrl": "https://img.ivoox.com/programa/...jpg",
      "categories": ["Salud y bienestar", "Sociedad y cultura"],
      "description": "Podcast de psicología y crecimiento personal."
    }
  ]
}
```

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `items` | array | Ordenado por posición; puede ser vacío; máximo `limit`. |
| `items[].title` | string | No vacío. |
| `items[].author` | string \| null | Puede faltar. |
| `items[].feedUrl` | string | URL de feed válida; el servidor omite elementos sin feed resoluble. |
| `items[].imageUrl` | string \| null | Puede faltar. |
| `items[].categories` | string[] | Puede ser vacío; el servidor normaliza. |
| `items[].description` | string \| null | Puede faltar. |

Notas de contrato:
- El formato es idéntico a `GET /api/v1/podcasts/search`; el cliente reutiliza `PodcastSearchPage`/`PodcastSearchResult`.
- El servidor cachea por `country:genre:limit` durante ~3 h. La app no depende de la frescura exacta.
- El ranking solo está disponible con `PODCAST_DISCOVERY_PROVIDER=itunes`.
- No hay paginación: una sola respuesta de hasta 100 elementos; la app pide 50.

## Errores

Formato estándar del backend:

```json
{
  "error": {
    "code": "DISCOVERY_UNAVAILABLE",
    "message": "El directorio de podcasts no esta disponible en este momento",
    "status": 503
  }
}
```

| HTTP | `code` | Cuándo | Tratamiento en la app |
| --- | --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `country`/`genre`/`limit` inválidos | Mensaje de validación; no ocurre con los enums. |
| 400 | `DISCOVERY_DISABLED` | Descubrimiento externo deshabilitado | La sección no debería estar visible; si ocurre, error controlado. |
| 400 | `DISCOVERY_UNSUPPORTED` | Proveedor distinto de Apple | La sección no debería estar visible; si ocurre, error controlado. |
| 401 | `UNAUTHORIZED` | Sin sesión o token inválido | Renovación/re-login automático; si falla, mensaje de credenciales y acción «Editar servidor». |
| 403 | `PODCASTS_DISABLED` | Podcasts deshabilitados | La pantalla no debería estar accesible; se muestra «Editar servidor» si la sesión es la causa. |
| 404 | `NOT_FOUND` | Servidor de `main` que publica `podcastDiscovery` pero aún no tiene la ruta | Se oculta la sección sin error; degradación silenciosa. |
| 503 | `DISCOVERY_UNAVAILABLE` | Apple no responde o feed caído | Error con reintento; si era una categoría, fallback al general si el error es recuperable. |

## Semántica de fallback

- Sin `genre`: si falla, no hay fallback; error con reintento.
- Con `genre`: si el error es recuperable (`Unavailable`/`Unknown`), el cliente solicita el mismo `country` sin `genre`. Si el general responde, muestra ese ranking con `genreUnavailable = true`, un aviso discreto y una acción «Reintentar» que vuelve a pedir la categoría. Si el general también falla, muestra error con reintento.
- El fallback NO se aplica a `Unauthorized` (401/403) ni a `NotFound` (404): se propaga el error para que la UI muestre «Editar servidor» o se oculte la sección, respectivamente. Evita reintentos de autenticación duplicados.
- El reintento de una categoría vuelve a pedir la combinación filtrada; no se queda en el ranking general.
- Si falla la comprobación de capacidad (`/config`) por un error transitorio, la app muestra un error con reintento; no la interpreta como «no soportado». Un 404 del endpoint sí se interpreta como no soportado y oculta la sección.

## Compatibilidad

- Servidores antiguos sin `/podcasts/top` no deben recibir la petición: la app consulta `features.podcastDiscovery` y oculta la sección.
- `podcastDiscovery` ausente o `null` se interpreta como no soportado.
- `podcastDiscovery = "podcastindex"` o `"none"` se interpreta como no soportado.
- El endpoint requiere `features.podcasts = true`.
- Si el servidor publica `podcastDiscovery = "itunes"` pero la ruta no existe (por ejemplo, `main` antes de desplegar la PR #54), el `404` se trata como no soportado: la sección se oculta sin mensaje de error. No se reintenta automáticamente.
