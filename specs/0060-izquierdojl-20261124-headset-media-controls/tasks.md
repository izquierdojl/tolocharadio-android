# Tasks: Controles de auriculares para pausar y saltar

**Input**: Design documents from `/specs/0060-izquierdojl-20261124-headset-media-controls/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/headset-controls.md`

**Tests**: obligatorios para `domain`, `data`, controladores y ViewModel
(constitución III, regla Red-Green). Se escriben antes de la implementación y
deben fallar. El test Compose cubre el flujo de Ajustes de la historia 3.

**Organization**: tareas agrupadas por historia de usuario para que cada una sea
implementable y verificable por separado.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: paralelizable (ficheros distintos, sin dependencias pendientes)
- **[Story]**: US1, US2, US3
- Rutas exactas en cada tarea

## Path Conventions

- Android, módulo único `app`: `app/src/main/java/com/izquierdojl/tolocharadio/`,
  `app/src/test/java/com/izquierdojl/tolocharadio/` y
  `app/src/androidTest/java/com/izquierdojl/tolocharadio/`.
- Documentación de la feature: `specs/0060-izquierdojl-20261124-headset-media-controls/`.

---

## Phase 1: Setup

**Purpose**: recursos compartidos por las historias 2 y 3.

- [X] T001 [P] Añadir en `app/src/main/res/values/strings.xml` los recursos de notificación con formato para los segundos configurados (`media_seek_back_seconds`, `media_seek_forward_seconds`) y los textos de ayuda de la sección de controles.
- [X] T002 [P] Añadir en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/TolochaTestTags.kt` los tags de test de la sección de controles de auriculares (selector de retroceso, selector de avance y switch).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: modelo, política, persistencia y caché que necesitan US2 y US3.

**⚠️ CRITICAL**: US2 y US3 no pueden empezar hasta completar esta fase. US1 es
una regresión de comportamiento existente y puede validarse en paralelo.

- [X] T003 [P] Escribir el test que falla de `HeadsetControlSettings` para opciones permitidas, defaults, saneado de valores corruptos y derivados en milisegundos, citando `data-model.md HeadsetControlSettings.skipBackSeconds`, `HeadsetControlSettings.skipForwardSeconds` y `HeadsetControlSettings.gesturesEnabled` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/HeadsetControlSettingsTest.kt`.
- [X] T004 Implementar `HeadsetControlSettings`, `DEFAULT`, `OPTIONS_SECONDS` y el saneado para que pase T003, citando `data-model.md HeadsetControlSettings.skipBackSeconds`, `HeadsetControlSettings.skipForwardSeconds` y `HeadsetControlSettings.gesturesEnabled` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/HeadsetControlSettings.kt`.
- [X] T005 [P] Escribir el test que falla de `HeadsetControlPolicy` para la matriz contenido × comando × fuente × ajustes, incluido que `ExplicitControl` no queda bloqueado por `gesturesEnabled`, citando `data-model.md PlaybackContent`, `HeadsetCommand`, `SkipSource` y `HeadsetAction` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/HeadsetControlPolicyTest.kt`.
- [X] T006 Implementar `HeadsetControlPolicy` y los tipos `PlaybackContent`, `HeadsetCommand`, `SkipSource` y `HeadsetAction` para que pase T005, citando `data-model.md PlaybackContent`, `HeadsetCommand`, `SkipSource` y `HeadsetAction` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/HeadsetControlPolicy.kt`.
- [X] T007 [P] Extender `InstancePrefsTest` para las claves nuevas, defaults, valores inválidos y setters, citando `data-model.md HeadsetControlSettings.skipBackSeconds`, `HeadsetControlSettings.skipForwardSeconds`, `HeadsetControlSettings.gesturesEnabled` y las claves de persistencia `headset_skip_back_sec`, `headset_skip_forward_sec` y `headset_skip_enabled` en `app/src/test/java/com/izquierdojl/tolocharadio/data/local/InstancePrefsTest.kt`.
- [X] T008 Añadir a `InstancePrefs` el flujo `headsetControlSettings`, las claves y los setters para que pase T007, citando `data-model.md HeadsetControlSettings.skipBackSeconds`, `HeadsetControlSettings.skipForwardSeconds`, `HeadsetControlSettings.gesturesEnabled` y las claves de persistencia en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/InstancePrefs.kt`.
- [X] T009 [P] Escribir el test que falla de `HeadsetControlSettingsCache` para el valor inicial y las emisiones del flujo, citando `data-model.md HeadsetControlSettingsCache.settings` en `app/src/test/java/com/izquierdojl/tolocharadio/data/local/HeadsetControlSettingsCacheTest.kt`.
- [X] T010 Implementar `HeadsetControlSettingsCache` con `@Singleton`, `@ApplicationScope` y `SharingStarted.Eagerly` para que pase T009, citando `data-model.md HeadsetControlSettingsCache.settings` en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/HeadsetControlSettingsCache.kt`.

**Checkpoint**: modelo, política, preferencias y caché listos; US2 y US3 pueden empezar.

---

## Phase 3: User Story 1 — Pausar y reanudar desde el auricular (Priority: P1) 🎯 MVP

**Goal**: la pulsación corta del botón central pausa y reanuda la radio o el
podcast con la app en primer plano, en segundo plano y con la pantalla bloqueada.

**Independent Test**: ejecutar el Escenario 2 de `quickstart.md` con un auricular
con cable y uno Bluetooth; comprobar pausa/reanudación en app, notificación y
pantalla de bloqueo.

### Validation for User Story 1

- [X] T011 [US1] Validar el comportamiento existente de play/pausa central en el Escenario 2 de `specs/0060-izquierdojl-20261124-headset-media-controls/quickstart.md` con cable, Bluetooth, primer plano, segundo plano y pantalla bloqueada; registrar la evidencia y dejar fijado que el nuevo `onMediaButtonEvent` debe devolver `false` para play/pausa (la prueba de regresión automatizada se cubre en T012). — ASUMIDO sin implementar, aceptado por el usuario al cierre (2026-11-24); validación pendiente en dispositivo real.

**Checkpoint**: US1 validada; Media3 mantiene el play/pausa sin cambios de código.

---

## Phase 4: User Story 2 — Saltar adelante y atrás desde el auricular (Priority: P2)

**Goal**: anterior/siguiente y la pulsación larga de volumen saltan los segundos
configurados en episodios, sin cambiar de episodio y sin afectar a la radio; el
salto funciona también con el episodio pausado.

**Independent Test**: ejecutar los Escenarios 3, 4 y 5 de `quickstart.md`:
volumen corto/largo con cable, anterior/siguiente con Bluetooth, salto en pausa,
radio sin saltos y comandos de notificación/bloqueo.

### Tests for User Story 2 (write first, must fail)

- [X] T012 [P] [US2] Escribir el test que falla de `HeadsetControlsController` para play/pausa delegado (no consumido), anterior/siguiente, repeticiones, episodio pausado, radio, gestos desactivados, fuente `HeadsetGesture` vs `ExplicitControl`, gating de llamada/foco, límites de posición y desconexión/reconexión de auricular, citando `data-model.md HeadsetControlSettings.gesturesEnabled`, `HeadsetControlSettingsCache.settings`, `PlaybackContent`, `HeadsetCommand`, `SkipSource` y `HeadsetAction` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/HeadsetControlsControllerTest.kt`.
- [X] T013 [P] [US2] Escribir el test que falla de `HeadsetVolumeGestureHandler` para pulsación corta, larga de 500 ms, repeticiones, cancelación, app en segundo plano, llamada, pérdida de foco y desconexión/reconexión a mitad de gesto, citando `data-model.md VolumeGestureState.keyCode`, `VolumeGestureState.job`, `VolumeGestureState.longTriggered` y `HeadsetControlSettings.gesturesEnabled` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/HeadsetVolumeGestureHandlerTest.kt`.
- [X] T014 [P] [US2] Extender `MediaButtonPolicyTest` para comprobar que `EPISODE` publica botones de salto incluso con `gesturesEnabled = false` y que `RADIO`/`RADIO_RETRY` nunca los publican, citando `data-model.md MediaButtonLayout` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/MediaButtonPolicyTest.kt`.

