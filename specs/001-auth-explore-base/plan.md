# Implementation Plan: Auth + Explorar + base app TolochaRadio

**Branch**: `001-auth-explore-base` | **Date**: 2026-09-04 | **Spec**: `specs/001-auth-explore-base/spec.md`

**Input**: Feature specification from `/specs/001-auth-explore-base/spec.md`

## Summary

Bootstrap de la app Android (del esqueleto Views a Compose M3 + Hilt +
Retrofit/Media3/Room) y paridad con la web para **auth con sesión
persistente + Explorar con filtros/paginación + mini-player por proxy
autenticado**, contra la instancia self-hosted del usuario (`/api/v1`,
OpenAPI 3.1 de `izquierdojl/tolocharadio`). Decisiones clave en
`research.md`: access en memoria + refresh cifrado, `Authenticator` con
reintento único, `DataSource.Factory` con Bearer, `baseUrl` editable.

## Technical Context

**Language/Version**: Kotlin 2.3.10 (+ plugin Compose), Java 17, Gradle
version catalog (`libs.versions.toml`). AGP 9.3.2 con
`android.builtInKotlin=false` + `android.newDsl=false` (KSP/Hilt
exigen plugin Kotlin clásico; migrar a built-in cuando KSP lo soporte).

**Primary Dependencies**: Compose BOM 2025.01.00 + Material3,
Navigation-Compose 2.7.7, Hilt 2.60.1, Retrofit 2.11.0 + OkHttp 4.12.0 +
kotlinx.serialization 1.7.3, Media3 1.4.1 (exoplayer, session, ui),
Room 2.6.1, DataStore 1.1.1, Security-Crypto 1.1.0, Coil 2.6.0,
Coroutines 1.8.1, Lifecycle 2.8.x.

**Storage**: Room (`stations_cache` para 503/offline) + DataStore
(`instance_prefs`: baseUrl, tema) + EncryptedSharedPreferences
(refresh token). Access token solo en memoria.

**Testing**: JUnit4 + `kotlinx-coroutines-test` + Turbine 1.1.0 +
MockK 1.13.12 (unitarios); Compose Test + Espresso (UI crítica);
Detekt 1.23.7 + ktlint + Android Lint; CI GitHub Actions.

**Target Platform**: Android `minSdk=26`, `target/compileSdk=37`,
solo teléfono/retrato en v1.

**Project Type**: `mobile-app` (single-module `app`, por features).

**Performance Goals**: arranque frío < 2 s en gama media; lista Explorar
pagina 24 ítems, scroll sin jank (Coil + lazy layouts); primer audio
< 3 s tras `Playing` con red normal.

**Constraints**: HTTPS-only; cero secretos en código; Bearer nunca en
URL/log; `Station.url` directa prohibida; dark Tolocha por defecto;
`applicationId` final pendiente (hoy `com.example.tolocharadio`).

**Scale/Scope**: ~10 pantallas, 6 servicios Retrofit, 1
`MediaSessionService`, alcance spec FR-001…FR-014 (sin Favoritos
completo/Historial/Mis emisoras/Sugerencias).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] I (MVVM+Clean): `ui/{home,explore,auth,profile,player}` →
  `domain` (UseCases puros) → `data` (Retrofit/Room); Hilt en todo.
- [x] II (Stack): Compose M3 único UI nuevo; Media3 (no MediaPlayer);
  Flow sin LiveData/GlobalScope; DTOs espejo OpenAPI.
- [x] III (Test-First): unitarios por UseCase/Repo/VM + serialización
  DTO + Compose Test en 5 flujos; Detekt/ktlint/Lint en CI.
- [x] IV (Robusto): `UiState` sellado + `PlayerState`, mapeo
  `{code,message,details}`, precheck `status`, backoff + foco audio.
- [x] V (YAGNI): sin multi-módulo, sin Paging3, sin DRM/offline-audio;
  pantallas fuera de alcance marcadas "próximamente", no implementadas.
- Re-check tras diseño: OK — el plan no introduce violaciones nuevas.

## Project Structure

### Documentation (this feature)

```text
specs/001-auth-explore-base/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── api-v1.md        # Endpoints + DTOs + servicios Retrofit
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
app/src/main/java/<pkg>/
├── di/                  # NetworkModule, StorageModule, PlayerModule
├── core/
│   ├── network/         # Retrofit builder, TokenAuthenticator, ApiResult, ApiError
│   ├── session/         # SessionManager, TokenStore (cifrado)
│   └── ui/              # Theme Tolocha, Navigation, EmptyState, FavoriteButton
├── feature/
│   ├── onboarding/      # InstanceSetup (baseUrl + health/config check)
│   ├── home/            # Home pública
│   ├── auth/            # Login, Register (condicional), forgot/reset (si cabe)
│   ├── explore/         # Explore + filtros + StationCard/ListItem + detalle
│   ├── profile/         # Perfil mínimo (me + theme)
│   └── player/          # RadioPlaybackService, AuthDataSource, MiniPlayer, FullPlayer
└── data/
    ├── remote/          # 6 *Api + DTOs kotlinx.serialization
    ├── local/           # Room (stations_cache) + DataStore
    └── repo/            # AuthRepo, StationsRepo, FavoritesRepo(toggle), PlaybackRepo

app/src/test/java/<pkg>/     # unitarios por capa + serialización DTO
app/src/androidTest/java/     # Compose Test 5 flujos críticos
```

**Structure Decision**: single-module `app` organizado por feature
espejo de la web (`home/explore/auth/profile/player`), capas
`ui→domain→data` dentro de cada feature + `core/data` compartidos;
sin módulos Gradle nuevos (principio V).

## Complexity Tracking

> Sin violaciones constitucionales que justificar.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
