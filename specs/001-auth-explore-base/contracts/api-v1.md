# Contrato API v1 (resumen operativo para Android)

Base: `{baseUrl}/api/v1`. Spec completa:
`GET /api/v1/openapi.json` · Swagger: `GET /api/v1/docs`.
Errores siempre: `{error:{code,message,status,details?[{field,message}]}}`.

## Sistema (público)

- `GET /health` → 200 operativo.
- `GET /config` → `{appName, registrationEnabled}`. Si `false`, ocultar
  registro (spec US-3).

## Auth

- `POST /auth/register {email,password(8–72),name?(≤80)}` → 201
  `{user,accessToken,refreshToken}` (+ `Set-Cookie`, ignorada en
  Android); 403 registro cerrado · 409 email en uso · 422 validación.
- `POST /auth/login {email,password}` → 200 idem; 401 credenciales.
- `POST /auth/refresh {refreshToken?}` (también cookie; Android manda
  body) → 200 nuevo par; 401 → logout local + ir a Login.
- `POST /auth/logout {refreshToken?}` → 200 `{ok:true}`; limpiar local
  aunque falle la red.
- `POST /auth/forgot-password {email}` → 200 `{resetToken: String?}`
  (`null` si no existe → mensaje neutro, anti-enumeración).
- `POST /auth/reset-password {token,newPassword(8–72)}` → 200.

## Usuarios (Bearer)

- `GET /users/me` → `{user}`.
- `PATCH /users/me {name?(≤80),theme?(light|dark)}` → `{user}`.
- `PATCH /users/me/password {currentPassword,newPassword}` → `{ok:true}`
  y revoca refresh previos → forzar re-login.

## Catálogo (público)

- `GET /stations?name&country&language&tag&limit(1–100,def 24)&offset
  &unique` → `{items: Station[], pagination{offset,limit,hasMore}}`.
- `GET /stations/{id}` → `Station` · 404 · 503.
- `GET /stations/countries|languages|tags` → `{items: String[]}`.
- `Station = {id,name,url(→ solo proxy),homepage?,favicon?,country?,
  countryCode?,language?,tags[],codec?,bitrate?,isSsl?,lastCheckOk?,
  votes?,clickCount?,isCustom}`.

## Favoritos / Historial / Propias (Bearer, alcance parcial v1)

- `POST /favorites {stationId}` → 201 · `DELETE /favorites/{stationId}`
  → 200. (Lista + `PUT /favorites/order` → siguiente spec.)
- Playback registra historial en servidor (sin pantalla en esta spec).
- `/custom-stations`, `/suggestions` → siguiente spec.

## Reproducción (Bearer)

- `GET /playback/{stationId}/status` → `{id,playable,reason}`.
  Si `false`, mostrar `reason` sin arrancar ExoPlayer.
- `GET /playback/{stationId}` → stream (audio/mpeg, aac, …) con header
  `Authorization: Bearer <access>`; 401 → refresh + 1 reintento;
  404 · 503.

## Servicios Retrofit (a crear en plan)

`SystemApi (health, config) · AuthApi (register, login, refresh, logout,
forgot, reset) · UserApi (me, patchMe, patchPassword) ·
StationsApi (search, detail, countries, languages, tags) ·
FavoritesApi (add, remove) · PlaybackApi (status, streamUrl)`.
DTOs `kotlinx.serialization`, `Result<T>` + `ApiError` tipado.