### Implementation for User Story 2

- [X] T015 [US2] Implementar `HeadsetControlsController` con `canSkip(source)`, `skipForward(source)`, `skipBack(source)` y `onMediaButton(keyCode, action, repeatCount)`, aplicando el gating de modo de audio/foco y la distinción `HeadsetGesture`/`ExplicitControl`, para que pase T012; citar `data-model.md HeadsetControlSettings.gesturesEnabled`, `HeadsetControlSettingsCache.settings`, `PlaybackContent`, `HeadsetCommand`, `SkipSource` y `HeadsetAction` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/HeadsetControlsController.kt`.
- [X] T016 [US2] Implementar `HeadsetVolumeGestureHandler` con temporizador de 500 ms, volumen corto nativo, salto largo único, cancelación y `handle(keyCode, action, repeatCount, hostResumed)`, para que pase T013; citar `data-model.md VolumeGestureState.keyCode`, `VolumeGestureState.job`, `VolumeGestureState.longTriggered` y `HeadsetControlSettings.gesturesEnabled` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/HeadsetVolumeGestureHandler.kt`.
- [X] T017 [US2] Delegar `MainActivity.dispatchKeyEvent` en `HeadsetVolumeGestureHandler` y cancelar el gesto en pausa/destrucción, citando `data-model.md VolumeGestureState` en `app/src/main/java/com/izquierdojl/tolocharadio/MainActivity.kt`.
- [X] T018 [US2] Añadir `onMediaButtonEvent` a `RadioPlaybackService` para delegar solo `KEYCODE_MEDIA_NEXT`/`PREVIOUS` con `SkipSource.HeadsetGesture`, consumir repeticiones/`ACTION_UP` y devolver `false` para play/pausa, citando `data-model.md HeadsetCommand` y `SkipSource` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`.
- [X] T019 [US2] Añadir `onPlayerCommandRequest` a `RadioPlaybackService` para interceptar los comandos de seek/anterior/siguiente del sistema con `SkipSource.ExplicitControl`, y atender `CUSTOM_COMMAND_SEEK_BACK`/`CUSTOM_COMMAND_SEEK_FORWARD` en `onCustomCommand`, citando `data-model.md HeadsetCommand`, `SkipSource` y `HeadsetAction` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`.
- [X] T020 [US2] Construir los `CommandButton` de episodio con `SessionCommand`, slots `SLOT_BACK`/`SLOT_FORWARD` y nombres dinámicos, y añadir `CUSTOM_COMMAND_SEEK_BACK`/`CUSTOM_COMMAND_SEEK_FORWARD` a los comandos disponibles de `onConnect`, citando `data-model.md MediaButtonLayout`, `HeadsetControlSettings.skipBackSeconds` y `HeadsetControlSettings.skipForwardSeconds` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`.
- [X] T021 [US2] Aplicar `ExoPlayer.setSeekBackIncrementMs` y `setSeekForwardIncrementMs` al player actual y a cada player nuevo cuando cambien los ajustes, citando `data-model.md HeadsetControlSettings.skipBackMs`, `HeadsetControlSettings.skipForwardMs` y `HeadsetControlSettingsCache.settings` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`.
- [X] T022 [US2] Añadir logs estructurados de gesto, contenido y resultado sin PII en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/HeadsetControlsController.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/HeadsetVolumeGestureHandler.kt`, siguiendo `contracts/headset-controls.md` §6.
- [X] T023 [US2] Verificar que `HeadsetControlsController` y `HeadsetVolumeGestureHandler` se inyectan con `@Inject`/`@Singleton`; si falta, añadir en `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt` los providers de `AudioManager` y del scope de hilo principal, citando `data-model.md HeadsetControlSettingsCache.settings`.

**Checkpoint**: US2 funcional con los valores por defecto 15/30; US3 puede empezar.

---

## Phase 5: User Story 3 — Elegir segundos y activar/desactivar gestos (Priority: P3)

**Goal**: Configuración permite elegir 5–60 s y desactivar los gestos del
auricular; los cambios se aplican al siguiente salto, persisten y no desactivan
los botones explícitos del reproductor ni de la notificación.

**Independent Test**: ejecutar los Escenarios 1 y 6 de `quickstart.md`: cambiar
segundos, comprobar aplicación inmediata y persistencia, y desactivar gestos
verificando que los botones en pantalla y notificación siguen saltando.

### Tests for User Story 3 (write first, must fail)

- [X] T024 [P] [US3] Extender `SettingsViewModelTest` para el estado inicial, la colección de la caché y los setters que persisten, citando `data-model.md HeadsetControlSettings.skipBackSeconds`, `HeadsetControlSettings.skipForwardSeconds` y `HeadsetControlSettings.gesturesEnabled` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModelTest.kt`.
- [X] T025 [P] [US3] Extender `EpisodePlayerViewModelTest` para saltos con valores configurables, exposición de `skipBackSeconds`/`skipForwardSeconds` en `EpisodePlayerUi`, límites, episodio pausado y gating `ExplicitControl`, citando `data-model.md HeadsetAction`, `HeadsetControlSettings.skipBackMs`, `HeadsetControlSettings.skipForwardMs` y `SkipSource` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModelTest.kt`.
- [X] T026 [P] [US3] Añadir el test Compose `HeadsetSettingsSectionTest` para valores por defecto, cambio de segundos, switch y comprobación de que los botones de salto explícitos siguen presentes, citando `data-model.md HeadsetControlSettings.skipBackSeconds`, `HeadsetControlSettings.skipForwardSeconds`, `HeadsetControlSettings.gesturesEnabled` y `MediaButtonLayout` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/HeadsetSettingsSectionTest.kt`.

