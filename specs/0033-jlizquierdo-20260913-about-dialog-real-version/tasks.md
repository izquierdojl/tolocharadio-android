---

description: "Task list for Diálogo Acerca de con versión real"
---

# Tasks: Diálogo "Acerca de" con versión real y detalles ampliados

**Input**: Design documents from `/specs/0033-jlizquierdo-20260913-about-dialog-real-version/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md

**Tests**: INCLUIDOS — la constitución (Principio III, Test-First NON-NEGOTIABLE) exige tests Red-Green para `ViewModel` y lógica pura. Escribir el test antes de la implementación y verificar que falla (Red) antes de tocar producción (Green).

**Organization**: Tareas agrupadas por historia de usuario para permitir implementación y prueba independientes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo (ficheros distintos, sin dependencias pendientes)
- **[Story]**: US1, US2, US3 (mapea a las historias de `spec.md`)
- Rutas de fichero exactas en cada descripción

## Path Conventions

- App Android de un solo módulo: `app/src/main/java/com/izquierdojl/tolocharadio/`
- Tests unitarios: `app/src/test/java/com/izquierdojl/tolocharadio/`
- Tests instrumentados: `app/src/androidTest/java/com/izquierdojl/tolocharadio/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Crear el value object de metadatos del build y su proveedor Hilt.

- [X] T001 [P] Crear `AppBuildInfo` (data class con `appName`, `versionName`, `versionCode`, `applicationId`, `buildType`, `repositoryUrl`, `developer`, `license`) en `app/src/main/java/com/izquierdojl/tolocharadio/core/util/AppBuildInfo.kt`
- [X] T002 Crear el módulo Hilt `AppInfoModule` con `@Provides fun provideAppBuildInfo(): AppBuildInfo` leyendo `BuildConfig.VERSION_NAME`, `VERSION_CODE`, `APPLICATION_ID`, `DEBUG` y las constantes de proyecto en `app/src/main/java/com/izquierdojl/tolocharadio/di/AppInfoModule.kt` (depende de T001)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Skeleton compartido por las tres historias (entidad `AppInfo`, etiquetas, alias de servidor, mensajes y formateador).

**CRITICAL**: Ninguna historia puede empezar hasta terminar esta fase.

