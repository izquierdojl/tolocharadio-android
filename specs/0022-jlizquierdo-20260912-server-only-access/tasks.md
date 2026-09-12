---
description: "Task list for Acceso solo con servidores y retirada de la autenticación de usuario"
---

# Tasks: Acceso solo con servidores y retirada de la autenticación de usuario

**Input**: Design documents from `/specs/0022-jlizquierdo-20260912-server-only-access/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: INCLUIDOS Y OBLIGATORIOS. La constitución (Principio III, NON-NEGOTIABLE) exige tests unitarios y Red-Green: escribir el test que falla antes de la implementación. Sin Robolectric; la lógica de arranque/servidores es Kotlin puro/JVM. Además, tests de UI Compose (androidTest) para los flujos críticos exigidos por la constitución: bienvenida/configuración de servidor (T045) y cambio de servidor (T046).

**✅ GATE DE GOBERNANZA**: T001 COMPLETADO (constitución 2.0.0 aplicada el 2026-09-12; specs 001/007 marcadas como superseded). La implementación está desbloqueada.

**Organization**: tareas agrupadas por user story (US1 bienvenida + primer servidor P1; US2 uso diario sin credenciales P1; US3 gestión de servidores P2).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: US1/US2/US3 según `spec.md`
- Rutas exactas de archivo en cada tarea

## Path Conventions

- Módulo único `app` (Android/Kotlin). Código: `app/src/main/java/com/izquierdojl/tolocharadio/`
- Tests: `app/src/test/java/com/izquierdojl/tolocharadio/`
- Base package: `com.izquierdojl.tolocharadio`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: gate de gobernanza y limpieza de recursos compartidos

- [X] T001 Aprobar la enmienda constitucional MAJOR 2.0.0 que redefine el acceso como "servidor sin autenticación de usuario", retira JWT/refresh/Bearer de los Principios II y IV y limpia las referencias de auth en `specs/001-auth-explore-base/` y `specs/007-persistent-session/` — COMPLETADO 2026-09-12 (`.specify/memory/constitution.md` v2.0.0; notas de superseded en ambas specs; plan.md actualizado a PASS)
- [X] T002 [P] Retirar la dependencia `security-crypto` de `gradle/libs.versions.toml` y de `app/build.gradle.kts`
- [X] T003 [P] Retirar las exclusiones del almacén de tokens en `app/src/main/res/xml/backup_rules.xml` y `app/src/main/res/xml/data_extraction_rules.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: retirada del sistema de autenticación y adaptación de datos/red/arranque

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase

