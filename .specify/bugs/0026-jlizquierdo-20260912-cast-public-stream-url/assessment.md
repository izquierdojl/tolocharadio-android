# Bug Assessment: Cast conecta pero no reproduce (receptor recibe 401 del proxy)

- **Slug**: 0026-jlizquierdo-20260912-cast-public-stream-url
- **Created**: 2026-09-12
- **Source**: logcat del dispositivo + prueba `curl` contra el servidor
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> Ya aparecen los dispositivos y se conecta como renderer, pero al reproducir no llega a sonar.

Tras el fix 0025 (mimeType), la app ya no se cierra, pero en Cast no suena nada. Logs del receptor:

```
W/MediaControlChannel: received unexpected error: Invalid Request.
W/MediaQueue: Error fetching queue item ids, statusCode=2001, statusMessage=null
```

Prueba contra el servidor:

```
https://radio.jlizquierdo.com/api/v1/playback/test          -> 401
...?token=abc / ?access_token=abc / ?t=abc / ?api_key=abc    -> 401
{"error":{"code":"UNAUTHORIZED","message":"No autorizado","status":401}}
```

## Symptom

El Cast conecta y el receptor arranca (Default Media Receiver), pero no reproduce. Esperado: la emisora suena en el receptor.

## Reproduction

1. Conectar a un dispositivo Cast desde la app (Android 17).
2. Reproducir cualquier emisora.
3. El receptor arranca pero no suena; el sender registra `MediaControlChannel Invalid Request` / `MediaQueue 2001`.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` — el `MediaItem` de Cast usaba la URL del proxy autenticado.
- `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/api/PlaybackApi.kt:20` — `streamUrl` apunta a `/api/v1/playback/{id}`, que exige `Authorization: Bearer`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerDataSourceFactory.kt:26` — el player **local** sí añade el `Bearer`.

## Root Cause Hypothesis

**Confidence: high**

El proxy `/api/v1/playback/{id}` exige `Authorization: Bearer <token>` (contrato 0024) y **no acepta credenciales por query**. El Chromecast descarga la URL por su cuenta y **no puede enviar cabeceras HTTP**, así que recibe 401; el `LOAD` falla y el receptor responde `INVALID_REQUEST`. La sección 5 del contrato 0024 asumía que "Chromecast reutiliza la misma fuente autenticada", lo cual es técnicamente imposible.

## Proposed Remediation

**Preferred**: para Cast, enviar al receptor la **URL pública de la emisora** (`station.url`) en lugar del proxy autenticado, manteniendo el `mimeType`. La reproducción local sigue usando el proxy.

**Alternativas**:
- URL firmada / token en query en el servidor (requiere cambiar el backend; descartada por el usuario al preferir la URL pública).
- Endpoint de reproducción público (menos seguro).

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactory.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactoryTest.kt`

**Tests to add or update**:
- `castUriFor` usa la URL pública; cae al proxy si la emisora no tiene URL.

## Risks & Considerations

- Algunas emisoras podrían estar restringidas por User-Agent/Referer/geografía; el proxy podría normalizarlo y la URL directa no.
- Emisoras **HTTP en claro**: el receptor podría bloquearlas por contenido mixto.
- Emisoras **HLS**: el receptor pedirá manifiesto/segmentos a la URL pública y puede requerir CORS.
- Los errores de Cast no se muestran en UI (`CastPlayerManager` no implementa `onPlayerError`).

## Open Questions

- [NEEDS CLARIFICATION: ¿alguna emisora del catálogo requiere UA/Referer para su URL pública?] 

## Deuda registrada (gobernanza)

- **Estado**: excepción consciente, no un defecto a corregir a corto plazo. FR-001/FR-010 de
  0021 y FR-004 de 0024 se enmiendan con esta excepción (Cast usa URL pública; el receptor no
  puede enviar `Authorization`). Registrado en
  `specs/0021-jlizquierdo-20260912-proxy-only-playback/contracts/proxy-playback.md` §9.
- **Fecha de revisión**: **2026-10-09** (≤ 2 sprints). Si el servicio ofrece URLs firmadas o
  sesión embebible para el receptor, se reintroduce Cast autenticado y se cierra esta deuda.
