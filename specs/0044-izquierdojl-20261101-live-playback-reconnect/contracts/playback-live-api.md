# Contract: API de reproducción continua

**Feature**: `0044-izquierdojl-20261101-live-playback-reconnect`
**Scope**: consumo cliente de `GET /api/v1/playback/{stationId}/live`, ruta de
reserva y precheck de disponibilidad.

## Endpoints

| Método | Ruta | Uso | Autenticación |
|---|---|---|---|
| `GET` | `/api/v1/playback/{stationId}/live?r={sequence}` | Flujo continuo principal | `Authorization: Bearer <access>` |
| `GET` | `/api/v1/playback/{stationId}?r={sequence}` | Ruta de reserva (manifiesto o progresivo) | `Authorization: Bearer <access>` |
| `GET` | `/api/v1/playback/{stationId}/status` | Precheck de disponibilidad | `Authorization: Bearer <access>` |
| `GET` | `/api/v1/playback/{stationId}/hls/...` | Subrecursos HLS firmados de la ruta de reserva | `Authorization: Bearer <access>` |

`stationId` es un UUID de radio-browser o `custom:<hex>`. La app no envía
credenciales de usuario en la URL; el Bearer lo añade el datasource del player o
el cliente Retrofit. `r` es un entero monótono que el servidor ignora y que solo
sirve para evitar respuestas cacheadas por intermediarios.

## Respuesta del flujo continuo

```http
HTTP/1.1 200 OK
Content-Type: audio/aac
Cache-Control: no-store
Transfer-Encoding: chunked
```

| Aspecto | Contrato |
|---|---|
| `Content-Type` | `audio/aac`, `audio/mpeg` o `audio/aacp` |
| Longitud | Puede no incluir `Content-Length`; el cliente no la exige |
| Cuerpo | Audio continuo, sin manifiesto HLS |
| Caché | `no-store`; el cliente añade `?r=` de todas formas |
| Cierre | Puede cerrar la conexión; el cliente reconecta |

La app MUST tratar la respuesta como audio progresivo. No MUST interpretar el
cuerpo como `.m3u8` ni pedir subrecursos HLS para esta ruta.

## Errores y decisión del cliente

| HTTP | Código de error | Ruta activa | Decisión |
|---:|---|---|---|
| 401 | — | Cualquiera | Renovar token; si falla, `EDIT_SERVER` |
| 403 | — | Cualquiera | Igual que 401 |
| 404 | — | `CONTINUOUS` | Fallback único a `LEGACY` (compatibilidad con servidor antiguo) |
| 405 | — | `CONTINUOUS` | Fallback único a `LEGACY` |
| 404 | — | `LEGACY` | Emisora no disponible; reintentos acotados y error accionable |
| 415 | `HLS_UNSUPPORTED_CONTAINER` | `CONTINUOUS` | Fallback único a `LEGACY`; si falla por formato, mensaje y fin |
| 415 | `HLS_UNSUPPORTED_CONTAINER` | `LEGACY` | Mensaje «Esta emisora usa un formato que este reproductor no admite»; sin bucle |
| 503 | `STREAM_UNAVAILABLE`, `PLAYLIST_*` | Cualquiera | Reintentos acotados; error accionable al agotar |
| 5xx | — | Cualquiera | Reintentos acotados; error accionable al agotar |
| — | Error de red/timeout | Cualquiera | Reintentos acotados; sin red, esperar conectividad |

El 415 no se modela como `reason` de `/status`; la app lo traduce con
`PlaybackMessages` al aplicar el fallback.

**Reglas**:
- El fallback `CONTINUOUS → LEGACY` se intenta **una sola vez** por sesión de
  reproducción.
- Un 415 en `LEGACY` es determinista: no se reintenta en bucle.
- Un 404/405 en `CONTINUOUS` no se muestra al usuario si `LEGACY` funciona.
- Un 401/403 no consume el presupuesto de reintentos hasta que la sesión esté
  renovada; si la renovación falla, se detiene.
- El resultado 415 no se persiste entre reproducciones.

## Precheck de disponibilidad

`GET /api/v1/playback/{stationId}/status` devuelve:

```json
{ "id": "uuid-o-custom", "playable": true }
```

o

```json
{ "id": "uuid-o-custom", "playable": false, "reason": "STREAM_UNAVAILABLE" }
```

| Resultado | Cliente |
|---|---|
| `playable=true` | Reproducir |
| `playable=false` | Mostrar `reason` traducido; no arrancar el player |
| Timeout (>2 s) | Intentar la reproducción; mantener `CONNECTING` |
| Error de transporte | Intentar la reproducción; mantener `CONNECTING` |

El precheck no bloquea la interfaz más de 2 segundos y se cancela al cambiar de
emisora. Los motivos desconocidos usan un mensaje genérico en español.

## Marcador anticaché

- La primera conexión usa `r=0`; cada conexión posterior incrementa el valor.
- El valor es único aunque el usuario pulse "Reintentar" o se reinicie el
  presupuesto de intentos.
- No contiene token, stationId, correo ni ningún dato personal.
- El servidor lo ignora; no cambia el contrato de autenticación.

## Compatibilidad

- Un servidor anterior a 0.29.0 que no ofrece `/live` responde 404/405 y la app
  usa `LEGACY` sin mostrar error.
- Las rutas antiguas y sus subrecursos HLS no cambian.
- Chromecast sigue usando la URL pública de la emisora; no consume este
  contrato.
- No se añaden endpoints, cabeceras ni requisitos de autenticación.

## Trazabilidad

Cubre FR-001..FR-008, FR-016..FR-019, FR-023..FR-024; SC-001..SC-007.