- [X] T004 [P] Eliminar `app/src/main/java/com/izquierdojl/tolocharadio/core/session/SessionManager.kt`, `SessionRestorer.kt` y `TokenStore.kt`
- [X] T005 [P] Eliminar `app/src/main/java/com/izquierdojl/tolocharadio/core/network/AuthInterceptor.kt` y `TokenAuthenticator.kt`
- [X] T006 [P] Eliminar `app/src/main/java/com/izquierdojl/tolocharadio/domain/auth/` (`LoginUseCase.kt`, `LogoutUseCase.kt`, `RestoreSessionUseCase.kt`, `StoreServerCredentialsUseCase.kt`) y retirar `ValidateAuthUseCase` de `domain/ValidateUseCases.kt` (conservar `NormalizeBaseUrlUseCase`); actualizar `app/src/test/java/com/izquierdojl/tolocharadio/domain/ValidateUseCasesTest.kt` (retirar `ValidateAuthUseCaseTest`)
- [X] T007 [P] Eliminar `app/src/main/java/com/izquierdojl/tolocharadio/feature/auth/` (5 archivos), `data/repo/AuthRepo.kt`, `data/repo/UserRepo.kt`, `data/remote/api/AuthApi.kt` y `data/remote/api/UserApi.kt`
- [X] T008 [P] Retirar de `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/AuthDtos.kt` los DTO de auth/usuario (`AuthResponseDto`, `UserDto`, `ThemeDto`, cuerpos de login/registro/refresh/forgot/reset, envelopes) conservando `AppConfigDto` y `PlaybackStatusDto`
- [X] T009 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/core/network/RetrofitFactory.kt` y `di/NetworkModule.kt`: eliminar `SessionManager`, `TokenStore`, `AuthInterceptor`, `TokenAuthenticator` y el provider de `AuthApi`; `RetrofitFactory.create(baseUrl, debug)` (depende de T005)
- [X] T010 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/core/network/ApiError.kt`: mapear 401/403 a "instancia no actualizada/no compatible" y retirar el mensaje de sesión expirada (FR-016)
- [X] T011 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/domain/servers/SavedServer.kt` y `data/local/servers/SavedServerEntity.kt` para quitar `userEmail`, y añadir `MIGRATION_6_7` (recrear `saved_servers` sin `userEmail`, conservando filas e índices) subiendo `TolochaDb` a v7
- [X] T047 [P] Añadir test de migración Room 6→7 en `app/src/androidTest/java/com/izquierdojl/tolocharadio/data/local/servers/ServerMigrationTest.kt` (conserva filas de `saved_servers` y elimina `userEmail`) — depende de T011 (constitución: migraciones Room probadas)
- [X] T012 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/servers/ServerRepository.kt`: quitar `TokenStore` y toda lógica de credenciales en `add`/`switchTo`/`delete`; conservar activo/por defecto, limpieza de caché y shortcuts (depende de T011)
- [X] T013 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/data/local/servers/MigrationHelper.kt`: usar `first()` en lugar de `collect` (bug de arranque), quitar `TokenStore` y borrar una sola vez el almacén legacy `tolocha_tokens` con `Context.deleteSharedPreferences("tolocha_tokens")` (API framework; no depende de `security-crypto`, que T002 retira) (depende de T004)
- [X] T014 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/domain/servers/AddServerUseCase.kt`: eliminar parámetros `email`/`password` (depende de T011)
- [X] T015 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/MainActivity.kt`: retirar `SessionManager`, `SessionRestorer` y la llamada a `restore()` (depende de T004)
- [X] T016 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/Routes.kt`, `TolochaNavGraph.kt` y `feature/home/HomeScreen.kt`: retirar `LOGIN`/`REGISTER`/`AUTH_REQUIRED`, destinos y ramas de login/registro, el parámetro `sessionManager` y `SessionManager`/`AuthState` de HomeScreen; actualizar `app/src/test/java/com/izquierdojl/tolocharadio/core/ui/navigation/RoutesTest.kt` (sin `AUTH_REQUIRED`) (depende de T007)
- [X] T017 [P] Eliminar los tests de auth/sesión: `app/src/test/java/com/izquierdojl/tolocharadio/auth/` (SessionManagerTest, TokenStoreTest, LogoutUseCaseTest, RestoreSessionUseCaseTest, StoreServerCredentialsUseCaseTest), `core/session/SessionManagerTest.kt`, `core/network/TokenAuthenticatorTest.kt`, `data/repo/AuthRepoTest.kt` y `feature/auth/AuthViewModelsTest.kt`

**Checkpoint**: auth eliminada; base lista para US1, US2 y US3

---

## Phase 3: User Story 1 - Primer arranque te guía a configurar un servidor (Priority: P1) 🎯 MVP

**Goal**: sin servidores, pantalla de bienvenida dedicada y bloqueante; al configurar el primero, acceso al contenido sin credenciales.

**Independent Test**: instalación limpia → bienvenida con "configura un servidor", sin login; añadir servidor válido → contenido; URL inválida → error con reintento. Ver `quickstart.md` Escenarios 1–2.

### Tests for User Story 1 ⚠️ (escribir primero, deben FALLAR)

- [X] T018 [P] [US1] Escribir `StartupGateTest` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/servers/StartupGateTest.kt` (lista vacía → `NoServers`; con ≥1 servidor → `Ready`)
- [X] T019 [P] [US1] Reescribir `InstanceSetupViewModelTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/onboarding/InstanceSetupViewModelTest.kt` (URL válida crea servidor activo+por defecto y fija `baseUrl`; inválida no guarda y muestra error; sin `SessionManager`/`TokenStore`)
- [X] T045 [US1] Escribir test Compose `WelcomeServerSetupUiTest` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/onboarding/WelcomeServerSetupUiTest.kt` (sin servidores: bienvenida bloqueante y sin campos de credenciales; URL+alias válidos navegan al contenido; URL inválida muestra error con reintento) — depende de T021–T023

