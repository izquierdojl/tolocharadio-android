---
description: "Task list for Credenciales por servidor, auto-login y pantalla unificada de servidor"
---

# Tasks: Credenciales por servidor, auto-login y pantalla unificada de servidor

**Input**: Design documents from `/specs/0024-jlizquierdo-20260912-per-server-credentials/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: INCLUIDOS Y OBLIGATORIOS. La constitución (Principio III, NON-NEGOTIABLE) exige tests unitarios y Red-Green: escribir el test que falla antes de la implementación. Además, Compose UI (androidTest) para los flujos críticos (pantalla unificada de servidor).

**⚠️ GATE DE GOBERNANZA**: la implementación está BLOQUEADA hasta completar **T001** (enmienda constitucional MAJOR 2.0.0 → 3.0.0, incluida a petición del usuario). Ver `plan.md` → Constitution Check y Complexity Tracking.

**Organization**: tareas agrupadas por user story (US1 alta/edición unificada P1; US2 sesión automática sin login P1; US3 corregir credenciales desde el error P2).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: US1/US2/US3 según `spec.md`
- Rutas exactas de archivo en cada tarea

## Path Conventions

- Módulo único `app` (Android/Kotlin). Código: `app/src/main/java/com/izquierdojl/tolocharadio/`
- Tests: `app/src/test/java/com/izquierdojl/tolocharadio/`
- Instrumentados: `app/src/androidTest/java/com/izquierdojl/tolocharadio/`
- Base package: `com.izquierdojl.tolocharadio`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: gobernanza y recursos compartidos

- [X] T001 Aprobar la enmienda constitucional MAJOR 2.0.0 → 3.0.0 (bloqueante): redefinir Principios II y IV (credenciales cifradas por servidor + auto-login JWT/Bearer sin pantallas de login; 401 → refresh/re-login; Reintentar → editar servidor activo), restaurar cifrado/backup en Seguridad, actualizar Sync Impact Report y marcar la spec 0022 como parcialmente superseded — COMPLETADO 2026-09-12 (`.specify/memory/constitution.md` v3.0.0; plan.md a PASS)
- [X] T002 [P] Reintroducir `security-crypto` en `gradle/libs.versions.toml` y `app/build.gradle.kts`
- [X] T003 [P] Excluir `tolocha_tokens` del backup en `app/src/main/res/xml/backup_rules.xml` y `app/src/main/res/xml/data_extraction_rules.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: sesión por servidor, red autenticada y modelo de datos

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase

- [X] T004 [P] Crear `app/src/main/java/com/izquierdojl/tolocharadio/core/session/TokenStore.kt` (credenciales cifradas por servidor: `srv:<id>:email|password|refresh` + `active_server_id`; KDoc de contrato; nunca log)
- [X] T005 [P] Crear `app/src/main/java/com/izquierdojl/tolocharadio/core/session/SessionManager.kt` (token de acceso solo en memoria: `accessTokenNow()`, `setAccess()`, `clear()`, `state`)
- [X] T006 [P] Crear `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/api/AuthApi.kt` (solo `POST auth/login` y `POST auth/refresh`; reutiliza `OkResult` existente)
- [X] T007 Crear `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/AuthRepo.kt` (login/refresh → `SessionManager` + `TokenStore`) — depende de T004/T005/T006
- [X] T008 [P] Crear `app/src/main/java/com/izquierdojl/tolocharadio/core/network/AuthInterceptor.kt` (`Authorization: Bearer` cuando hay token) — depende de T005
- [X] T009 Crear `app/src/main/java/com/izquierdojl/tolocharadio/core/network/TokenAuthenticator.kt` (401 → refresh single-flight + reintento único; si falla, re-login con credenciales guardadas; si falla, error de credenciales) — depende de T006/T007
- [X] T010 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/core/network/RetrofitFactory.kt` y `di/NetworkModule.kt` (wiring de `SessionManager`, `TokenStore`, `AuthInterceptor`, `TokenAuthenticator`, `AuthApi`) — depende de T008/T009
- [X] T011 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/core/network/ApiError.kt` (401/403 → error de credenciales accionable con destino "editar servidor")
- [X] T012 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/domain/servers/SavedServer.kt` (+`hasCredentials`) y `data/repo/servers/ServerRepository.kt` (credenciales en `TokenStore`; alta/edición/cambio con login; `hasCredentials`) — depende de T004/T007
- [X] T013 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/domain/servers/AddServerUseCase.kt` (email/password + validación de login) y crear `domain/servers/UpdateServerUseCase.kt` — depende de T012
- [X] T014 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/domain/servers/StartupGate.kt` (`NoServers`/`NeedsCredentials`/`Ready`) — depende de T012
- [X] T015 [P] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerDataSourceFactory.kt` y `di/PlayerModule.kt` (Bearer en el datasource: proxy y subrecursos HLS) — depende de T005
- [X] T016 Modificar `app/src/main/java/com/izquierdojl/tolocharadio/MainActivity.kt` (bootstrap: auto-login del servidor activo/por defecto) — depende de T007/T014

