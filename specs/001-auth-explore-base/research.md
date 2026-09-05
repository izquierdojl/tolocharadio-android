# Research: Auth + Explorar + base app (001-auth-explore-base)

**Fecha**: 2026-09-04 · **Spec**: `specs/001-auth-explore-base/spec.md`

## R1. Dónde guardar cada token

- **Decision**: access JWT solo en memoria (`SessionManager` con
  `StateFlow`, nunca en disco); refresh en `EncryptedSharedPreferences`
  (AndroidX Security Crypto) con fallback a DataStore cifrado.
- **Rationale**: el access caduca en 15 min y su robo en disco no aporta
  nada; el refresh rotatorio es el secreto de larga vida y MUST ir
  cifrado (constitución II + Technology Stack).
- **Alternativa rechazada**: guardar ambos en DataStore plano — expone
  el refresh en claro en backups/root.

## R2. Refresh transparente ante 401

- **Decision**: OkHttp `Authenticator` propio (`TokenAuthenticator`):
  ante 401, `POST /auth/refresh` con el refresh guardado (mutex para
  una sola renovación concurrente), reintento único de la llamada
  original; si falla → `SessionManager.logout()` + evento
  `SessionExpired` que la navegación observa para ir a Login.
- **Rationale**: evita que cada repositorio gestione 401 a mano y
  cumple FR-003/FR-011.
- **Alternativa rechazada**: interceptor que adjunta token y refresca
  pre-emptivamente — más llamadas y condiciones de carrera.

## R3. Playback autenticado en Media3 (web usa cookies, Android no)

- **Decision**: `AuthenticatedDataSourceFactory` que envuelve
  `DefaultHttpDataSource.Factory` e inyecta
  `Authorization: Bearer <access>` por petición a
  `GET /playback/{stationId}`. Precheck previo con
  `GET /playback/{stationId}/status` (`{id,playable,reason}`).
  Servicio `RadioPlaybackService : MediaSessionService` + ExoPlayer.
- **Rationale**: `<audio>` web se autentica con cookie httpOnly
  `tolocha-access`; en Android el equivalente nativo es header Bearer
  (constitución II). Prohibido token en query (FR-007).
- **Alternativa rechazada**: pasar la cookie al datasource — frágil,
  expone CSRF y no rota bien con el access en memoria.

## R4. baseUrl configurable (self-hosted)

- **Decision**: `BuildConfig.TOLOCHA_BASE_URL` como default +
  pantalla de onboarding/ajustes para editarla. Validación con
  `GET /health` y `GET /config` antes de guardar. Cambio de URL →
  logout + limpieza de tokens y caché Room de la instancia anterior.
- **Rationale**: FR-002; cada usuario apunta a su instancia.
- **Alternativa rechazada**: URL fija compilada — rompe el concepto
  self-hosted del backend.

## R5. Paginación y filtros de Explorar

- **Decision**: `PagingSource` manual simple (no Paging3 en v1 para
  reducir riesgo): estado `offset/limit/hasMore` en el ViewModel,
  `limit=24` default, clamp 1–100. Filtros con
  `GET /stations/countries|languages|tags` precargados una vez por
  sesión y cacheados en Room 24 h.
- **Rationale**: el contrato devuelve `pagination{offset,limit,hasMore}`;
  Paging3 añade complejidad sin beneficio a este tamaño (principio V).
- **Alternativa rechazada**: Paging3 — se reevaluará si Favoritos/
  Historial exigen listas gigantes.

## R6. UI Pocket Casts en M3 con mínimo riesgo

- **Decision**: `NavigationBar` inferior (Home/Explorar/Perfil en esta
  spec), `StationCard` + `StationListItem` con Coil (placeholder con
  inicial si `favicon==null`), toggle lista/grid, `EmptyState`,
  `FavoriteButton`, mini-player `BottomBar` persistente + full-player
  `ModalBottomSheet`. Tema oscuro Tolocha (verde-bosque `#1B4332`/ocre
  `#DDA15C` aprox.) por defecto, `theme` del perfil (`light|dark`)
  lo gobierna.
- **Rationale**: paridad web + dirección aprobada; todo con componentes
  M3 estándar, sin diseño custom costoso.

## R7. Versiones objetivo (verificado en implement: AGP 9.3.2 + Gradle 9.5)

AGP 9.3.2 + `compileSdk/target 37`, `minSdk 26`, Java 17.
OJO AGP 9: trae Kotlin integrado y nuevo DSL. Como KSP/Hilt aún lo
exigen, se usa `android.builtInKotlin=false` + `android.newDsl=false`
+ plugin `kotlin.android` clásico (deuda: migrar a built-in Kotlin
cuando KSP lo soporte).
Kotlin 2.3.10 (+ plugin Compose, sin `kotlinCompilerExtensionVersion`),
KSP 2.3.11, Hilt 2.60.1, Compose BOM 2025.01.00, Navigation 2.7.7,
Retrofit 2.11.0 + OkHttp 4.12.0, kotlinx.serialization 1.7.3,
Media3 1.4.1, Room 2.6.1, DataStore 1.1.1, Security-Crypto 1.1.0,
Coil 2.6.0, Coroutines 1.8.1 + Turbine 1.1.0, MockK 1.13.12,
Detekt 1.23.7 (pendiente de re-activar, ver T003).