### Implementation for User Story 3

- [X] T027 [US3] Añadir a `SettingsUi` y `SettingsActions` los tres valores/callbacks, coleccionar `HeadsetControlSettingsCache.settings` y persistir los setters, para que pase T024; citar `data-model.md HeadsetControlSettings.skipBackSeconds`, `HeadsetControlSettings.skipForwardSeconds` y `HeadsetControlSettings.gesturesEnabled` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt`.
- [X] T028 [US3] Añadir `HeadsetControlsSection` a `SettingsScreen` con selectores de 5–60 s, switch de gestos, textos de ayuda y semántica accesible, para que pase T026; citar `data-model.md HeadsetControlSettings.skipBackSeconds`, `HeadsetControlSettings.skipForwardSeconds`, `HeadsetControlSettings.gesturesEnabled` y `SkipSource` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt`.
- [X] T029 [US3] Actualizar `EpisodePlayerViewModel.skipBack()`/`skipForward()` para usar los milisegundos configurados, exponer `skipBackSeconds`/`skipForwardSeconds` en `EpisodePlayerUi` desde `HeadsetControlSettingsCache.settings`, limitar a `[0, durationMs]` y comprobar `canSkip(SkipSource.ExplicitControl)`, para que pase T025; citar `data-model.md HeadsetAction`, `HeadsetControlSettings.skipBackSeconds`, `HeadsetControlSettings.skipForwardSeconds`, `HeadsetControlSettings.skipBackMs`, `HeadsetControlSettings.skipForwardMs`, `HeadsetControlSettingsCache.settings` y `SkipSource` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt`.
- [X] T030 [US3] Añadir etiquetas y formato de segundos en `SettingsLabels.kt`, `strings.xml`, `EpisodePlayerControls.kt` y `EpisodeMiniPlayer.kt`; pasar `skipBackSeconds`/`skipForwardSeconds` desde `EpisodePlayerUi` y sustituir los `contentDescription` fijos «Atrás 15 segundos»/«Adelante 30 segundos» por etiquetas con los segundos configurados; actualizar `EpisodeFullPlayerSheet.kt` para pasar los valores a `EpisodePlayerControls` y las aserciones de `EpisodeMiniPlayerTest`/`EpisodeFullPlayerSheetTest`, citando `data-model.md HeadsetControlSettings.skipBackSeconds` y `HeadsetControlSettings.skipForwardSeconds` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsLabels.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/EpisodePlayerControls.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeFullPlayerSheet.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayer.kt` y los tests Compose correspondientes.
- [X] T031 [US3] Reconstruir los botones/labels de la notificación cuando emita `HeadsetControlSettingsCache.settings`, para que el cambio de segundos se refleje sin reiniciar, citando `data-model.md HeadsetControlSettings.skipBackSeconds`, `HeadsetControlSettings.skipForwardSeconds` y `HeadsetControlSettingsCache.settings` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`.
- [X] T032 [US3] Completar la semántica de accesibilidad y los tags de la sección (`contentDescription`, `stateDescription` y `TolochaTestTags`), citando `data-model.md HeadsetControlSettings.gesturesEnabled` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/TolochaTestTags.kt`.

