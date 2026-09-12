# Implementation Plan: Credenciales por servidor, auto-login y pantalla unificada de servidor

**Branch**: `0024-jlizquierdo-20260912-per-server-credentials` | **Date**: 2026-09-12 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0024-jlizquierdo-20260912-per-server-credentials/spec.md`

## Summary

Restaurar la autenticación **por servidor** (sin pantallas de login): cada servidor guarda URL, alias, **email** y **contraseña cifrada**; la app hace login automático (JWT + refresh) y firma todas las peticiones con `Authorization: Bearer`, incluida la reproducción (proxy y subrecursos HLS) y Chromecast. Se crea una **pantalla unificada de servidor** (alta y edición) usada en la bienvenida y en la sección Servidores, con icono Editar por tarjeta. Si el servidor activo/por defecto no tiene credenciales (p. ej. migrado de la 0022), el arranque abre esa pantalla de forma bloqueante. Ante errores de credenciales, el botón Reintentar abre la edición del servidor activo; ante errores de red, reintenta. Historial y favoritos vuelven a ser datos del usuario autenticado.

Esto **revierte parcialmente la spec 0022** y exige una **enmienda constitucional** (la 2.0.0 prohíbe almacenar credenciales).

## Technical Context

**Language/Version**: Kotlin 2.3.10, JDK 17 (bytecode target 17; CI Temurin 17)

**Primary Dependencies**: Media3 1.4.1 (ExoPlayer, HLS, MediaSession, `datasource-okhttp`), OkHttp 4.12.0, Retrofit 2.11.0 + `kotlinx.serialization`, Hilt, Coroutines/Flow, Compose M3, Room 2.7.2, DataStore 1.1.1, ProcessPhoenix. **Se reintroduce** `androidx.security:security-crypto` (`EncryptedSharedPreferences`) para credenciales y refresh token.

**Storage**: Room `tolocha.db` **sin cambio de esquema** (v7): `saved_servers` sigue con URL, alias, `appName`, `isActive`, `isDefault`. **Credenciales** (email, contraseña, refresh token) en `EncryptedSharedPreferences` (`tolocha_tokens`) indexadas por `serverId`. El token de acceso vive solo en memoria.

**Testing**: JUnit4 4.13.2, MockK, Turbine, `kotlinx-coroutines-test`; Compose UI tests (androidTest) para los flujos críticos; CI: `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug`.

**Target Platform**: Android `minSdk 26`, `targetSdk`/`compileSdk` 37; `usesCleartextTraffic="false"`.

**Project Type**: mobile-app (un único módulo Gradle `app`, capas `core/`, `data/`, `domain/`, `feature/`).

**Performance Goals**: auto-login silencioso ≤ 2 s percibido al arrancar/cambiar servidor; refresh 401 **single-flight** sin tormentas de login; sin trabajo de red en el hilo principal; reproducción sin cortes con Bearer en subrecursos HLS.

**Constraints**: HTTPS-only; credenciales nunca en logs ni en URL; sin pantallas de login/registro; arranque bloqueante si el servidor activo/por defecto no tiene credenciales; `Email` como campo de login (contrato specs 001/007); no se toca el backend.

**Scale/Scope**: ~10 archivos nuevos (sesión/auth/almacén cifrado + pantalla de servidor), ~20 modificados, ~10 tests restaurados/reescritos. Se retira el formulario simple de la 0022 y se sustituye por la pantalla unificada.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento | Evidencia |
|-----------|--------------|-----------|
| I. MVVM + Clean por capas | PASS | La sesión/auth viven en `core/`/`data/`/`domain/`; la pantalla unificada es UI sin lógica; Hilt inyecta `TokenStore`/`SessionManager`; un solo módulo `app` |
| II. Kotlin-First, Compose M3 y Media3 (auth por servidor) | PASS | Constitución 3.0.0: cada servidor guarda email y contraseña cifrada; login automático JWT/refresh y `Authorization: Bearer` en todas las peticiones, incluida reproducción (proxy y HLS) y Cast; sin pantallas de login (FR-001/FR-004) |
| III. Calidad Test-First (NON-NEGOTIABLE) | PASS | Red-Green: se restauran/reescriben tests de `TokenStore`, `TokenAuthenticator`, `AuthRepo`, `ServerRepository`, `ServerFormViewModel`, arranque y navegación de reintento; Compose test de la pantalla unificada |
| IV. Streaming robusto y manejo de errores | PASS | Constitución 3.0.0: 401/403 → renovación transparente y re-login con credenciales guardadas; error de credenciales → editar servidor activo; error de red → reintentar (FR-005/FR-006) |
| V. Simplicidad modular (YAGNI) | PASS | Se restaura únicamente el subconjunto necesario (login+refresh, sin registro/recuperación/logout); se reintroduce una dependencia ya justificada (`security-crypto`); la pantalla unificada elimina duplicación de formularios |

**Gate result: PASS (2026-09-12).** La enmienda constitucional **3.0.0** redefinió los Principios II y IV con el modelo "credenciales cifradas por servidor + auto-login JWT/Bearer, sin pantallas de login"; T001 completado. La implementación queda desbloqueada.

**Post-Phase 1 re-check**: PASS sin violaciones. El diseño reutiliza el patrón de la spec 007 (credenciales por servidor + auto-login) y el almacén cifrado, sin añadir complejidad innecesaria ni módulos Gradle; `security-crypto` vuelve a ser una dependencia justificada.

## Project Structure

### Documentation (this feature)

```text
specs/0024-jlizquierdo-20260912-per-server-credentials/
├── plan.md              # Este archivo (/speckit.plan)
├── research.md          # Fase 0 (/speckit.plan)
├── data-model.md        # Fase 1 (/speckit.plan)
├── quickstart.md        # Fase 1 (/speckit.plan)
├── contracts/
│   └── server-auth.md   # Contrato de auth por servidor y pantalla unificada
├── checklists/
│   └── requirements.md  # Calidad de spec (ya existente)
└── tasks.md             # Fase 2 (/speckit.tasks — NO creado aquí)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/session/
│   ├── TokenStore.kt                        # NUEVO: credenciales cifradas por servidor (email, password, refresh) + activeServerId
│   └── SessionManager.kt                    # NUEVO: token de acceso en memoria; accessTokenNow()/clear()
├── core/network/
│   ├── AuthInterceptor.kt                   # NUEVO: inyecta Authorization: Bearer
│   ├── TokenAuthenticator.kt                # NUEVO: 401 → refresh single-flight + reintento
│   ├── RetrofitFactory.kt                   # ~ create(baseUrl, session, tokens, authApi, debug) con auth
│   └── ApiError.kt                          # ~ mapear 401/403 a credenciales/sesión (mensaje accionable)
├── core/ui/navigation/
│   ├── Routes.kt                            # ~ añadir SERVER_FORM (y sufijo serverId)
│   └── TolochaNavGraph.kt                   # ~ gate por servidores y por credenciales; destino del formulario unificado
├── core/ui/components/CommonUi.kt           # ~ ErrorBanner con acción primaria configurable (Reintentar vs Editar servidor)
├── domain/auth/
│   └── AuthenticateServerUseCase.kt         # NUEVO: login con credenciales guardadas + refresh; estado de sesión
├── domain/servers/
│   ├── StartupGate.kt                       # ~ estados NoServers / NeedsCredentials / Ready
│   ├── SavedServer.kt                       # ~ + hasCredentials
│   ├── AddServerUseCase.kt                  # ~ con email/password y validación de login
│   └── UpdateServerUseCase.kt               # NUEVO: editar credenciales/alias
├── data/local/servers/
│   ├── SavedServerEntity.kt                 # = sin cambios (URL/alias/appName/estado)
│   └── ServerRepository.kt                  # ~ alta/edición con credenciales en TokenStore; hasCredentials; switch con re-login
├── data/repo/
│   └── AuthRepo.kt                          # NUEVO: POST /auth/login y /auth/refresh
├── data/remote/api/
│   ├── AuthApi.kt                           # NUEVO: login + refresh (sin registro/recuperación/logout)
│   └── (Stations/Favorites/History/CustomStations/Playback/System)  # = sin cambios (reciben Bearer por interceptor)
├── feature/servers/
│   ├── ServerFormScreen.kt                  # NUEVO: pantalla unificada de alta/edición (URL, alias, email, contraseña)
│   ├── ServerFormViewModel.kt               # NUEVO: modos Add/Edit, validación, guardado y re-login
│   ├── ServerListScreen.kt                  # ~ icono Editar por tarjeta; estado de credenciales
│   └── ServerListViewModel.kt               # ~ edición y estado; sin cambios de credenciales en Room
├── feature/onboarding/
│   ├── InstanceSetupScreen.kt               # ✗ ELIMINAR (lo sustituye ServerFormScreen)
│   └── InstanceSetupViewModel.kt            # ✗ ELIMINAR (lo sustituye ServerFormViewModel)
├── feature/favorites/FavoritesScreen.kt     # ~ Reintentar con error de credenciales → editar servidor activo
├── feature/history/HistoryScreen.kt         # ~ Reintentar con error de credenciales → editar servidor activo
├── feature/player/PlayerDataSourceFactory.kt # ~ vuelve a inyectar Bearer (proxy y HLS)
├── MainActivity.kt                          # ~ auto-login de arranque con el servidor activo/default
├── di/NetworkModule.kt                      # ~ restaura wiring de auth
└── di/PlayerModule.kt                       # ~ datasource con sesión