**Checkpoint**: sesión por servidor lista; US1, US2 y US3 pueden empezar

---

## Phase 3: User Story 1 - Alta y edición unificada de servidor con credenciales (Priority: P1) 🎯 MVP

**Goal**: una sola pantalla (URL + alias + email + contraseña) para bienvenida y Servidores, con alta, edición (contraseña enmascarada) y validación con login.

**Independent Test**: datos borrados → bienvenida pide URL+alias+email+contraseña; añadir servidor válido entra al contenido; desde Servidores, añadir otro y editar el primero con el mismo formulario. Ver `quickstart.md` Escenarios 1, 2, 5 y 8.

### Tests for User Story 1 ⚠️ (escribir primero, deben FALLAR)

- [X] T017 [P] [US1] Reescribir `app/src/test/java/com/izquierdojl/tolocharadio/servers/domain/usecase/AddServerUseCaseTest.kt` y crear `.../servers/domain/usecase/UpdateServerUseCaseTest.kt` (email/password; validación; login fallido no persiste)
- [X] T018 [P] [US1] Reescribir `app/src/test/java/com/izquierdojl/tolocharadio/servers/data/repo/ServerRepositoryTest.kt` (credenciales por servidor, `hasCredentials`, alta con login, edición sin perder contraseña)
- [X] T019 [P] [US1] Escribir `app/src/test/java/com/izquierdojl/tolocharadio/feature/servers/ServerFormViewModelTest.kt` (Add/Edit, validación, contraseña enmascarada conservada)
- [X] T020 [P] [US1] Escribir androidTest `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/servers/ServerFormScreenTest.kt` (formulario con URL/alias/email/contraseña; validación bloqueante)

### Implementation for User Story 1

- [X] T021 [US1] Crear `app/src/main/java/com/izquierdojl/tolocharadio/feature/servers/ServerFormScreen.kt` (pantalla unificada de alta/edición)
- [X] T022 [US1] Crear `app/src/main/java/com/izquierdojl/tolocharadio/feature/servers/ServerFormViewModel.kt` (modos Add/Edit, validación, guardado y re-login) — hacer pasar T019
- [X] T023 [US1] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/feature/servers/ServerListScreen.kt` y `ServerListViewModel.kt` (icono Editar por tarjeta; estado de credenciales) — depende de T012
- [X] T024 [US1] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/Routes.kt` y `TolochaNavGraph.kt` (ruta `SERVER_FORM` add/edit; bienvenida y arranque `NeedsCredentials` usan el formulario) — depende de T014/T021/T022
- [X] T025 [US1] Eliminar `feature/onboarding/InstanceSetupScreen.kt`, `InstanceSetupViewModel.kt` y `app/src/test/java/.../feature/onboarding/InstanceSetupViewModelTest.kt` (sustituidos por el formulario unificado) — depende de T024
- [X] T026 [US1] Validar manualmente los Escenarios 1, 2, 5 y 8 de `quickstart.md` (bienvenida con credenciales, alta, edición, borrado) en emulador/dispositivo — requiere T001 — verificado manualmente en dispositivo real por el usuario (2026-09-12)

**Checkpoint**: alta/edición unificada operativa — MVP demostrable

---

## Phase 4: User Story 2 - Sesión automática por servidor, sin pantallas de login (Priority: P1)

**Goal**: login automático con las credenciales del servidor, refresh transparente, Bearer en REST y reproducción (incl. HLS), sin pantallas de login.

**Independent Test**: con credenciales válidas, Favoritos/Historial cargan y la emisora reproduce sin pedir nada; token caducado → renovación transparente; cambio de servidor → sesión del nuevo. Ver `quickstart.md` Escenarios 3, 6 y 9.

### Tests for User Story 2 ⚠️ (escribir primero, deben FALLAR)

- [X] T027 [P] [US2] Escribir `app/src/test/java/com/izquierdojl/tolocharadio/core/session/SessionManagerTest.kt` (token en memoria, clear, estado)
- [X] T028 [P] [US2] Escribir `app/src/test/java/com/izquierdojl/tolocharadio/core/network/AuthInterceptorTest.kt` (Bearer presente/ausente)
- [X] T029 [P] [US2] Escribir `app/src/test/java/com/izquierdojl/tolocharadio/core/network/TokenAuthenticatorTest.kt` (refresh single-flight, reintento único, re-login con credenciales, fallo → error)
- [X] T030 [P] [US2] Escribir `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/AuthRepoTest.kt` (login/refresh y errores)