**Checkpoint**: las tres historias funcionan y pueden validarse por separado.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: limpieza y validación final.

- [X] T033 [P] Eliminar los recursos y referencias obsoletas `media_seek_back_15`/`media_seek_forward_30` si ya no se usan en `app/src/main/res/values/strings.xml` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`.
- [X] T034 [P] Revisar y corregir avisos de Detekt/ktlint en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/HeadsetControlsController.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/HeadsetVolumeGestureHandler.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`; si aparece `TooManyFunctions` o `LongMethod`, extraer helpers o justificar la supresión en la PR.
- [X] T035 Ejecutar los gates completos desde la raíz del repositorio (`assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug`), según `.github/workflows/android.yml`, y verificar en el diff que `app/src/main/AndroidManifest.xml` no añade permisos nuevos ni servicios de accesibilidad.
- [X] T036 Ejecutar la matriz manual de `specs/0060-izquierdojl-20261124-headset-media-controls/quickstart.md` con cable, Bluetooth, pantalla bloqueada, radio, episodio pausado, gestos desactivados, Cast si está disponible, desconexión/reconexión, etiquetas del reproductor/notificación y el protocolo de usabilidad del Escenario 10 si se valida SC-009. — ASUMIDO sin implementar, aceptado por el usuario al cierre (2026-11-24); validación pendiente en dispositivo real.
- [X] T037 Revisar los logs de `HeadsetControlsController.kt` y `HeadsetVolumeGestureHandler.kt` contra `contracts/headset-controls.md` §6: sin PII, con tipo de gesto, contenido y resultado.
- [X] T038 Actualizar `specs/0060-izquierdojl-20261124-headset-media-controls/quickstart.md` con los hallazgos finales de dispositivos/OEM y los resultados esperados definitivos, si difieren de lo previsto.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias; puede empezar ya.
- **Foundational (Phase 2)**: depende de Setup; bloquea US2 y US3.
- **US1 (Phase 3)**: no depende de Phase 2; es una regresión del comportamiento
  existente de Media3 y puede validarse en paralelo.
