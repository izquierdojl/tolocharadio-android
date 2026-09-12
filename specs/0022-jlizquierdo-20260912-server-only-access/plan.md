# Implementation Plan: Acceso solo con servidores y retirada de la autenticación de usuario

**Branch**: `0022-jlizquierdo-20260912-server-only-access` | **Date**: 2026-09-12 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0022-jlizquierdo-20260912-server-only-access/spec.md`

## Summary

Retirar por completo la autenticación de usuario (login, registro, recuperación, sesión/JWT, refresh y `Authorization: Bearer`) y dejar el **servidor (instancia)** como única unidad de configuración y acceso. El arranque pasa a depender de si hay servidores configurados: **sin ninguno**, se muestra una pantalla de bienvenida/onboarding dedicada que lleva a configurar el primero (bloqueante); **con al menos uno**, la app funciona igual que hoy pero sin credenciales.

Técnicamente: se eliminan `core/session/*` (`SessionManager`, `SessionRestorer`, `TokenStore`), `domain/auth/*`, `feature/auth/*`, `data/repo/AuthRepo.kt`, `data/repo/UserRepo.kt`, `AuthApi`/`UserApi`, `AuthInterceptor`, `TokenAuthenticator` y la dependencia `security-crypto`; `RetrofitFactory`/`NetworkModule` dejan de inyectar auth; `SavedServerEntity`/`SavedServer` pierden `userEmail` (migración Room 6→7); el datasource del reproductor pierde el Bearer; `TolochaNavGraph` deja de tener gate de sesión y pasa a gate de servidores; `InstanceSetupViewModel.connect` crea el `SavedServerEntity` inicial; shortcuts y ajustes dejan de depender de la sesión (el tema pasa a ser local). Historial y favoritos siguen viviendo en el servidor, ahora compartidos por instancia (FR-015).

## Technical Context

**Language/Version**: Kotlin 2.3.10, JDK 17 (bytecode target 17; CI Temurin 17)

**Primary Dependencies**: Media3 1.4.1 (ExoPlayer, HLS, MediaSession, `datasource-okhttp`), OkHttp 4.12.0, Retrofit 2.11.0 + `kotlinx.serialization`, Hilt, Coroutines/Flow, Compose M3, Room 2.7.2, DataStore 1.1.1, ProcessPhoenix. **Se elimina** `androidx.security:security-crypto` (su único consumidor era `TokenStore`). Sin dependencias nuevas.

**Storage**: Room `tolocha.db` (v6 → v7: `saved_servers` sin `userEmail`); DataStore `tolocha_prefs` (`baseUrl`, `setupDone`, tema, pantalla de arranque, modo de vista). No se almacena ninguna credencial (FR-009/SC-006).

**Testing**: JUnit4 4.13.2, MockK, Turbine, `kotlinx-coroutines-test`; sin Robolectric. CI: `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug`.

**Target Platform**: Android `minSdk 26`, `targetSdk`/`compileSdk` 37; `usesCleartextTraffic="false"`.

**Project Type**: mobile-app (un único módulo Gradle `app`, capas `core/`, `data/`, `domain/`, `feature/`).

**Performance Goals**: arranque percibido ≤ 2 s; primera configuración de servidor + contenido < 30 s (SC-003); decisión de arranque O(1) y sin red; sin trabajo de red en el hilo principal.

**Constraints**: cero credenciales almacenadas o enviadas (FR-009/FR-011); HTTPS-only (FR-011); se asume instancia actualizada sin auth y no se conserva compatibilidad con login (FR-016); sin cambios de esquema más allá de quitar `userEmail`; se mantiene el cambio de servidor con rebirth de proceso (ProcessPhoenix); historial/favoritos compartidos por instancia (FR-015); no se toca el backend (salvo el contrato ya desplegado).

**Scale/Scope**: reducción neta de código — ~16 archivos de producción eliminados, ~22 modificados, ~10 de test eliminados/reescritos. Una sola pantalla nueva (bienvenida), reutilizando `InstanceSetupScreen`; sin pantallas de login/registro.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento | Evidencia |
|-----------|--------------|-----------|
| I. MVVM + Clean por capas | PASS | Se retiran `core/session`, `domain/auth` y `feature/auth`; el arranque y la gestión de servidores permanecen en `domain/servers` + `feature/onboarding`/`feature/servers`; Hilt se simplifica; un solo módulo `app` |
| II. Kotlin-First, Compose M3 y Media3 (servidor sin auth de usuario) | PASS | Constitución 2.0.0: la app opera sin autenticación de usuario; el servidor (instancia) es la unidad de acceso y está PROHIBIDO enviar credenciales, tokens o `Authorization`; el playback va por el proxy sin credenciales (FR-001, FR-011, FR-016) |
| III. Calidad Test-First (NON-NEGOTIABLE) | PASS | Red-Green: se reescriben primero los tests de servidores/onboarding/settings/shortcuts y se eliminan los de auth/sesión; sin Robolectric; se conservan los gates de CI |
| IV. Streaming robusto y manejo de errores | PASS | Constitución 2.0.0: 401/403 se mapea a "instancia no compatible/actualizar servidor" sin ofrecer login; se mantienen `UiState` sellado, mensajes accionables, precheck y KDoc (FR-012, FR-016) |
| V. Simplicidad modular (YAGNI) | PASS | Se elimina código muerto (auth/sesión/`TokenStore`, `ForgotScreen` sin ruta, `switchInstance` sin llamadas) y una dependencia (`security-crypto`); no se añaden módulos ni librerías |

**Gate result: PASS (2026-09-12).** La enmienda constitucional **2.0.0** redefine el acceso como "servidor sin auth de usuario", retira JWT/refresh/Bearer de los Principios II y IV y limpia las referencias de las specs 001/007 (T001 completado). La implementación queda desbloqueada.

**Post-Phase 1 re-check**: PASS sin violaciones. El diseño (`research.md`, `data-model.md`, `contracts/`, `quickstart.md`) no añade módulos Gradle ni dependencias (retira `security-crypto`), reduce la superficie de red y de código y es coherente con la constitución 2.0.0.

## Project Structure

### Documentation (this feature)

```text
specs/0022-jlizquierdo-20260912-server-only-access/
├── plan.md              # Este archivo (/speckit.plan)
├── research.md          # Fase 0 (/speckit.plan)
├── data-model.md        # Fase 1 (/speckit.plan)
├── quickstart.md        # Fase 1 (/speckit.plan)
├── contracts/
│   └── server-only-access.md  # Contrato de arranque/acceso sin credenciales
├── checklists/
│   └── requirements.md  # Calidad de spec (ya existente)
└── tasks.md             # Fase 2 (/speckit.tasks — NO creado aquí)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/session/
│   ├── SessionManager.kt                    # ✗ ELIMINAR
│   ├── SessionRestorer.kt                   # ✗ ELIMINAR
│   └── TokenStore.kt                        # ✗ ELIMINAR (único consumidor de security-crypto)
├── core/network/
│   ├── AuthInterceptor.kt                   # ✗ ELIMINAR
│   ├── TokenAuthenticator.kt                # ✗ ELIMINAR
│   ├── RetrofitFactory.kt                   # ~ create(baseUrl, debug) sin auth
│   └── ApiError.kt                          # ~ retirar mensaje de sesión expirada
├── core/ui/navigation/
│   ├── Routes.kt                            # ~ retirar LOGIN/REGISTER/AUTH_REQUIRED
│   └── TolochaNavGraph.kt                   # ~ gate por servidores; inicio en bienvenida si no hay
├── core/shortcuts/
│   └── ShortcutSyncCoordinator.kt           # ~ publicar historial sin gate de sesión
├── domain/auth/                             # ✗ ELIMINAR carpeta completa
│   ├── LoginUseCase.kt
│   ├── LogoutUseCase.kt
│   ├── RestoreSessionUseCase.kt
│   └── StoreServerCredentialsUseCase.kt
├── domain/ValidateUseCases.kt               # ~ retirar ValidateAuthUseCase (conservar NormalizeBaseUrlUseCase)
├── domain/servers/
│   ├── SavedServer.kt                       # ~ sin userEmail
│   └── AddServerUseCase.kt                  # ~ sin email/password
├── domain/shortcuts/
│   ├── ResolveShortcutLaunchUseCase.kt      # ~ sin AuthState ni GoLogin
│   └── ShortcutLaunchResolution.kt          # ~ sin GoLogin
├── data/local/
│   ├── servers/SavedServerEntity.kt         # ~ sin userEmail
│   ├── servers/MigrationHelper.kt           # ~ first() (bug de collect), sin TokenStore
│   └── TolochaDb.kt                         # ~ MIGRATION_6_7 (recrear saved_servers sin userEmail)
├── data/repo/
│   ├── AuthRepo.kt                          # ✗ ELIMINAR
│   ├── UserRepo.kt                          # ✗ ELIMINAR
│   └── servers/ServerRepository.kt          # ~ sin TokenStore ni credenciales
├── data/remote/api/
│   ├── AuthApi.kt                           # ✗ ELIMINAR
│   ├── UserApi.kt                           # ✗ ELIMINAR
│   └── (Stations/Favorites/History/CustomStations/Playback/System)  # = sin cambios (llamadas sin Bearer)
├── data/remote/dto/AuthDtos.kt              # ~ conservar AppConfigDto/PlaybackStatusDto; retirar DTOs de auth/user
├── feature/auth/                            # ✗ ELIMINAR carpeta completa (Login/Register/Forgot + ViewModels)
├── feature/onboarding/
│   ├── InstanceSetupScreen.kt               # ~ bienvenida dedicada: captura URL + alias del servidor (sin credenciales)
│   └── InstanceSetupViewModel.kt            # ~ crear SavedServerEntity activo/por defecto; retirar switchInstance muerto
├── feature/servers/
│   ├── ServerListScreen.kt                  # ~ quitar email/password del diálogo y userEmail de la tarjeta
│   └── ServerListViewModel.kt               # ~ switch sin credenciales (siempre rebirth), sin TokenStore
├── feature/settings/
│   ├── SettingsViewModel.kt                 # ~ sin LogoutUseCase/UserRepo; tema local
│   └── SettingsScreen.kt                    # ~ quitar "Cerrar sesión"
├── feature/home/HomeScreen.kt               # ~ retirar SessionManager/AuthState
├── feature/player/
│   ├── AuthDataSourceFactory.kt             # ~ renombrar a PlayerDataSourceFactory, sin Bearer
│   └── StationMediaItemFactory.kt           # ~ usa el datasource sin auth (URI proxy se mantiene)
├── feature/shortcuts/ShortcutLaunchViewModel.kt  # ~ sin SessionManager
├── MainActivity.kt                          # ~ sin SessionManager/SessionRestorer
├── di/NetworkModule.kt                      # ~ sin session/tokens/authApi
└── di/PlayerModule.kt                       # ~ datasource sin session

app/src/main/res/xml/
├── backup_rules.xml                         # ~ retirar exclusiones de tokens
└── data_extraction_rules.xml                # ~ retirar exclusiones de tokens

app/build.gradle.kts                         # ~ retirar security-crypto
gradle/libs.versions.toml                    # ~ retirar lib security-crypto
```

```text
app/src/test/java/com/izquierdojl/tolocharadio/
├── auth/... (SessionManagerTest, TokenStoreTest, LogoutUseCaseTest, RestoreSessionUseCaseTest, StoreServerCredentialsUseCaseTest)  # ✗ ELIMINAR
├── core/session/SessionManagerTest.kt       # ✗ ELIMINAR
├── core/network/TokenAuthenticatorTest.kt   # ✗ ELIMINAR
├── data/repo/AuthRepoTest.kt                # ✗ ELIMINAR
├── feature/auth/AuthViewModelsTest.kt       # ✗ ELIMINAR
├── servers/data/repo/ServerRepositoryTest.kt  # ~ sin credenciales
├── servers/domain/usecase/AddServerUseCaseTest.kt  # ~ sin email/password
├── feature/onboarding/InstanceSetupViewModelTest.kt # ~ crea servidor
├── feature/settings/SettingsViewModelTest.kt  # ~ sin logout
├── core/ui/navigation/RoutesTest.kt         # ~ sin AUTH_REQUIRED
├── core/shortcuts/ShortcutSyncCoordinatorTest.kt  # ~ sin gate de sesión
├── domain/shortcuts/ResolveShortcutLaunchUseCaseTest.kt # ~ sin AuthState
├── domain/ValidateUseCasesTest.kt           # ~ sin ValidateAuthUseCaseTest
├── domain/servers/StartupGateTest.kt        # NUEVO (decisión de arranque: sin servidores → bienvenida)
└── (androidTest) data/local/servers/ServerMigrationTest.kt # NUEVO (migración Room 6→7; ver T047)
```

**Structure Decision**: se mantiene el único módulo `app` y las capas actuales. La lógica de arranque/estado de servidores queda en `domain/servers` + `feature/onboarding`/`feature/servers` (pura y testable en JVM en lo posible); desaparece toda la capa de sesión/auth. `InstancePrefs.baseUrl` se conserva como URL efectiva de runtime (Retrofit/Cast/Player), mantenida por onboarding y por el cambio de servidor; `SavedServerEntity` sigue siendo la lista persistida (activo/por defecto) sin credenciales.

## Complexity Tracking

> Sin violaciones: la retirada del modelo de autenticación quedó legitimada por la **enmienda constitucional 2.0.0** (2026-09-12, T001 de esta spec). Complexity Tracking vacío.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |

**Gobernanza**: enmienda MAJOR aplicada (1.1.0 → 2.0.0) con Sync Impact Report; specs 001 (`auth-explore-base`) y 007 (`persistent-session`) marcadas como superseded por la spec 0022. Sin deuda de gobernanza pendiente; la implementación puede comenzar.
