# Tasks: Auth + Explorar + base app TolochaRadio

**Input**: Design documents from `/specs/001-auth-explore-base/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api-v1.md, quickstart.md

**Tests**: OBLIGATORIOS por constitución III y FR-014 (test-first Red-Green en cada historia).

**Organization**: tareas agrupadas por historia; cada historia es implementable y testeable de forma independiente tras la fase Foundational.

**Paths**: `pkg = com.izquierdojl.tolocharadio` (applicationId final, FR-013). Base main: `app/src/main/java/com/izquierdojl/tolocharadio/`. Tests: `app/src/test/java/com/izquierdojl/tolocharadio/` (unit), `app/src/androidTest/java/com/izquierdojl/tolocharadio/` (UI).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: paralelizable (distintos ficheros, sin dependencias)
- **[Story]**: US1…US6 según spec.md

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: dependencias, tooling y migración del esqueleto Views a Compose.

- [X] T001 Añadir al version catalog (`gradle/libs.versions.toml`) Kotlin 2.1.x, Compose BOM 2025.01.00 + M3, Navigation 2.7.7, Hilt 2.51.1, Retrofit 2.11.0/OkHttp 4.12.0/kotlinx.serialization 1.7.3, Media3 1.4.1, Room 2.6.1, DataStore 1.1.1, Security-Crypto 1.1.0, Coil 2.6.0, Coroutines 1.8.1, Turbine 1.1.0, MockK 1.13.12, Detekt 1.23.7.
- [X] T002 Subir a Java 17 en `app/build.gradle.kts`, aplicar plugins Hilt/kotlinx.serialization/Room/Hilt-Compose y declarar `BuildConfig.TOLOCHA_BASE_URL` (default + override `local.properties`).
- [X] T003 [P] Configurar Detekt + ktlint + Android Lint y workflow CI `.github/workflows/android.yml` (build + `testDebugUnitTest` + lint en cada PR).
- [X] T004 Migrar `MainActivity` a `ComponentActivity` Compose con `TolochaTheme` (M3, dark Tolocha verde-bosque/ocre por defecto) y grafo Navigation-Compose base (`home`, `login`) en `core/ui/navigation/`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: red, sesión, almacenamiento y diseño base. BLOQUEA todas las historias.

- [X] T005 [P] Implementar `core/network/ApiResult.kt` + `ApiError.kt` (mapeo `{code,message,status,details}` → `Unauthorized/NotFound/Conflict/Validation/Unavailable/Unknown`) + test en `app/src/test/.../core/network/ApiErrorMapperTest.kt`.
- [X] T006 [P] Implementar DTOs `data/remote/dto/` (User, AuthResponse, Station, StationPage, Favorite, PlaybackStatus, AppConfig) con kotlinx.serialization + tests de serialización contra ejemplos del OpenAPI.
- [X] T007 Implementar `core/network/RetrofitFactory.kt` (baseUrl dinámica, `TokenAuthenticator` con mutex + 1 reintento, `AuthInterceptor`) + `di/NetworkModule.kt` (Hilt).
- [X] T008 Implementar `core/session/SessionManager.kt` (access en memoria `StateFlow`, estados Loading/Authenticated/Unauthenticated) + `TokenStore.kt` (refresh en EncryptedSharedPreferences) + tests `SessionManagerTest.kt` / `TokenAuthenticatorTest.kt` (401→refresh→reintento; refresh inválido→logout).
- [X] T009 Implementar `data/local/` Room (`stations_cache`) + DataStore (`instance_prefs`: baseUrl, tema) + `di/StorageModule.kt` + test de migración/DAO básico.
- [X] T010 Implementar `data/repo/` base: `SystemRepo` (`GET /health`, `GET /config`) + `AuthRepo`/`UserRepo`/`StationsRepo` interfaces + `core/ui/` compartido (`EmptyState`, `FavoriteButton`, `ErrorBanner` ES, `StationCard`/`StationListItem` con Coil + placeholder).

**Checkpoint**: `./gradlew testDebugUnitTest detekt ktlintCheck lintDebug` en verde; foundation lista.

---

## Phase 3: US1 — Arranque contra mi instancia (P1) 🎯 MVP base

**Goal**: onboarding con baseUrl validada + Home pública.

**Independent Test**: instalar, configurar URL válida/inválida, ver Home; cambiar URL limpia sesión.

- [X] T011 [P] [US1] Test `InstanceSetupViewModelTest.kt` (URL válida→guarda; inválida→error; cambio→logout+limpieza).
- [X] T012 [US1] Implementar `feature/onboarding/InstanceSetupScreen.kt` + `InstanceSetupViewModel.kt` (normalizar URL, `health`+`config`, guardar en DataStore) + `NormalizeBaseUrlUseCase.kt`.
- [X] T013 [US1] Implementar `feature/home/HomeScreen.kt` pública (bienvenida + estado conexión + accesos a Login/Explorar) y ruta `home` como start si hay baseUrl, `setup` si no.
- [X] T014 [US1] Ajustes mínimos: editar baseUrl (revalida, cierra sesión y limpia caché anterior).

**Checkpoint**: US1 funciona sin login (quickstart pasos 1–2).

---

## Phase 4: US2 — Login y sesión persistente (P1)

**Goal**: login, persistencia, refresh invisible, logout.

**Independent Test**: login OK → matar app → sigue autenticado; 401→refresh sin pedir login; logout limpia.

- [X] T015 [P] [US2] Tests `LoginViewModelTest.kt` + `AuthRepoTest.kt` (login OK/401/422, logout borra tokens).
- [X] T016 [US2] Implementar `AuthApi.login/refresh/logout` + `LoginUseCase`/`LogoutUseCase` en `domain/` + `LoginScreen.kt`/`LoginViewModel.kt` (validación email, error 401 en español, oculta registro si `registrationEnabled=false`).
- [X] T017 [US2] Cablear `RequireAuth` en navegación (destinos protegidos redirigen a login) + evento `SessionExpired` → Login con aviso.
- [X] T018 [US2] Compose Test `LoginFlowTest.kt` (login válido/inválido, persistencia tras recrear).

**Checkpoint**: US1+US2: sesión completa sin explorar.

---

## Phase 5: US3 — Registro condicional (P2)

**Goal**: registro solo si la instancia lo permite.

**Independent Test**: contra instancia abierta crea cuenta; contra cerrada no hay UI de registro.

- [X] T019 [P] [US3] Tests `RegisterViewModelTest.kt` (201→autenticado; 409→conflicto; 422→errores por campo).
- [X] T020 [US3] Implementar `AuthApi.register` + `RegisterScreen.kt`/`RegisterViewModel.kt` (name/email/password 8–72, name ≤80) + enlace condicional desde Login según `config`.

**Checkpoint**: US3 no rompe US1/US2.

---

## Phase 6: US4 — Explorar el catálogo (P1)

**Goal**: búsqueda + filtros + paginación + ficha + favorito rápido.

**Independent Test**: buscar/filtrar/paginar/abrir ficha/favoritar sin reproducir.

- [X] T021 [P] [US4] Tests `StationsRepoTest.kt` (filtros, `limit/offset/unique`, `hasMore`) + `ExploreViewModelTest.kt` (paginación 3 páginas, 503→caché+aviso, favoritar optimista con rollback).
- [X] T022 [P] [US4] Implementar `StationsApi` (search/detail/countries/languages/tags) + `SearchStationsUseCase` + precarga de filtros con caché Room 24 h.
- [X] T023 [US4] Implementar `ExploreScreen.kt` + `ExploreViewModel.kt` (filtros país/idioma/tag, `unique`, lista/grid con toggle, `StationCard`, estados carga/vacío/error con reintento).
- [X] T024 [US4] Implementar `StationDetailScreen.kt` (ficha `GET /stations/:id` + play + favoritar) y `FavoritesApi.add/remove` + `ToggleFavoriteUseCase` (optimista + rollback).
- [X] T025 [US4] Compose Test `ExploreFlowTest.kt` (buscar "jazz"+país, paginar, abrir ficha, favoritar).

**Checkpoint**: corazón del producto visible sin audio.

---

## Phase 7: US5 — Mini-player persistente (P1)

**Goal**: proxy autenticado conBearer, precheck, background, mini+full player.

**Independent Test**: playable→suena y sobrevive a navegación/rotación; no playable→motivo sin arrancar; error→reintento.

- [X] T026 [P] [US5] Tests `PlaybackRepoTest.kt` (status false→no reproduce; header Bearer presente, token jamás en URL) + `PlayerViewModelTest.kt` (Idle→Buffering→Playing→Error+reintento).
- [X] T027 [US5] Implementar `PlaybackApi.status` + `AuthenticatedDataSourceFactory` (inyecta Bearer) + `RadioPlaybackService : MediaSessionService` + `di/PlayerModule.kt` (ExoPlayer, notificación, foco audio/Bluetooth).
- [X] T028 [US5] Implementar `MiniPlayer` (sobre bottom bar) + full-player `ModalBottomSheet` (play/pausa/quitar) + `PlayerViewModel` (`PlayerState` sellado, backoff, `ConnectivityManager`).
- [X] T029 [US5] Compose Test `PlayerFlowTest.kt` (play/stop, persistencia al navegar, error con reintento).

**Checkpoint**: radio funcional end-to-end (quickstart pasos 4–5).

---

## Phase 8: US6 — Perfil y recuperación (P3)

**Goal**: perfil mínimo + password/reset (si cabe sin crecer alcance).

**Independent Test**: ver/editar perfil y tema; forgot neutro siempre.

- [X] T030 [P] [US6] Tests `ProfileViewModelTest.kt` (`GET/PATCH me`, tema aplica; password→re-login) + forgot neutro.
- [X] T031 [US6] Implementar `UserApi` + `ProfileScreen.kt` (nombre/tema light-dark) + `PasswordScreen.kt` + `ForgotResetScreen.kt` (mensaje neutro anti-enumeración).

**Checkpoint**: paridad `/perfil` web completa.

---

## Phase 9: Polish & Cross-Cutting

- [X] T032 [P] Pasar `applicationId` al paquete final (FR-013) + `versionCode` incremental + `networkSecurityConfig` HTTPS-only.
- [X] T033 Revisión seguridad: cero tokens en URL/log/caché en claro (grep + test `DataSource.Factory`), backup rules excluyen prefs cifradas.
- [X] T034 README (compilar/probar/apuntar a instancia) + KDoc en `domain`/`data` públicos.
- [X] T035 Validación `quickstart.md` end-to-end contra la instancia del usuario + arranque frío < 2 s.

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 (bloqueante) → US1+US2+US4+US5 en orden P1 (US3 P2 y US6 P3 pueden ir en paralelo tras foundation) → Polish.
- MVP incremental: Phase 1+2+US1 (app conecta) → +US2 (sesión) → +US4 (catálogo) → +US5 (radio suena) → +US3/US6.
- En cada historia: tests PRIMERO (deben FALLAR), luego DTOs/casos de uso, luego UI, luego integración; commit por tarea.


---

## Phase 10: Convergence

**Origen**: `/speckit.converge` 2026-09-20 — 14 FR, 6 historias y 5 principios revisados contra el código. Las partes vigentes (catálogo público, Media3, mini-player, mapeo de errores, applicationId, tests) ya están verificadas en las convergences de 0022/0024/0010/0013; el hueco está en la nota de gobernanza, desactualizada tras la constitución 3.0.0.

- [ ] T036 Actualizar la nota de gobernanza de `specs/001-auth-explore-base/spec.md` (línea 9): precisar que la constitución 3.0.0 y la spec 0024 **reintrodujeron** las credenciales por servidor (email/contraseña cifrada) con login automático JWT/`Bearer` — incluida la reproducción por proxy—, y que lo sigue retirado es únicamente el modelo de UI (pantallas de login/registro/Perfil, `/users/me` y registro/recuperación/logout desde la app); matizar FR-003/FR-004/FR-005/FR-010 en consecuencia per plan: gobernanza 0024 (partial) — MEDIUM