- **US2 (Phase 4)**: depende de Phase 2.
- **US3 (Phase 5)**: depende de Phase 2 y de la implementación del controlador
  de US2 (T015); su UI puede prepararse en paralelo una vez T015 está listo, pero
  la validación completa requiere US2.
- **Polish (Phase 6)**: depende de que las historias deseadas estén completas.

### User Story Dependencies

- **US1 (P1)**: independiente; solo valida que el callback nuevo no rompa
  play/pausa.
- **US2 (P2)**: independiente de US3; usa los defaults 15/30 de Phase 2.
- **US3 (P3)**: depende de US2 para el controlador; configura sus valores y el
  interruptor de gestos.

### Within Each User Story

- Tests primero y deben fallar (Red).
- Implementación después (Green).
- Controlador antes que servicio y UI.
- Story completa y validada antes de pasar a la siguiente prioridad.

### Parallel Opportunities

- T001 y T002 (Setup).
- T003, T005, T007 y T009 (tests de Phase 2).
- T012, T013 y T014 (tests de US2).
- T024, T025 y T026 (tests de US3).
- T033 y T034 (limpieza y revisión estática).

---

## Parallel Example: User Story 2

```bash
# Tests de US2 en paralelo (ficheros distintos):
Task: "T012 HeadsetControlsControllerTest"
Task: "T013 HeadsetVolumeGestureHandlerTest"
Task: "T014 MediaButtonPolicyTest"
```

