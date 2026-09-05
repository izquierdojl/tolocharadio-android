# Data Model: Auth + Explorar (001-auth-explore-base)

Fuente de verdad del servidor: `GET /api/v1/openapi.json` del repo
`izquierdojl/tolocharadio`. Los DTOs de red espejan esos schemas;
el dominio usa los mismos nombres sin anotaciones Android.

## AppInstance

- `baseUrl: String` — normalizada (trim, sin `/` final, con esquema;
  solo `https`, salvo override local explícito).
- `appName: String`, `registrationEnabled: Boolean` (de `/config`).
- Una activa por dispositivo, en DataStore (`instance_prefs`).

## User

- `id: Int`, `email: String`, `name: String?`,
  `theme: Theme (LIGHT|DARK)`, `createdAt: Long` (ms).
- Validación cliente: email con formato, password 8–72 (registro y
  reset), name ≤ 80.

## AuthSession

- `accessToken: String` (memoria), `refreshToken: String` (cifrado).
- Estado: `Loading | Authenticated(User) | Unauthenticated(reason?)`.
- `POST /auth/*` → `AuthResponse{user, accessToken, refreshToken}`.

## Station

- `id: String (UUID)`, `name: String`, `homepage: String?`,
  `favicon: String?`, `country: String?`, `countryCode: String?`,
  `language: String?`, `tags: List<String>`, `codec: String?`,
  `bitrate: Int?`, `isSsl: Boolean?`, `lastCheckOk: Boolean?`,
  `votes: Int?`, `clickCount: Int?`, `isCustom: Boolean`.
- `url` del servidor **no se modela en UI**: solo el proxy.
- Caché Room `stations_cache` (snapshot + `cachedAt`) para 503/offline.

## StationPage

- `items: List<Station>`,
  `pagination: {offset: Int, limit: Int, hasMore: Boolean}`.
- Query: `name?, country?, language?, tag?, limit=24 (1–100),
  offset=0, unique=false`.

## Favorite (toggle en esta spec)

- `station: Station`, `addedAt: Long`.
- Operaciones: `POST /favorites {stationId}` → 201,
  `DELETE /favorites/{stationId}` → 200. Lista completa y
  `PUT /favorites/order` → siguiente spec.

## PlaybackStatus / PlayerState

- `PlaybackStatus{id, playable: Boolean, reason: String?}`.
- `PlayerState = Idle | Buffering(station) | Playing(station) |
  Paused(station, position?) | Error(station?, message, retryable)`.
- Escuchar por proxy registra historial en servidor automáticamente.

## ApiError (envoltorio único)

- `{error: {code: String, message: String, status: Int,
  details: [{field, message}]?}}` → dominio:
  `Unauthorized | NotFound | Conflict | Validation(details) |
  Unavailable | Unknown`.