- [X] T003 Extender `AppInfo` con `versionCode`, `applicationId`, `buildType`, `theme`, `startScreen` y `activeServerAlias: String?`; eliminar el `version = "1.0"` fijo y el `AppInfo()` por defecto de `AppInfoUiState.Showing`; actualizar el test existente `AppInfo contiene datos correctos por defecto` que asumía `"1.0"` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModelTest.kt`
- [X] T004 [P] Añadir funciones puras de etiqueta `ThemeMode.label()` ("Sistema"/"Claro"/"Oscuro") y `StartScreen.label()` ("Explorar"/"Favoritos"/"Historial") en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsLabels.kt`
- [X] T005 Inyectar `GetServersUseCase` en `SettingsViewModel` y exponer `activeServerAlias: String?` en `SettingsUi`, derivado del servidor con `isActive = true` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt`
- [X] T006 Añadir `messages: SharedFlow<String>` (`MutableSharedFlow(extraBufferCapacity = 1)`) y `onCopyResult(success: Boolean)` que emite "Información copiada" / "No se pudo copiar la información" en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt`
- [X] T007 [P] Crear la función pura `AppInfo.toClipboardText()` (líneas "Etiqueta: valor", omitiendo la línea "Servidor activo" si `activeServerAlias == null`) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/AppInfoFormat.kt`

**Checkpoint**: Skeleton listo — las historias pueden implementarse.

---

## Phase 3: User Story 1 - Ver la versión real (Priority: P1) MVP

**Goal**: El diálogo muestra la versión real del build en lugar del `1.0` fijo.

**Independent Test**: Instalar con `-PversionName=2.3.1 -PversionCode=42`, abrir Configuración → Acerca de y ver `Versión: 2.3.1`.

### Tests for User Story 1

> Escribir PRIMERO y verificar que FALLA (Red) antes de implementar.

- [X] T008 [US1] Test `showAppInfoDialog usa la versión del build` (mock de `AppBuildInfo` con `versionName = "2.3.1"`; comprueba `version == "2.3.1"`) en `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModelTest.kt`

### Implementation for User Story 1

- [X] T009 [US1] `showAppInfoDialog()` compone `AppInfo` usando `AppBuildInfo.versionName` (nunca un literal) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt` (depende de T008)
- [X] T010 [US1] Mostrar el campo `version` real en el diálogo en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/AppInfoDialog.kt`

**Checkpoint**: US1 funcional y verificable de forma aislada.

---

## Phase 4: User Story 2 - Detalles ampliados y configuración activa (Priority: P2)

**Goal**: Añadir nº de compilación, identificador de la app, tipo de build, tema, pantalla de arranque y alias del servidor activo.

**Independent Test**: Abrir el diálogo con tema "Oscuro", arranque "Favoritos" y un servidor activo y comprobar que todos esos datos aparecen; compilar en debug y release y comprobar el tipo de build.

### Tests for User Story 2

- [X] T011 [P] [US2] Test que comprueba que `AppInfo` incluye `versionCode`, `applicationId`, `buildType`, `theme`, `startScreen` y `activeServerAlias` correctos, verificando `buildType` tanto para "Depuración" como para "Publicación", en `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModelTest.kt`
- [X] T012 [P] [US2] Test de `ThemeMode.label()` y `StartScreen.label()` para todos los valores en `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsLabelsTest.kt`

### Implementation for User Story 2

- [X] T013 [US2] Componer en `showAppInfoDialog()` los metadatos del build (`versionCode`, `applicationId`, `buildType`) y la configuración activa (`theme`, `startScreen`, `activeServerAlias`) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt` (depende de T011, T012)
- [X] T014 [US2] Renderizar en el diálogo los nuevos campos (nº compilación, identificador, tipo de build, tema, pantalla de arranque, servidor activo) omitiendo "Servidor activo" si es null; usar defaults definidos para datos ausentes (`buildType` siempre presente; alias ausente → omitir línea) (FR-007); y **conservar** los campos existentes `developer`, `license` y el enlace interactivo "Ver repositorio" (`repositoryUrl`) con apertura silenciosa ante fallo (FR-003, FR-004) en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/AppInfoDialog.kt`

**Checkpoint**: US1 y US2 funcionan de forma independiente.

---

## Phase 5: User Story 3 - Copiar información para soporte (Priority: P3)

**Goal**: Acción "Copiar información" que vuelca el bloque al portapapeles y confirma con snackbar sin cerrar el diálogo.

**Independent Test**: Abrir el diálogo, pulsar "Copiar información", pegar en otra app y verificar el texto completo; ver el snackbar y que el diálogo sigue abierto.

### Tests for User Story 3

- [X] T015 [P] [US3] Test del formato `toClipboardText()` (todas las etiquetas; sin línea "Servidor activo" si es null) y que el texto **no contiene** credenciales, tokens ni PII (FR-009, SC-006) en `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/AppInfoFormatTest.kt`
- [X] T016 [P] [US3] Test con Turbine de `onCopyResult(true)` → "Información copiada" y `onCopyResult(false)` → "No se pudo copiar la información", y que `appInfoUiState` sigue en `Showing` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModelTest.kt`

### Implementation for User Story 3

