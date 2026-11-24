# Implementation Plan: Controles de auriculares para pausar y saltar

**Branch**: `0060-izquierdojl-20261124-headset-media-controls` | **Date**: 2026-11-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0060-izquierdojl-20261124-headset-media-controls/spec.md`

## Summary

Se añade una capa de controles de auricular sobre la `MediaSession` que ya
existe:

- La pulsación central mantiene el play/pausa actual de Media3 (no se
  reimplementa).
- Los botones anterior/siguiente de los auriculares saltan los segundos
  configurados en episodios, en lugar de cambiar de episodio. En radio en
  directo se ignoran.
- La pulsación larga de volumen (≥ 0,5 s, solo con la app visible y en
  auriculares que entregan las teclas a la app) salta hacia atrás/adelante.
  En Bluetooth con volumen absoluto del sistema se documenta la limitación y se
  usan anterior/siguiente.
- Configuración gana una sección de controles de auriculares: 5–60 s de salto
  (15/30 por defecto) y un interruptor para desactivar los gestos del auricular;
  los botones de salto en pantalla y notificación siguen activos.

La implementación es event-driven, sin polling ni dependencias nuevas. Se
reutilizan Media3 1.11.0, DataStore, Hilt y el `MediaSessionService` actual.
El alcance incluye la notificación y la pantalla de bloqueo mediante comandos
de la sesión; la radio en directo nunca publica saltos.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17

**Primary Dependencies**: AndroidX Media3 1.11.0 (`media3-exoplayer`,
`media3-session`, `media3-cast`), Compose Material 3 (BOM 2026.05.00), Hilt
2.60.1, DataStore 1.1.1, Coroutines/Flow. Sin dependencias nuevas.

**Storage**: DataStore Preferences (`InstancePrefs`) para los segundos de salto
y el interruptor de gestos. Sin cambios en Room ni en el contrato del backend.

**Testing**: JUnit 4, `kotlinx-coroutines-test`, Turbine y MockK para
`domain`/`data`/ViewModels/controladores; test Compose de la sección de
Ajustes; matriz manual de dispositivos para teclas físicas (no hay un simulador
fiable de botones de auricular).

**Target Platform**: Android 26–37 (`minSdk 26`, `compileSdk`/`targetSdk` 37)

**Project Type**: Aplicación móvil Android, módulo Gradle único (`app`)

**Performance Goals**:
- Respuesta visible a un control ≤ 500 ms (FR-003, SC-003).
- Un único salto por gesto; sin cadenas de saltos por eventos repetidos
  (FR-011).
- Sin polling ni wakelocks nuevos; el temporizador de volumen solo vive durante
  una pulsación.

**Constraints**:
- La pulsación larga de volumen solo se captura con la app visible
  (`MainActivity.dispatchKeyEvent`); en segundo plano el volumen es nativo. No se
  detecta el tipo de salida: aplica a cualquier tecla de volumen que llegue a la
  Activity, con o sin auricular.
- Sin permisos nuevos y sin servicio de accesibilidad (FR-015, non-goal).
- Los auriculares Bluetooth que gestionan el volumen de forma absoluta no
  entregan la pulsación a la app; se documenta y se usa anterior/siguiente
  (FR-004).
- La radio en directo no permite saltos; los gestos se ignoran (FR-009).
- La feature opera con el `MediaSessionService`/sesión vivos; no se añade
  `MediaButtonReceiver` ni playback resumption para no arrancar audio
  automáticamente (FR-013).
- Solo Media3 para reproducción y sesión; prohibido `MediaPlayer`/`VideoView`.
- Logs estructurados sin PII ni nombres de emisora (FR-018).
- Sin autoplay cuando no hay contenido activo (FR-013).

**Scale/Scope**: Un usuario y una reproducción activa a la vez; 7 opciones de
segundos (5, 10, 15, 20, 30, 45, 60); 3 preferencias nuevas; una sección de
Ajustes; tres puntos de entrada (teclas multimedia, teclas de volumen y
comandos de la sesión).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Gate | Estado |
|-----------|------|--------|
| I. MVVM + Clean | Política pura en `domain/playback`; controladores/gestos en `feature/player`; UI en `feature/settings`; ViewModel sin `Context`; Hilt | PASS |
| II. Kotlin/Compose/Media3 | `MediaSession.Callback`, `Player` y `ExoPlayer` de Media3; Ajustes en Compose M3; sin XML ni `MediaPlayer` | PASS |
| III. Test-First | Tests de política, preferencias, controlador, gesto de volumen, ViewModels y sección Compose; regla Red-Green; gates obligatorios | PASS |
| IV. Streaming robusto | Sin autoplay; se respeta pausa, error, buffering, llamadas y foco; degradación silenciosa en radio/Bluetooth; logs sin PII | PASS |
| V. Simplicidad (YAGNI) | Sin dependencias, módulos, permisos ni multi-tap; una sola sección de Ajustes; reutiliza `MediaSessionService` y DataStore | PASS |

**Post-design re-check**: se mantiene PASS. No hay violaciones que justificar
en `Complexity Tracking`.

## Project Structure

### Documentation (this feature)

```text
specs/0060-izquierdojl-20261124-headset-media-controls/
├── plan.md                    # Este archivo
├── research.md                # Decisiones R1–R8
├── data-model.md              # Entidades, validaciones y estados
├── quickstart.md              # Validación automática y manual
├── contracts/
│   └── headset-controls.md    # Contrato interno de teclas, volumen y ajustes
├── checklists/
│   └── requirements.md        # Calidad de la spec (16/16)
└── tasks.md                   # generado por /speckit.tasks
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── domain/playback/
│   ├── HeadsetControlSettings.kt        # nuevo: opciones, defaults, validación
│   └── HeadsetControlPolicy.kt          # nuevo: contenido + comando + fuente + ajustes → acción
├── data/local/
│   ├── InstancePrefs.kt                 # + 3 claves y setters de controles
│   └── HeadsetControlSettingsCache.kt   # nuevo: StateFlow global de ajustes
├── feature/player/
│   ├── HeadsetControlsController.kt     # nuevo: ejecuta saltos y mapea teclas
│   ├── HeadsetVolumeGestureHandler.kt   # nuevo: corta/larga de volumen y volumen nativo
│   ├── EpisodePlayerControls.kt         # + etiquetas de salto configurables
│   ├── RadioPlaybackService.kt          # + onMediaButtonEvent, comandos y botones dinámicos
│   └── PlaybackPlayerFactory.kt         # sin cambios de API; el servicio aplica incrementos
├── feature/podcasts/
│   ├── EpisodePlayerViewModel.kt        # + segundos configurables en skipBack/Forward
│   └── player/
│       ├── EpisodeFullPlayerSheet.kt    # + pasa segundos a EpisodePlayerControls
│       └── EpisodeMiniPlayer.kt         # + contentDescription configurable
├── feature/settings/
│   ├── SettingsViewModel.kt             # + estado y setters de controles
│   ├── SettingsScreen.kt                # + sección Controles de auriculares
│   └── SettingsLabels.kt                # + etiquetas de segundos
├── core/ui/components/
│   └── TolochaTestTags.kt               # + tags de la sección de controles
├── MainActivity.kt                      # + dispatchKeyEvent delegando al gesto de volumen
├── di/PlayerModule.kt                   # + provides de controller/handler/cache si aplica
└── res/values/strings.xml               # + etiquetas de notificación con segundos