### Implementation for User Story 2

- [X] T031 [US2] Integrar el auto-login en el cambio de servidor (`ServerRepository.switchTo`) y en el arranque (`MainActivity`/`SessionManager`) — hacer pasar T027–T030
- [X] T032 [US2] Verificar Cast con la misma fuente autenticada (`app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` + `feature/player/PlayerDataSourceFactory.kt`) incl. subrecursos HLS
- [X] T033 [US2] Mostrar estado de autenticación y errores accionables en las secciones autenticadas (sin pantallas de login) — depende de T011
- [X] T034 [US2] Validar manualmente los Escenarios 3, 6 y 9 de `quickstart.md` (uso sin credenciales, cambio de servidor, seguridad de tokens) — verificado manualmente en dispositivo real por el usuario (2026-09-12)

**Checkpoint**: sesión automática y reproducción autenticada

---

## Phase 5: User Story 3 - Corregir credenciales desde el error (Priority: P2)

**Goal**: si el error es de credenciales, Reintentar abre la edición del servidor activo; si es de red, reintenta.

**Independent Test**: con contraseña incorrecta, Favoritos/Historial ofrecen "Editar servidor"; corregir y guardar recarga los datos. Ver `quickstart.md` Escenario 4.

### Tests for User Story 3 ⚠️ (escribir primero, deben FALLAR)

- [X] T035 [P] [US3] Escribir `app/src/test/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModelTest.kt` (error de credenciales → evento editar servidor; error de red → retry)
- [X] T036 [P] [US3] Escribir `app/src/test/java/com/izquierdojl/tolocharadio/feature/history/HistoryViewModelTest.kt` (idem)

### Implementation for User Story 3

- [X] T037 [US3] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/CommonUi.kt` (`ErrorBanner` con acción primaria configurable: "Editar servidor" vs "Reintentar")
- [X] T038 [US3] Modificar `feature/favorites/FavoritesScreen.kt`/`FavoritesViewModel.kt` y `feature/history/HistoryScreen.kt`/`HistoryViewModel.kt` para enrutar el error de credenciales a editar el servidor activo — hacer pasar T035/T036
- [X] T039 [US3] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` (navegación desde el error a `ServerForm(edit, servidor activo)`) — depende de T024/T038
- [X] T040 [US3] Validar manualmente el Escenario 4 de `quickstart.md` (credenciales incorrectas → editar servidor → recarga) — verificado manualmente en dispositivo real por el usuario (2026-09-12)

**Checkpoint**: recuperación de credenciales sin pantallas de login

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: documentación, gobernanza, gates y validación integral

- [X] T041 [P] Actualizar `README.md` (auth por servidor, credenciales cifradas, instancia con login)
- [X] T042 [P] Marcar la spec 0022 como parcialmente superseded por la 0024 en `specs/0022-jlizquierdo-20260912-server-only-access/spec.md` (nota de gobernanza)
- [X] T043 Verificar con búsqueda global 0 credenciales en logs/URL y que el token de acceso no se persiste (SC-005)
- [X] T044 [P] Actualizar `detekt-baseline.xml` si el cambio introduce/retira entradas (ejecutar `./gradlew detektBaseline` solo si hace falta)
- [X] T045 [P] Añadir test instrumentado del arranque bloqueante por credenciales en `app/src/androidTest/java/com/izquierdojl/tolocharadio/domain/servers/ServerStartupGateTest.kt`
- [X] T046 Ejecutar `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` y `./gradlew connectedDebugAndroidTest`, corrigiendo cualquier error o warning nuevo
- [X] T047 Validar el `quickstart.md` completo (Escenarios 0–9) en el entorno real del usuario y cerrar la spec — verificado manualmente en dispositivo real por el usuario (2026-09-12)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 (enmienda) BLOQUEA toda la implementación; T002/T003 en paralelo
- **Foundational (Phase 2)**: depende de Setup; bloquea todas las user stories
- **US1 (Phase 3)**: depende de Foundational (T012/T014) y de T021/T022
- **US2 (Phase 4)**: depende de Foundational (T005–T016) y de T001
- **US3 (Phase 5)**: depende de US1 (T024) para poder navegar a la edición
- **Polish (Phase 6)**: depende de US1–US3

### User Story Dependencies

- **US1**: sin dependencias de otras historias (tras Foundational)
- **US2**: sin dependencias de US1 (tras Foundational); comparte `ServerRepository` (T012/T031: coordinar)
- **US3**: depende de US1 (destino `ServerForm`) y de US2 (errores de credenciales)

### Within Each User Story