### Implementation for User Story 1

- [X] T020 [US1] Crear `StartupGate` puro (estado `NoServers`/`Ready` a partir de la lista de servidores) en `app/src/main/java/com/izquierdojl/tolocharadio/domain/servers/StartupGate.kt` — hacer pasar T018
- [X] T021 [US1] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/feature/onboarding/InstanceSetupViewModel.kt`: crear el `SavedServerEntity` activo/por defecto vía `ServerRepository`/`AddServerUseCase`, retirar `SessionManager` y `switchInstance` (incluye propagar URL + alias a `AddServerUseCase`) — hacer pasar T019
- [X] T022 [US1] Adaptar `app/src/main/java/com/izquierdojl/tolocharadio/feature/onboarding/InstanceSetupScreen.kt` como pantalla de bienvenida dedicada ("Configura un servidor para acceder") con campos **URL + alias** (alias por defecto editable desde `appName` de `/config` o "Mi servidor") y ajustar strings en `app/src/main/res/values/strings.xml`
- [X] T023 [US1] Implementar el gate de arranque en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`: sin servidores → bienvenida bloqueante; con servidores → `StartScreen`; instancia inalcanzable → error accionable con reintento (FR-002, FR-012)
- [X] T024 [US1] Validar manualmente los Escenarios 1–2 de `quickstart.md` (bienvenida + primer servidor) en emulador/dispositivo — verificado manualmente en emulador por el usuario (2026-09-12)

**Checkpoint**: primer arranque sin auth operativo — MVP demostrable

---

## Phase 4: User Story 2 - Uso diario sin credenciales (Priority: P1)

**Goal**: reproducción, historial, favoritos, ajustes y shortcuts funcionan sin pedir usuario ni contraseña y sin enviar credenciales.

**Independent Test**: con servidor configurado, reproducir y abrir Historial/Favoritos sin login; tráfico sin `Authorization`; ajustes sin "Cerrar sesión"; shortcuts sin sesión. Ver `quickstart.md` Escenarios 3–4 y 8–9.

### Tests for User Story 2 ⚠️ (escribir primero, deben FALLAR)

- [X] T025 [P] [US2] Reescribir `SettingsViewModelTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModelTest.kt` (sin logout ni `UserRepo`; tema y pantalla de arranque locales)
- [X] T026 [P] [US2] Reescribir `ShortcutSyncCoordinatorTest` y `ResolveShortcutLaunchUseCaseTest` en `app/src/test/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSyncCoordinatorTest.kt` y `app/src/test/java/com/izquierdojl/tolocharadio/domain/shortcuts/ResolveShortcutLaunchUseCaseTest.kt` (sin `AuthState`/`GoLogin`; publicar con servidor configurado)
- [X] T027 [P] [US2] Actualizar `StationMediaItemFactoryTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactoryTest.kt` (URI siempre proxy; HLS/progresivo por extensión; sin Bearer)

### Implementation for User Story 2