app/build.gradle.kts                         # ~ reintroducir security-crypto
gradle/libs.versions.toml                    # ~ reintroducir lib security-crypto
app/src/main/res/xml/backup_rules.xml        # ~ excluir tolocha_tokens de backup
app/src/main/res/xml/data_extraction_rules.xml # ~ excluir tolocha_tokens de backup
```

```text
app/src/test/java/com/izquierdojl/tolocharadio/
├── core/session/TokenStoreTest.kt                    # NUEVO (o androidTest si depende de EncryptedSharedPreferences)
├── core/session/SessionManagerTest.kt                # NUEVO
├── core/network/TokenAuthenticatorTest.kt            # NUEVO (refresh single-flight, reintento único)
├── core/network/AuthInterceptorTest.kt               # NUEVO (Bearer presente/ausente)
├── data/repo/AuthRepoTest.kt                         # NUEVO (login/refresh, errores)
├── servers/data/repo/ServerRepositoryTest.kt         # ~ credenciales por servidor, hasCredentials, switch con re-login
├── servers/domain/usecase/AddServerUseCaseTest.kt    # ~ email/password y validación
├── feature/servers/ServerFormViewModelTest.kt        # NUEVO (Add/Edit, validación, contraseña conservada)
├── domain/servers/StartupGateTest.kt                 # ~ NeedsCredentials
├── feature/favorites/FavoritesViewModelTest.kt       # ~ enrutado de error de credenciales
├── feature/history/HistoryViewModelTest.kt           # ~ enrutado de error de credenciales
└── feature/player/PlayerDataSourceFactoryTest.kt     # ~ Bearer en el datasource (si es testable en JVM)