- Tests PRIMERO y en rojo antes de implementar (constitución III)
- Modelos/almacén antes de servicios; servicios antes de UI
- Cada historia termina con su validación manual de `quickstart.md`
- No commitear sin que lo pida el usuario (el hook `speckit.git.commit` es opcional)

### Parallel Opportunities

- Setup: T002 y T003 en paralelo (T001 bloqueante)
- Foundational: T004–T006 y T008/T015 en paralelo; T007, T009–T014 y T016 en secuencia según dependencias
- US1 tests: T017, T018, T019 y T020 en paralelo
- US2 tests: T027, T028, T029 y T030 en paralelo
- US3 tests: T035 y T036 en paralelo
- Polish: T041, T042, T044 y T045 en paralelo

---

## Parallel Example: User Story 1

```bash
# Tests de US1 en paralelo (archivos distintos):
Task: "Reescribir AddServerUseCaseTest y crear UpdateServerUseCaseTest en app/src/test/java/com/izquierdojl/tolocharadio/servers/domain/usecase/"
Task: "Reescribir ServerRepositoryTest en app/src/test/java/com/izquierdojl/tolocharadio/servers/data/repo/ServerRepositoryTest.kt"
Task: "Escribir ServerFormViewModelTest en app/src/test/java/com/izquierdojl/tolocharadio/feature/servers/ServerFormViewModelTest.kt"
Task: "Escribir ServerFormScreenTest en app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/servers/ServerFormScreenTest.kt"
```

## Parallel Example: User Story 2

```bash
# Tests de US2 en paralelo (archivos distintos):
Task: "Escribir SessionManagerTest en app/src/test/java/com/izquierdojl/tolocharadio/core/session/SessionManagerTest.kt"
Task: "Escribir AuthInterceptorTest en app/src/test/java/com/izquierdojl/tolocharadio/core/network/AuthInterceptorTest.kt"
Task: "Escribir TokenAuthenticatorTest en app/src/test/java/com/izquierdojl/tolocharadio/core/network/TokenAuthenticatorTest.kt"
Task: "Escribir AuthRepoTest en app/src/test/java/com/izquierdojl/tolocharadio/data/repo/AuthRepoTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup (T001–T003), con la **enmienda constitucional** primero
2. Completar Phase 2: Foundational (T004–T016)
3. Completar Phase 3: US1 (T017–T026)
4. **PARAR Y VALIDAR**: Escenarios 1, 2, 5 y 8 de `quickstart.md`
5. Demo de alta/edición unificada con credenciales

### Incremental Delivery

1. Setup + Foundational → sesión por servidor y red autenticada
2. US1 → pantalla unificada (MVP)
3. US2 → auto-login y reproducción autenticada
4. US3 → recuperación de credenciales desde el error
5. Polish → documentación, gobernanza, gates y validación integral

### Parallel Team Strategy

Con dos personas: tras Setup+Foundational, una toma US1 y otra US2 (coordinar T012/T031 en `ServerRepository`). US3 después de US1. El resto de archivos es disjunto.

---

## Notes

- [P] = archivos distintos y sin dependencias pendientes
- La etiqueta [Story] da trazabilidad a `spec.md`
- Verificar que cada test falla antes de implementar (Red-Green)
- Credenciales nunca en logs ni en URL; token de acceso solo en memoria; contraseña/refresh cifrados
- La implementación NO puede empezar hasta T001 (enmienda constitucional 3.0.0)
- Evitar: tareas vagas, conflictos de archivo y dependencias cruzadas que rompan la independencia de las historias

---

## Phase 7: Convergence

**Origen**: `/speckit.converge` 2026-09-20 — 14 FR, 7 SC, 3 historias, 11 clarificaciones y 5 principios de constitución revisados contra el código. Sin hallazgos funcionales en la vía local (auto-login, refresh único, formulario unificado, arranque bloqueante, `Bearer` en proxy+HLS, `tolocha_tokens` fuera de backup, sin pantallas de login); el único hueco es la anotación de la excepción de Chromecast.

- [ ] T048 Anotar la excepción de Chromecast ya registrada en `specs/0021-jlizquierdo-20260912-proxy-only-playback/contracts/proxy-playback.md` §9 (bug 0026) en `specs/0024-jlizquierdo-20260912-per-server-credentials/spec.md`: marcar en FR-004 y en la clarificación "el player/Cast vuelven a inyectar el token" que Chromecast usa la URL pública (el receptor no puede enviar `Authorization` → 401) con cross-ref a 0021 §9 y a `createForCast`/`castUriFor`; corregir la afirmación de T032 en `tasks.md`; y fijar en 0021 §9 o en `.specify/bugs/0026-*/assessment.md` la fecha de revisión de la deuda (Constitución, Governance: excepciones con issue y fecha de revisión, máx. 2 sprints) per FR-004 + Constitución II (contradicts) — MEDIUM