- [X] T028 [US2] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt` y `SettingsScreen.kt`: retirar `LogoutUseCase` y el botón "Cerrar sesión"; tema solo local — hacer pasar T025
- [X] T029 [US2] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSyncCoordinator.kt`, `domain/shortcuts/ResolveShortcutLaunchUseCase.kt`, `domain/shortcuts/ShortcutLaunchResolution.kt` y `feature/shortcuts/ShortcutLaunchViewModel.kt` para operar sin sesión — hacer pasar T026
- [X] T030 [US2] Renombrar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/AuthDataSourceFactory.kt` a `PlayerDataSourceFactory.kt` sin `SessionManager`/Bearer, y actualizar `StationMediaItemFactory.kt` y `di/PlayerModule.kt` — hacer pasar T027
- [X] T031 [US2] Añadir test de regresión `NoAuthRequestsTest` en `app/src/test/java/com/izquierdojl/tolocharadio/core/network/NoAuthRequestsTest.kt` (el cliente de `RetrofitFactory` no añade `Authorization`) y verificar que `FavoritesRepo`/`HistoryRepo`/`CustomStationsRepo` funcionan sin auth
- [X] T032 [US2] Validar manualmente los Escenarios 3–4 y 8–9 de `quickstart.md` (uso sin credenciales, datos compartidos, actualización desde versión anterior, ajustes/shortcuts) — verificado manualmente en emulador por el usuario (2026-09-12)

**Checkpoint**: uso diario sin credenciales y sin regresiones del reproductor

---

## Phase 5: User Story 3 - Gestión de varios servidores sin credenciales (Priority: P2)

**Goal**: alta, cambio y borrado de servidores sin credenciales; activo/por defecto; borrar el último vuelve a la bienvenida.

**Independent Test**: dos servidores, cambiar activo (con limpieza de caché) y borrar hasta el último → bienvenida. Ver `quickstart.md` Escenarios 5–7.

### Tests for User Story 3 ⚠️ (escribir primero, deben FALLAR)

- [X] T033 [P] [US3] Reescribir `ServerRepositoryTest` en `app/src/test/java/com/izquierdojl/tolocharadio/servers/data/repo/ServerRepositoryTest.kt` (sin credenciales; primer servidor activo+por defecto; `switchTo` limpia caché; `delete` promociona activo/por defecto)
- [X] T034 [P] [US3] Reescribir `AddServerUseCaseTest` en `app/src/test/java/com/izquierdojl/tolocharadio/servers/domain/usecase/AddServerUseCaseTest.kt` (sin email/password; validación de alias; fallo de validación de URL)
- [X] T035 [P] [US3] Escribir `ServerListViewModelTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/servers/ServerListViewModelTest.kt` (switch sin `TokenStore`; borrado del último → evento de bienvenida; sin `onNeedLogin`)
- [X] T046 [US3] Escribir test Compose `ServerSwitchUiTest` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/servers/ServerSwitchUiTest.kt` (cambio de servidor sin credenciales; borrar el último vuelve a la bienvenida) — depende de T036–T037

### Implementation for User Story 3

- [X] T036 [US3] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/feature/servers/ServerListScreen.kt`: quitar email/password del diálogo y `userEmail` de la tarjeta (FR-009)
- [X] T037 [US3] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/feature/servers/ServerListViewModel.kt`: quitar `TokenStore`/`onNeedLogin`, cambiar servidor siempre sin credenciales y señalar el retorno a bienvenida al borrar el último — hacer pasar T035 (depende de T012)
- [X] T038 [US3] Validar manualmente los Escenarios 5–7 de `quickstart.md` (varios servidores, cambio, borrado del último, instancia no disponible/no actualizada) — verificado manualmente en emulador por el usuario (2026-09-12)

**Checkpoint**: gestión multi-servidor sin credenciales completa

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: limpieza total, gobernanza, gates y validación integral

- [X] T039 [P] Eliminar código muerto restante: `ServerRepository.getDefault()`/`ServerDao.getDefault()` si quedan sin uso y el directorio vacío `feature/profile/`
- [X] T040 Verificar con búsqueda global 0 referencias en código de producción a `SessionManager`, `TokenStore`, `AuthInterceptor`, `TokenAuthenticator`, `AuthRepo`, `UserRepo`, `AuthApi`, `UserApi`, `LoginScreen`, `RegisterScreen`, `AUTH_REQUIRED`, `Bearer` y `credential` (FR-008, SC-004)
- [X] T041 [P] Actualizar `README.md`/notas de versión indicando que las emisoras solo requieren una instancia actualizada sin autenticación (FR-016)
- [X] T042 [P] Actualizar `detekt-baseline.xml` si el cambio introduce/retira entradas (ejecutar `./gradlew detektBaseline` solo si hace falta)
- [X] T043 Ejecutar `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` y corregir cualquier error o warning nuevo (obligatorio para el merge)
- [X] T044 Validar el `quickstart.md` completo (Escenarios 0–9), incluida la actualización desde la versión anterior y la ausencia de credenciales en el dispositivo (SC-006) — verificado manualmente en emulador por el usuario (2026-09-12)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias; T001 (enmienda) ya COMPLETADO — no bloquea
- **Foundational (Phase 2)**: depende de Setup; bloquea todas las user stories
- **US1 (Phase 3)**: depende de Foundational y de T014/T020
- **US2 (Phase 4)**: depende de Foundational (T009, T010) y usa `PlayerDataSourceFactory` (T030)
- **US3 (Phase 5)**: depende de Foundational (T011, T012, T014)
- **Polish (Phase 6)**: depende de que US1–US3 estén completas