- [X] T017 [US3] Sustituir `AlertDialog` por una ventana `Dialog` a pantalla completa (`usePlatformDefaultWidth = false`) con scrim, tarjeta y `SnackbarHost` anclado abajo; añadir el botón "Copiar información" que ejecute `runCatching { clipboard.setText(AnnotatedString(info.toClipboardText())) }` y llame a `onCopyResult(result.isSuccess)` (fallo defensivo) en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/AppInfoDialog.kt` (depende de T010, T014, T015)
- [X] T018 [US3] En `SettingsScreen`, crear `SnackbarHostState`, recoger `messages` en `LaunchedEffect` y mostrar el snackbar; pasar `snackbar` y `onCopy` al diálogo en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt` (depende de T016, T017)

**Checkpoint**: Las tres historias funcionan de forma independiente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cobertura de UI, gates de calidad y validación end-to-end.

- [X] T019 [P] Test instrumentado del flujo crítico (abrir diálogo → pulsar copiar → aparece snackbar y el diálogo sigue abierto), de la presencia del enlace "Ver repositorio" (FR-004) y de la supervivencia del diálogo a la rotación (FR-008, SC-005) en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/AppInfoDialogTest.kt`
- [X] T020 Ejecutar gates y corregir: `.\gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug` (actualizar `detekt-baseline.xml` solo si aparece deuda nueva justificada)
- [X] T021 Ejecutar la validación de `quickstart.md` (VS-1 a VS-6) en un dispositivo/emulador
  *(Cerrada 2026-09-24: validada por el usuario en uso real.)*

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: depende de Setup — BLOQUEA todas las historias
- **User Stories (Phase 3-5)**: dependen de Foundational
- **Polish (Phase 6)**: depende de las historias deseadas

### User Story Dependencies

- **US1 (P1)**: tras Foundational — sin dependencias de otras historias
- **US2 (P2)**: tras Foundational — extiende `AppInfo` (skeleton de Foundational); independientemente testeable
- **US3 (P3)**: tras Foundational — usa `toClipboardText()` (T007) y `onCopyResult` (T006); independientemente testeable

### Within Each User Story

- Tests primero y en rojo
- `SettingsViewModel` antes de `AppInfoDialog`/`SettingsScreen`
- Formateador puro antes del botón de copiar
- **Fichero compartido**: T010 (US1), T014 (US2) y T017 (US3) editan `AppInfoDialog.kt`; deben ejecutarse en ese orden (no son paralelizables entre historias). La independencia de US1–US3 se garantiza a nivel de estado/tests, no de edición simultánea del diálogo.

### Parallel Opportunities

- T001 (Setup) puede ir en paralelo; T002 depende de T001
- T004 y T007 [P] dentro de Foundational
- T011 y T012 [P] (tests US2); T015 y T016 [P] (tests US3)
- T019 [P] en Polish

---

## Parallel Example: User Story 2

```bash
# Tests de US2 en paralelo:
Task: "Test de AppInfo ampliado en app/src/test/.../SettingsViewModelTest.kt"
Task: "Test de etiquetas en app/src/test/.../SettingsLabelsTest.kt"
```

## Parallel Example: User Story 3

```bash
# Tests de US3 en paralelo:
Task: "Test de toClipboardText en app/src/test/.../AppInfoFormatTest.kt"
Task: "Test de onCopyResult con Turbine en app/src/test/.../SettingsViewModelTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 (Setup) → Phase 2 (Foundational)
2. Phase 3 (US1): test rojo T008 → implementación T009/T010
3. **STOP y VALIDAR**: `.\gradlew.bat installDebug -PversionName=2.3.1 -PversionCode=42` y comprobar `Versión: 2.3.1`
4. Desplegar/demostrar si procede

### Incremental Delivery

1. Setup + Foundational
2. US1 → validar (MVP)
3. US2 → validar
4. US3 → validar
5. Polish (T019–T021)

---

## Notes

- [P] = ficheros distintos, sin dependencias pendientes
- Verificar que cada test FALLA antes de implementar (regla Red-Green)
- No commitear sin petición explícita del usuario (AGENTS.md)
- Evitar: tareas vagas, conflictos en el mismo fichero, dependencias cruzadas entre historias