## Parallel Example: User Story 3

```bash
# Tests de US3 en paralelo (ficheros distintos):
Task: "T024 SettingsViewModelTest"
Task: "T025 EpisodePlayerViewModelTest"
Task: "T026 HeadsetSettingsSectionTest"
```

---

## Implementation Strategy

### MVP First

US1 es el MVP funcional y ya está cubierto por Media3; T011 lo valida. El primer
incremento con valor nuevo es **US1 + US2**: controles de salto con los valores
por defecto 15/30, sin necesidad de la UI de ajustes.

### Incremental Delivery

1. Completar Setup + Foundational.
2. Validar US1 (regresión de play/pausa).
3. Implementar US2 (saltos con defaults) y validar con cable/Bluetooth.
4. Implementar US3 (segundos configurables + interruptor) y validar persistencia
   y botones explícitos.
5. Pulir y ejecutar la matriz de `quickstart.md`.

### Parallel Team Strategy

1. Una persona completa Setup + Foundational.
2. Después:
   - Persona A: tests y controlador de US2.
   - Persona B: UI/ViewModel de US3 una vez T015 esté disponible.
   - Persona C: validación manual de US1 y preparación de la matriz de dispositivos.
3. Integrar y ejecutar los gates en la fase de pulido.

---

## Data Model Traceability

Cada campo restringido de `data-model.md` queda citado en al menos una tarea de
implementación:

| Campo | Tareas |
|-------|--------|
| `HeadsetControlSettings.skipBackSeconds` | T003, T004, T020, T024, T027, T029, T030, T031 |
| `HeadsetControlSettings.skipForwardSeconds` | T003, T004, T020, T024, T027, T029, T030, T031 |
| `HeadsetControlSettings.gesturesEnabled` | T003, T004, T012, T013, T015, T024, T027, T028, T032 |
| `HeadsetControlSettings.skipBackMs` | T021, T025, T029 |
| `HeadsetControlSettings.skipForwardMs` | T021, T025, T029 |
| `HeadsetControlSettingsCache.settings` | T009, T010, T012, T021, T023, T029, T031 |
| `PlaybackContent` | T005, T006, T012, T015 |
| `HeadsetCommand` | T005, T006, T012, T015, T018, T019 |
| `HeadsetAction` | T005, T006, T012, T015, T019, T025, T029 |
| `SkipSource` | T005, T006, T012, T015, T018, T019, T028, T029 |
| `VolumeGestureState.keyCode` | T013, T016 |
| `VolumeGestureState.job` | T013, T016 |
| `VolumeGestureState.longTriggered` | T013, T016 |
| `MediaButtonLayout` | T014, T020, T026 |
| Claves `headset_skip_back_sec`, `headset_skip_forward_sec`, `headset_skip_enabled` | T007, T008 |

---

## Notes

- [P] = ficheros distintos y sin dependencia pendiente.
- [Story] mapea cada tarea a US1, US2 o US3.
- Los tests se escriben antes y deben fallar antes de implementar.
- No se añaden dependencias, permisos ni módulos nuevos.
- La radio nunca publica saltos; el interruptor de US3 solo desactiva los gestos
  del auricular, no los botones explícitos (FR-012, SC-014).
- Evitar tareas vagas o cambios simultáneos en el mismo fichero sin dependencia
  declarada.

---

## Implementation Status (2026-11-24)

- **Completadas**: T001–T010, T012–T035, T037–T038.
- **Cerradas como asumidas**: T011 (botón central) y T036 (matriz de
  `quickstart.md`). No se han verificado en dispositivo real; el usuario las
  validará después del cierre y reabrirá la spec si hay problemas. No hay
  dispositivo Android conectado, por lo que no se ejecutaron las pruebas
  instrumentadas ni la matriz con auricular real.
- **Gates automáticos**: `assembleDebug`, `testDebugUnitTest` (797 tests),
  `detekt`, `ktlintCheck` y `lintDebug` en verde.