### User Story Dependencies

- **US1**: sin dependencias de otras historias (tras Foundational)
- **US2**: sin dependencias de US1; comparte `TolochaNavGraph`/arranque (coordinar T023 con T029)
- **US3**: sin dependencias de US1/US2; comparte `ServerRepository`/`AddServerUseCase` (ya en Foundational)

### Within Each User Story

- Tests PRIMERO y en rojo antes de implementar (constitución III)
- Tipos puros (`StartupGate`) antes de la integración en ViewModel/navegación
- Cada historia termina con su validación manual de `quickstart.md`
- No commitear sin que lo pida el usuario (el hook `speckit.git.commit` es opcional)

### Parallel Opportunities

- Setup: T002 y T003 en paralelo (T001 ya completado)
- Foundational: T004–T008 y T017 en paralelo; T009–T016 en secuencia según dependencias; T047 tras T011
- US1 tests: T018 y T019 en paralelo
- US2 tests: T025, T026 y T027 en paralelo
- US3 tests: T033, T034 y T035 en paralelo
- Polish: T039, T041 y T042 en paralelo

---

## Parallel Example: User Story 1

```bash
# Tests de US1 en paralelo (archivos distintos):
Task: "Escribir StartupGateTest en app/src/test/java/com/izquierdojl/tolocharadio/domain/servers/StartupGateTest.kt"
Task: "Reescribir InstanceSetupViewModelTest en app/src/test/java/com/izquierdojl/tolocharadio/feature/onboarding/InstanceSetupViewModelTest.kt"
```

## Parallel Example: User Story 3

```bash
# Tests de US3 en paralelo (archivos distintos):
Task: "Reescribir ServerRepositoryTest en app/src/test/java/com/izquierdojl/tolocharadio/servers/data/repo/ServerRepositoryTest.kt"
Task: "Reescribir AddServerUseCaseTest en app/src/test/java/com/izquierdojl/tolocharadio/servers/domain/usecase/AddServerUseCaseTest.kt"
Task: "Escribir ServerListViewModelTest en app/src/test/java/com/izquierdojl/tolocharadio/feature/servers/ServerListViewModelTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup (T001–T003)
2. Completar Phase 2: Foundational (T004–T017)
3. Completar Phase 3: US1 (T018–T024)
4. **PARAR Y VALIDAR**: Escenarios 1–2 de `quickstart.md` (bienvenida + primer servidor)
5. Demo de arranque sin autenticación

### Incremental Delivery

1. Setup + Foundational → auth retirada, base lista
2. US1 → bienvenida y primer servidor (MVP)
3. US2 → uso diario sin credenciales (sin regresión del reproductor)
4. US3 → gestión multi-servidor sin credenciales
5. Polish → limpieza, notas de versión, gates y validación integral

### Parallel Team Strategy

Con dos personas: tras Setup+Foundational, una toma US1 y otra US2 (coordinar T023 con T029 en `TolochaNavGraph`/arranque). US3 después de US1/US2. El resto de archivos es disjunto.

---

## Notes

- [P] = archivos distintos y sin dependencias pendientes
- La etiqueta [Story] da trazabilidad a `spec.md`
- Verificar que cada test falla antes de implementar (Red-Green)
- Ninguna petición lleva credenciales; nunca `Authorization` ni tokens en URL (FR-011)
- T001 (enmienda constitucional 2.0.0) completado; el resto de tareas puede empezar
- Evitar: tareas vagas, conflictos de archivo y dependencias cruzadas que rompan la independencia de las historias
- Glosario: "servidor" e "instancia" son el mismo concepto; forma canónica: "servidor (instancia)"