app/src/test/java/com/izquierdojl/tolocharadio/
├── domain/playback/
│   ├── HeadsetControlSettingsTest.kt    # nuevo
│   └── HeadsetControlPolicyTest.kt      # nuevo
├── data/local/
│   ├── InstancePrefsTest.kt             # + defaults, persistencia y saneado
│   └── HeadsetControlSettingsCacheTest.kt # nuevo
├── feature/player/
│   ├── HeadsetControlsControllerTest.kt # nuevo
│   ├── HeadsetVolumeGestureHandlerTest.kt # nuevo
│   └── MediaButtonPolicyTest.kt         # + episodio publica saltos, radio no
├── feature/podcasts/
│   └── EpisodePlayerViewModelTest.kt    # + saltos configurables, gating y pausa
└── feature/settings/
    └── SettingsViewModelTest.kt         # + estado y setters

app/src/androidTest/java/com/izquierdojl/tolocharadio/
├── feature/podcasts/player/
│   ├── EpisodeMiniPlayerTest.kt         # + labels configurables
│   └── EpisodeFullPlayerSheetTest.kt    # + labels configurables
└── feature/settings/
    └── HeadsetSettingsSectionTest.kt    # nuevo: sección Compose
```

**Structure Decision**: se mantiene el módulo único `app` y la separación
`ui → domain → data` de la constitución. La política de decisión es pura y vive
en `domain/playback`; los puntos de entrada Android (teclas, AudioManager,
MediaSession) viven en `feature/player`; la persistencia en `data/local`; la UI
en `feature/settings`.

## Implementation Approach

### Fase A — Modelo y política (dominio)

1. Crear `HeadsetControlSettings` con:
   - `skipBackSeconds` (default 15) y `skipForwardSeconds` (default 30), ambos
     restringidos a `5, 10, 15, 20, 30, 45, 60`.
   - `gesturesEnabled` (default `true`).
   - Derivados `skipBackMs`/`skipForwardMs` y saneado de valores corruptos.
2. Crear `HeadsetControlPolicy` pura:
   - Entradas: `PlaybackContent` (`Idle`, `LiveRadio`, `EpisodePlaying`,
     `EpisodePaused`), `HeadsetCommand` (`SkipBack`, `SkipForward`, `Next`,
     `Previous`), `SkipSource` (`HeadsetGesture`, `ExplicitControl`) y
     `HeadsetControlSettings`.
   - Salidas: `HeadsetAction.Skip(deltaMs)` o `HeadsetAction.Ignore`.
   - Reglas: solo episodios activos; radio/idle → `Ignore`; `Next` ≡
     `SkipForward`, `Previous` ≡ `SkipBack`; `gesturesEnabled = false` solo
     bloquea `SkipSource.HeadsetGesture`, no los controles explícitos
     (reproductor, notificación, bloqueo).
3. Tests Red-Green de opciones, defaults, saneado y matriz de política.

### Fase B — Persistencia y caché

1. Añadir a `InstancePrefs`:
   - `headsetControlSettings: Flow<HeadsetControlSettings>`.
   - `setHeadsetSkipBackSeconds`, `setHeadsetSkipForwardSeconds`,
     `setHeadsetSkipGesturesEnabled`.
   - Claves: `headset_skip_back_sec`, `headset_skip_forward_sec`,
     `headset_skip_enabled`.
2. Crear `HeadsetControlSettingsCache` (`@Singleton`, `@ApplicationScope`) que
   expone `StateFlow<HeadsetControlSettings>` con `SharingStarted.Eagerly` y
   `HeadsetControlSettings.DEFAULT` como valor inicial.
3. Tests de defaults, persistencia, valores inválidos y emisión del `StateFlow`.

### Fase C — Controlador de saltos y sesión multimedia

1. Crear `HeadsetControlsController` (`@Singleton`, hilo principal):
   - `canSkip(source: SkipSource)`: hay episodio activo en estado `Active`
     (reproduciendo o pausado), el modo de audio es
     `AudioManager.MODE_NORMAL` y el player no está suprimido por pérdida de
     foco. El modo normal cubre llamadas, ringtone y comunicación. Si
     `source == HeadsetGesture`, además exige `gesturesEnabled`.
   - `skipForward(source)`/`skipBack(source)`: si `canSkip(source)`, calcula la
     posición objetivo con `episodeController.positionMs()` y
     `state.durationMs`, limita a `[0, durationMs]` y llama a
     `episodeController.seekTo`. No cambia `playWhenReady`.
   - `onMediaButton(keyCode: Int, action: Int, repeatCount: Int): Boolean` para
     `KEYCODE_MEDIA_NEXT`/`KEYCODE_MEDIA_PREVIOUS`:
     - `ACTION_DOWN` con `repeatCount == 0`: ejecuta el salto con
       `SkipSource.HeadsetGesture` y consume.
     - Repeticiones y `ACTION_UP`: consume sin acción.
     - Otras teclas: devuelve `false` para que Media3 mantenga play/pausa.
2. `RadioPlaybackService`:
   - `onMediaButtonEvent`: delega en el controlador solo para anterior/siguiente
     con `SkipSource.HeadsetGesture`; play/pausa sigue el camino nativo de
     Media3.
   - `onPlayerCommandRequest`: intercepta `COMMAND_SEEK_BACK`,
     `COMMAND_SEEK_FORWARD`, `COMMAND_SEEK_TO_NEXT`,
     `COMMAND_SEEK_TO_NEXT_MEDIA_ITEM`, `COMMAND_SEEK_TO_PREVIOUS` y
     `COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM`; ejecuta el salto configurado con
     `SkipSource.ExplicitControl` y devuelve `SessionError.INFO_CANCELLED` para
     suprimir el comportamiento por defecto. El resto de comandos sigue con
     `RESULT_SUCCESS`.
   - `onConnect` añade `CUSTOM_COMMAND_SEEK_BACK`/`CUSTOM_COMMAND_SEEK_FORWARD`
     a los comandos de sesión disponibles.
   - `onCustomCommand` atiende esos comandos desde la notificación con
     `SkipSource.ExplicitControl`.
   - `updateMediaButtons`: construye los botones de episodio con
     `SessionCommand` y nombres dinámicos («Retroceder N s», «Avanzar N s»),
     slots `SLOT_BACK`/`SLOT_FORWARD`; en radio no se añaden saltos.
   - Colección de `HeadsetControlSettingsCache.settings` para reconstruir
     botones y aplicar `ExoPlayer.setSeekBackIncrementMs`/
     `setSeekForwardIncrementMs` al player actual y a cada player nuevo.
3. Mantener `MediaButtonPolicy` como dueño de la decisión de layout
   (`RADIO`, `RADIO_RETRY`, `EPISODE`); añadir tests de que episodio con
   ajustes válidos publica saltos (también con `gesturesEnabled = false`) y
   radio nunca.
4. Tests del controlador: dedupe de repeticiones, pausa, radio, gestos
   desactivados (solo gestos; notificación/UI siguen saltando), límites
   0/duración, Cast, comandos de sesión, gating de llamada/foco y
   desconexión/reconexión de auricular.
5. **US1 (play/pausa central)**: no requiere código nuevo; el callback solo
   consume anterior/siguiente y devuelve `false` para play/pausa, que sigue en
   Media3. La prueba de regresión automatizada se añade en T012 y la validación
   manual en T011.

### Fase D — Pulsación larga de volumen

1. Crear `HeadsetVolumeGestureHandler` (`@Singleton`, dispatcher Main):
   - `handle(keyCode: Int, action: Int, repeatCount: Int, hostResumed: Boolean):
     Boolean`.
   - Solo captura `VOLUME_UP`/`VOLUME_DOWN` si `hostResumed` y
     `HeadsetControlsController.canSkip(SkipSource.HeadsetGesture)` (episodio
     activo, gestos activos, modo de audio normal y sin supresión por foco).
   - `ACTION_DOWN` inicial: consume y lanza un temporizador de 500 ms.
   - Al cumplirse 500 ms: ejecuta un único `skipForward`/`skipBack` con
     `SkipSource.HeadsetGesture` y marca el gesto como largo.
   - `ACTION_UP` antes del umbral: ajusta el volumen del sistema una vez con
     `AudioManager.adjustStreamVolume(STREAM_MUSIC, ..., FLAG_SHOW_UI)` y
     consume.
   - Repeticiones y `ACTION_UP` tras un gesto largo: consume sin acción.
   - `cancel()` en pausa/destrucción de la Activity; si no, no hay efectos.
2. `MainActivity.dispatchKeyEvent`: delega en el handler y devuelve su
   resultado; si no captura, el sistema gestiona el volumen.
3. Tests con `runTest`: corta pulsación ajusta volumen, larga salta una vez,
   fondo/llamada/foco/gestos desactivados no capturan, cancelación,
   desconexión/reconexión y límites.

### Fase E — Ajustes y reproductor de episodios

1. `SettingsUi`/`SettingsActions`/`SettingsViewModel`: añadir los tres valores,
   colección desde `HeadsetControlSettingsCache` y setters que persisten.
2. `SettingsScreen`: nueva `HeadsetControlsSection` después de Reproducción:
   - Dos selectores de segundos (dropdown o fila de opciones) con etiquetas
     «5 s»…«60 s».
   - Interruptor «Usar gestos de salto en el auricular».
   - Texto de ayuda: solo app visible; en Bluetooth usar anterior/siguiente;
     la radio en directo no se puede saltar; el interruptor solo afecta a los
     gestos del auricular, no a los botones en pantalla ni a la notificación.
   - `contentDescription`, `stateDescription` y test tags.
3. `EpisodePlayerViewModel`: colecciona los ajustes, expone
   `skipBackSeconds`/`skipForwardSeconds` en `EpisodePlayerUi` y, antes de
   saltar, comprueba `HeadsetControlsController.canSkip(SkipSource.ExplicitControl)`;
   usa `skipBackMs`/`skipForwardMs` en `skipBack()`/`skipForward()`, limitando
   a `[0, durationMs]`. `EpisodeFullPlayerSheet` pasa esos valores a
   `EpisodePlayerControls`; `EpisodePlayerControls` y `EpisodeMiniPlayer` usan
   `contentDescription` formateados.
4. Añadir strings de notificación con formato (`Retroceder %1$d segundos`,
   `Avanzar %1$d segundos`) y etiquetas de Ajustes.
5. Test Compose de la sección: valores por defecto, cambio de segundos,
   interruptor, botones de salto que siguen visibles/activos al desactivar y
   accesibilidad mínima.

### Fase F — Validación y gates

1. Ejecutar la matriz de tests unitarios y el test Compose.
2. Ejecutar `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck
   lintDebug`.
3. Completar la validación manual de `quickstart.md` con cable y Bluetooth.
4. Revisar logs: sin PII, con tipo de gesto y resultado.
5. Verificar que `app/src/main/AndroidManifest.xml` no añade permisos nuevos ni
   servicios de accesibilidad.

## Key Design Decisions

- **Teclas multimedia**: `MediaSession.Callback.onMediaButtonEvent` para
  anterior/siguiente físicos (permite ver `repeatCount` y deduplicar) y
  `onPlayerCommandRequest` para comandos de lock screen/controles del sistema.
  Play/pausa sigue en Media3.
- **Volumen**: captura en `MainActivity` porque las teclas de volumen no llegan
  a `MediaSession`; se consume y se emula el cambio de volumen en pulsación
  corta para no alterar el volumen durante una pulsación larga.
- **Segundos globales**: una sola preferencia para auricular, reproductor,
  notificación y bloqueo; la notificación usa comandos propios para funcionar
  también con Cast, donde no se pueden fijar incrementos de `ExoPlayer`.
- **Interruptor de gestos**: solo bloquea `SkipSource.HeadsetGesture` (volumen
  largo y anterior/siguiente físicos). Los botones de salto en pantalla,
  notificación y bloqueo siguen funcionando (FR-012, SC-014).
- **Sin wrapper de player**: se evita envolver `RecoveryAwarePlayer` y
  `CastPlayer`; los comandos de controlador se interceptan en la sesión, lo que
  reduce el riesgo de ciclos de Hilt y de dobles saltos.
- **Cast**: los botones de la notificación y los comandos de sesión usan el
  controlador; si el receptor no admite el salto, no hay efecto local ni audio
  duplicado (FR-014).

## Risks & Mitigations

| Riesgo | Mitigación |
|--------|------------|
| `onPlayerCommandRequest` está deprecado en Media3 1.11 | Aislado en un único callback; se documenta la alternativa (wrapper de player) y se cubre con tests. Si una futura versión lo elimina, el reemplazo queda localizado. |
| El volumen Bluetooth no llega a la app | Documentado en spec y Ajustes; se salta con anterior/siguiente; SC-011 separa la matriz cable/Bluetooth. |
| La pulsación corta de volumen cambia al soltar, no al pulsar | Se acepta para no alterar el volumen durante una pulsación larga; el panel del sistema se muestra con `FLAG_SHOW_UI` y se valida en quickstart. |
| `adjustStreamVolume` está desaconsejado para apps que no gestionan el audio global | Se usa solo para emular una pulsación corta, sin permiso nuevo; si un OEM lo bloquea, la pulsación corta puede no ajustar el volumen: se detecta en la matriz y se puede retirar la captura de volumen. |
| `KEYCODE_HEADSETHOOK` con doble pulsación tiene comportamiento propio de Media3 | Fuera de alcance; no se anuncia multi-tap. Se valida que una pulsación simple pause/reanude. |
| Cast no expone incrementos de seek | La notificación usa comandos propios y `episodeController.seekTo`; el bloqueo depende de las capacidades del receptor (FR-014). |
| Eventos repetidos generan varios saltos | `repeatCount > 0` se consume sin acción; el temporizador de volumen marca el gesto largo y ignora el resto. |

## Complexity Tracking

> No hay violaciones de la constitución. No se requieren entradas.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
