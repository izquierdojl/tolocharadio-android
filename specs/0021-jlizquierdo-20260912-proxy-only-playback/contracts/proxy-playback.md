# Contract: Reproducción unificada por el proxy autenticado

**Feature**: `0021-jlizquierdo-20260912-proxy-only-playback` | **Requisitos**: FR-001..FR-014 | **SC**: SC-001..SC-007

Contrato de reproducción del cliente contra el servicio ya actualizado (repo `tolocharadio`, cambio `resolve-playlist-proxy`). El cliente **no** resuelve listas ni reescribe manifiestos.

## 1. Endpoints consumidos

| Endpoint | Auth | Uso en la app |
|----------|------|----------------|
| `GET /api/v1/playback/:id/status` | Bearer | Preestado de disponibilidad (precheck) bloqueante para **todos** los tipos de emisora |
| `GET /api/v1/playback/:id` | Bearer | Stream principal (directo, lista de texto resuelta o manifiesto HLS reescrito) |
| `GET /api/v1/playback/:id/hls?u=&d=&s=` | Bearer | Subrecursos HLS (variantes, segmentos, mapas, claves) — los sigue Media3, no la app |

## 2. Reglas del cliente (MUST)

1. Toda reproducción usa `streamUrl(baseUrl, stationId)` como URI; nunca la `station.url` del proveedor (FR-001).
2. El cliente no descarga, parsea ni resuelve listas (FR-002).
3. El motor se elige por extensión de `station.url` (`.m3u8` → HLS), con URI siempre proxy (FR-003):
   - HLS → `MediaItem.mimeType = MimeTypes.APPLICATION_M3U8` + `HlsMediaSource.Factory(authDataSource)`.
   - resto → `ProgressiveMediaSource.Factory(authDataSource)`.
4. Todas las peticiones (incluidos subrecursos HLS) llevan `Authorization: Bearer`; el token nunca va en la URL (FR-004).
5. El preestado de disponibilidad (precheck) es bloqueante: `playable == false` ⇒ no se arranca el reproductor y se muestra el motivo con reintento (FR-005).
6. Los códigos de error del servicio se traducen a mensajes en español (FR-006, sección 4).

## 3. Respuesta de disponibilidad

```json
{ "id": "<stationId>", "playable": true }
{ "id": "<stationId>", "playable": false, "reason": "PLAYLIST_EMPTY" }
```

- `playable == true` → se procede a `GET /playback/:id`.
- `playable == false` → `Error(station, PlaybackStatusReason(reason))`.

## 4. Errores tipados y mensajes al usuario

Formato del servicio: `{error:{code,message,status,details?}}`.

| Código | Mensaje en la app |
|--------|-------------------|
| `PLAYLIST_EMPTY` | La lista de la emisora no contiene ninguna emisión reproducible. |
| `PLAYLIST_INSECURE_ONLY` | La emisora solo ofrece conexiones no seguras (HTTP), bloqueadas por la app. |
| `PLAYLIST_MALFORMED` | La lista de la emisora no tiene un formato válido. |
| `PLAYLIST_UNREACHABLE` | No se pudo descargar la lista de la emisora. Comprueba tu conexión. |
| `STREAM_UNAVAILABLE` | La emisora no está disponible en este momento. |
| desconocido / `null` | Emisora no disponible. |

Todos los errores conservan botón de reintento y no cierran la app (FR-006, SC-006).

## 5. Autenticación y seguridad

- `AuthDataSourceFactory` fija `Authorization: Bearer <access>` en cada `DataSource` que crea; se usa para stream principal y subrecursos HLS (FR-004).
- El token vive en memoria y se lee con `SessionManager.accessTokenNow()` al crear el datasource; un refresh se refleja en peticiones posteriores.
- La firma `u/d/s` de los subrecursos la genera el servicio (anti-SSRF); el cliente la transporta sin interpretarla.
- HTTPS-only; la app no introduce URLs de proveedores (FR-011).

## 6. Historial (salda issue #4)

- El servicio registra historial al consumir el stream principal de una emisora de lista, igual que en un stream directo (FR-007).
- Las peticiones de subrecursos HLS no generan entradas de historial (responsabilidad del servicio).

## 7. Compatibilidad y límites

- Se asume una instancia actualizada (FR-013); sin fallback ni detección de capacidades.
- Límite conocido: lista de texto cuya primera entrada es un HLS → el servicio la sirve sin reescribir; issue de seguimiento en el backend (FR-014, R7). La app no lo mitiga.

## 8. Mapeo de aceptación

| Escenario (spec) | Verificación |
|------------------|--------------|
| US1-1/2/3 (directas y listas por proxy + historial) | `PlayerViewModelTest` + quickstart §1 |
| US1-4 / US3 (errores accionables) | `PlaybackStatusReasonTest` + quickstart §3 |
| US2 (HLS continuo por proxy) | `StationMediaItemFactoryTest` + quickstart §2 |
| No regresión directa (SC-005) | batería existente de reproducción |
| Retirada de código (SC-007) | sin referencias a parser/fetcher/cola/directo |