app/src/androidTest/java/com/izquierdojl/tolocharadio/
├── feature/servers/ServerFormScreenTest.kt           # NUEVO (flujo crítico de la pantalla unificada)
└── feature/servers/ServerMigrationTest.kt            # = (migración 6→7 se mantiene)
```

**Structure Decision**: se mantiene el único módulo `app` y las capas actuales. La sesión vuelve a `core/session` + `core/network` + `domain/auth` (patrón de la spec 007) pero **sin** registro/recuperación/logout ni sección Perfil. El formulario de la 0022 (`InstanceSetup*`) se sustituye por `ServerForm*` para unificar alta y edición. Room no cambia: las credenciales viven cifradas en `EncryptedSharedPreferences`.

## Complexity Tracking

> Sin violaciones: la reintroducción de credenciales quedó legitimada por la **enmienda constitucional 3.0.0** (2026-09-12, T001 de esta spec). Complexity Tracking vacío.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |

**Gobernanza**: enmienda MAJOR aplicada (2.0.0 → 3.0.0) con Sync Impact Report; la spec 0022 queda parcialmente superseded por la 0024 (T042). Sin deuda de gobernanza pendiente; la implementación puede comenzar.

**Demanda constitucional (COMPLETADA)**: enmienda MAJOR 2.0.0 → 3.0.0 aplicada el 2026-09-12 (`.specify/memory/constitution.md`): Principios II y IV redefinidos (credenciales cifradas por servidor + auto-login JWT/Bearer sin pantallas de login; 401 → refresh/re-login; Reintentar → editar servidor activo), seguridad/backup restaurados y Sync Impact Report actualizado.
