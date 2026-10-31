# Implementation Plan: Continuidad de reproducción y buffer ajustable

**Branch**: `0042-izquierdojl-20261031-audio-playback-recovery` | **Date**: 2026-10-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0042-izquierdojl-20261031-audio-playback-recovery/spec.md`

## Summary

Implementar una recuperación automática y acotada de la reproducción local cuando el
stream se queda en silencio, falla o se pierde la conectividad, más dos ajustes
persistentes en Configuración: perfil de buffer (Ahorro de datos / Equilibrado /
Máxima estabilidad) y reanudación automática al recuperar la conexión. La
recuperación se ejecuta fuera del `ViewModel` (scope de aplicación y servicio de
reproducción) para que funcione en segundo plano; el `ViewModel` refleja el estado
`Reconnecting`/`Error` y ofrece reintento manual. El cambio de perfil se aplica
recreando el `ExoPlayer` con otro `DefaultLoadControl` y sustituyéndolo en la
`MediaSession` con `setPlayer`, sin reiniciar la app.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17.

**Primary Dependencies**: AndroidX Media3 1.11.0 (`media3-exoplayer`,
`media3-session`, `media3-exoplayer-hls`, `media3-datasource-okhttp`), Hilt
2.60.1, Compose BOM 2026.05.00 + Material 3, DataStore Preferences 1.1.1,
Coroutines/Flow, Retrofit/OkHttp, JUnit 4, Turbine, MockK, Compose UI Test.

**Storage**: DataStore `tolocha_prefs` con dos claves nuevas:
`buffer_profile` (`DATA_SAVER` | `BALANCED` | `MAX_STABILITY`) y
`resume_on_connectivity` (booleano). Sin cambios de esquema Room.

**Testing**: JUnit 4 + coroutines-test + Turbine + MockK para dominio, datos y
ViewModels; Compose UI Test para Configuración y estados del reproductor; gates
`assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug` y
`connectedDebugAndroidTest` cuando haya dispositivo.

**Target Platform**: Android `minSdk` 26, `compileSdk`/`targetSdk` 37.

**Project Type**: Aplicación Android de un solo módulo Gradle (`app`).

**Performance Goals**: detección de corte en 5 s; recuperación acotada a 5
intentos / 60 s; reanudación en menos de 10 s desde que vuelve la red; el perfil
"Máxima estabilidad" añade como máximo 5 s al arranque frente a "Equilibrado";
cero reintentos en bucle sin conectividad.

**Constraints**: reproducción en segundo plano vía `MediaSessionService`;
HTTPS-only; sin credenciales ni PII en logs; sin dependencias nuevas; Chromecast
fuera del alcance de recuperación y de buffer; se preservan foco de audio, pausa
por auriculares, temporizador de apagado y controles de notificación; las
emisoras son directos y se reanudan en el punto en vivo.

**Scale/Scope**: un stream local activo por dispositivo, tres perfiles de buffer,
dos ajustes nuevos y una acción nueva de notificación. Sin cambios en el
contrato del backend ni en la base de datos.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento | Evidencia en el plan |
|---|---|---|
| I. MVVM + Clean | PASS | `BufferProfileSpec` y `RecoveryPolicy` son dominio puro; persistencia en `InstancePrefs` (data); UI Compose en `feature/settings`; el ViewModel no obtiene `Context` de Activity. |
| II. Kotlin + Compose M3 + Media3 | PASS | Se usa Media3/ExoPlayer y `DefaultLoadControl`; Configuración se amplía con Compose M3; no se añaden XML/Views ni dependencias. |
| III. Calidad Test-First | PASS | Tests de dominio para política/perfiles, tests de DataStore, tests de `SettingsViewModel` y `PlayerViewModel`, y tests Compose de flujos críticos. Se exige Red-Green antes de implementar. |
| IV. Streaming robusto | PASS | Estados sellados `Reconnecting`/`Error` con acción, backoff acotado, manejo de 401/403, espera de conectividad y logs estructurados sin PII. |
| V. Simplicidad modular (YAGNI) | PASS | Se reutilizan DataStore, Media3, Hilt y el servicio existentes. `PlaybackEngine` se justifica porque hay varios consumidores del player y hace falta sustituirlo para aplicar el buffer. Sin módulos nuevos. |
| Quality Gates | PASS | El plan incluye compilación, tests unitarios, Detekt, ktlint, Lint y pruebas conectadas de UI. |

**Post-design check (Phase 1)**: PASS. No hay violaciones que requieran
`Complexity Tracking`; el intercambio de instancia de player es una decisión de
implementación para cumplir el requisito de aplicación inmediata, no una
excepción constitucional.

## Project Structure

### Documentation (this feature)

```text
specs/0042-izquierdojl-20261031-audio-playback-recovery/
├── plan.md              # Este archivo
├── research.md          # Fase 0
├── data-model.md        # Fase 1
├── quickstart.md        # Fase 1
├── contracts/
│   ├── playback-settings-ui.md
│   └── player-recovery-ui.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Fase 2 (/speckit.tasks, no creado aquí)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/network/
│   ├── ConnectivityMonitor.kt          # Interfaz + implementación Android
│   └── ...
├── data/local/
│   └── InstancePrefs.kt                # buffer_profile, resume_on_connectivity
├── di/
│   ├── ConnectivityModule.kt           # @Binds del monitor
│   ├── PlayerModule.kt                 # PlaybackEngine en lugar de ExoPlayer
│   └── ...
├── domain/playback/
│   ├── BufferProfile.kt                # DATA_SAVER | BALANCED | MAX_STABILITY
│   ├── BufferProfileSpec.kt            # milisegundos de LoadControl
│   ├── InterruptionCause.kt            # red, stall, auth, emisora, decoder
│   ├── RecoveryPolicy.kt               # backoff, límites y umbrales
│   └── RecoveryState.kt                # Idle/Waiting/Reconnecting/Failed/Paused
└── feature/
    ├── player/
    │   ├── PlaybackEngine.kt           # player actual + playerFlow + swap
    │   ├── PlaybackPlayerFactory.kt    # crea ExoPlayer con un perfil
    │   ├── LocalPlaybackStarter.kt     # reutilizado por ViewModel/servicio
    │   ├── PlaybackProfileController.kt# solicitud de cambio de perfil
    │   ├── PlaybackRecoveryManager.kt  # watchdog + reintentos en background
    │   ├── PlayerAudioConfig.kt        # aplica LoadControl al builder
    │   ├── PlayerViewModel.kt          # estados y acciones de UI
    │   ├── PlayerUi.kt                 # Reconnecting/Error/Reintentar
    │   ├── RadioPlaybackService.kt     # setPlayer, retry de notificación
    │   └── ...
    └── settings/
        ├── SettingsViewModel.kt        # estado y persistencia de ajustes
        ├── SettingsScreen.kt           # sección "Reproducción"
        └── SettingsLabels.kt           # etiquetas y descripciones

app/src/test/java/com/izquierdojl/tolocharadio/
├── domain/playback/
│   ├── BufferProfileSpecTest.kt
│   └── RecoveryPolicyTest.kt
├── data/local/InstancePrefsTest.kt     # ampliar
├── feature/player/PlaybackEngineTest.kt
├── feature/player/PlayerViewModelTest.kt # ampliar
└── feature/settings/SettingsViewModelTest.kt # ampliar

app/src/androidTest/java/com/izquierdojl/tolocharadio/
├── feature/settings/PlaybackSettingsTest.kt
└── feature/player/RecoveryUiTest.kt
```

**Structure Decision**: se mantiene el módulo único `app` y la separación por
features existente. La lógica de decisión (perfiles, backoff, clasificación) vive
en `domain/playback`; la integración con Media3/Android vive en `feature/player`;
la persistencia en `data/local`; la UI en `feature/settings` y `feature/player`.

## Phase 0: Outline & Research

Se consolidan las decisiones en [research.md](./research.md). Resumen:

- **R1**: perfiles de buffer con `DefaultLoadControl` y valores concretos.
- **R2**: aplicar el perfil recreando el player y usando
  `MediaSession.setPlayer`, porque ExoPlayer no permite cambiar `LoadControl` en
  caliente.
- **R3**: recuperación en un singleton de scope de aplicación, no en el
  `ViewModel`, para sobrevivir a segundo plano y muerte de Activity.
- **R4**: detección de stall por avance de `currentPosition` + `Player.Listener`,
  con umbral de 5 s tras la primera reproducción y 10 s en la carga inicial.
- **R5**: `ConnectivityMonitor` con `registerDefaultNetworkCallback`.
- **R6**: 401/403 se resuelven con `AuthenticateServerUseCase` antes de rendirse.
- **R7**: persistencia en DataStore con fallback seguro.
- **R8**: estados de UI y acción "Editar servidor" para credenciales.
- **R9**: logs estructurados con stationId anonimizado.
- **R10**: estrategia de pruebas y límites de testabilidad de ExoPlayer.

## Phase 1: Design & Contracts

- [data-model.md](./data-model.md): entidades, valores de perfil, política de
  recuperación, claves de DataStore y transiciones de estado.
- [contracts/playback-settings-ui.md](./contracts/playback-settings-ui.md):
  sección "Reproducción", opciones, valores por defecto y accesibilidad.
- [contracts/player-recovery-ui.md](./contracts/player-recovery-ui.md): estados
  visibles, textos, acciones y comportamiento en notificación.
- [quickstart.md](./quickstart.md): escenarios de validación manual con red,
  perfiles, segundo plano y errores de sesión.

## Implementation Phases

### Phase A - Dominio y pruebas (bloqueante)

1. Crear `BufferProfile`, `BufferProfileSpec` con validación
   (`minBufferMs >= bufferForPlaybackMs`, `maxBufferMs >= minBufferMs`, todos > 0)
   y los valores de R1.
2. Crear `InterruptionCause`, `RecoveryState` y `RecoveryPolicy` con:
   - máximo 5 intentos y 60 s;
   - espera por intento `[0, 1, 2, 4, 8]` s;
   - ventana de éxito por intento de 7 s;
   - umbral de stall 5 s y de carga inicial 10 s.
3. Tests Red-Green de `BufferProfileSpec` y `RecoveryPolicy`: límites, backoff,
   timeout, clasificación y ausencia de reintentos cuando el usuario pausó.

### Phase B - Persistencia y conectividad

1. Ampliar `InstancePrefs` con `bufferProfile` (default `BALANCED`) y
   `resumeOnConnectivity` (default `true`), con fallback ante valor corrupto.
2. Añadir tests a `InstancePrefsTest` con el `FakeDataStore` existente.
3. Crear `ConnectivityMonitor` (interfaz) y `AndroidConnectivityMonitor`
   (`registerDefaultNetworkCallback`, `NET_CAPABILITY_INTERNET` +
   `NET_CAPABILITY_VALIDATED`), exponiendo `StateFlow<Boolean>`.
4. Registrar el binding Hilt en `ConnectivityModule`; no se añaden permisos
   (`ACCESS_NETWORK_STATE` ya existe).

### Phase C - Motor de reproducción y aplicación del buffer

1. Ampliar `PlayerAudioConfig` para construir `DefaultLoadControl` desde un
   `BufferProfileSpec` y aplicarlo al `ExoPlayer.Builder`.
2. Crear `PlaybackPlayerFactory` (contexto + perfil → `ExoPlayer`).
3. Crear `PlaybackEngine`:
   - `player: ExoPlayer`, `playerFlow: StateFlow<ExoPlayer>`,
     `currentProfile: BufferProfile`;
   - `ensureProfile(profile): ExoPlayer` que reutiliza el player si el perfil no
     cambió y lo recrea si cambió (caso de servicio detenido);
   - `swap(profile): PlayerSwap` que crea el nuevo player y publica el cambio;
   - el engine conserva la instancia durante el proceso; `RadioPlaybackService.onDestroy`
     MUST llamar `stop()`/`clearMediaItems()` y MUST NOT llamar `release()`,
     porque el player se reutiliza al recrear el servicio. La liberación
     definitiva de la instancia ocurre con el proceso.
4. Crear `LocalPlaybackStarter` con la lógica local de `PlayerViewModel.startSource`
   (resolver fuente, `MediaItem`, `MediaSource`, `prepare`, `playWhenReady`,
   historial opcional). `PlayerViewModel` lo usa para reproducción normal;
   recuperación y cambio de perfil lo usan con `recordHistory = false`.
5. Cambiar `PlayerModule` para proveer `PlaybackEngine` con el perfil persistido
   (lectura inicial coherente con `NetworkModule`), y actualizar
   `PlayerViewModel`, `CastPlayerManager`, `PlaybackVolumeController` y
   `RadioPlaybackService` para usar `engine.player`/`playerFlow` en lugar de
   inyectar `ExoPlayer` directamente.
6. En `RadioPlaybackService`, sincronizar el perfil persistido al arrancar
   (`engine.ensureProfile(prefs.bufferProfile.first())`) antes de crear la
   `MediaSession`, y coleccionar `PlaybackProfileController.requests`:
   - si Cast está conectado, sustituir solo el player local sin tocar la
     `MediaSession` (el Cast manda); detener/limpiar el player local anterior;
   - si no, detener el player anterior (`stop()`/`clearMediaItems()`),
     `session.setPlayer(newPlayer)`, recrear la reproducción actual con
     `LocalPlaybackStarter(recordHistory = false)` y liberar el player anterior.
7. `PlaybackProfileController` expone `requests` y `applying: StateFlow<Boolean>`:
   el servicio marca `applying=true` al recibir la solicitud y lo baja al
   terminar; si el servicio no está activo, no se marca y el ajuste se aplicará
   al arrancar mediante `ensureProfile`. La UI usa `applying` para el aviso
   "Aplicando ajuste…" sin crear un estado de error.

### Phase D - Recuperación automática

1. Crear `PlaybackRecoveryManager` singleton con scope de aplicación en el hilo
   principal (`SupervisorJob() + Dispatchers.Main.immediate`):
   - se engancha a `engine.playerFlow` y reengancha el `Player.Listener` en cada
     swap;
   - watchdog de posición cada segundo; 5 s sin avance tras la primera
     reproducción, 10 s en carga inicial;
   - ignora pausa manual, pérdida de foco, `becoming noisy`, Cast y
     `STATE_IDLE/ENDED`.
2. Clasificar `PlaybackException`:
   - red/timeout → reintento con backoff;
   - 401/403 → `AuthenticateServerUseCase` y reintento; si falla, `EDIT_SERVER`;
   - 404/503 → reintento acotado y error accionable;
   - decoder/unknown → reintento acotado.
3. Ante falta de conectividad: estado `WaitingForNetwork`, cero reintentos;
   al volver la red, reintento inmediato si `resumeOnConnectivity` está activo;
   si está desactivado, dejar la emisora en pausa y permitir play manual.
4. Actualizar `ActiveStationHolder` con `BUFFERING` durante la recuperación,
   `PAUSED` si la reanudación está desactivada, `ERROR` al rendirse y `PLAYING`
   al recuperar; el `PlayerViewModel` combina `RecoveryState` con el estado del
   player, dando prioridad al primero mientras esté activo.
5. Cancelar recuperación ante pausa manual, stop, cambio de emisora, fin del
   temporizador o conexión Cast.
6. Registrar eventos con tag `PlaybackRecovery` (causa, intento, resultado,
   duración, stationId hasheado); sin PII ni credenciales.

### Phase E - UI, ajustes y notificación

1. Añadir `PlayerState.Reconnecting(station, attempt, maxAttempts,
   waitingForNetwork)` y `PlayerState.Error(station, message, action)` con
   `PlayerErrorAction.RETRY | EDIT_SERVER`.
2. Actualizar `PlayerUi`:
   - mini-player: subtítulo "Reconectando (n/5)…" o "Sin conexión…", spinner y
     acción de reintento/editar servidor;
   - reproductor completo: progreso, mensaje y botón "Reintentar" o
     "Editar servidor";
   - anuncios de TalkBack coherentes.
3. Pasar `onEditServer` desde `TolochaNavGraph` a `MiniPlayer`/`FullPlayerSheet`
   usando `editActiveServer`.
4. Ampliar `SettingsUi`/`SettingsViewModel` con `bufferProfile` y
   `resumeOnConnectivity`; persistir y solicitar el cambio de perfil.
5. Añadir en `SettingsScreen` la sección "Reproducción" con:
   - selector de tres perfiles con descripción y valor por defecto;
   - interruptor "Reanudar al recuperar conexión" (activado por defecto).
6. Añadir etiquetas/descripciones en `SettingsLabels` y `contentDescription`
   para accesibilidad.
7. En `RadioPlaybackService`, añadir comando `CUSTOM_COMMAND_RETRY` y botón de
   notificación visible en estado de error; el comando llama a
   `PlaybackRecoveryManager.manualRetry()`.
8. Ajustar `PlayerViewModel.toggle()`: si el estado es `Paused` pero el player no
   está listo (`IDLE`/error), delegar en `manualRetry()` en lugar de solo cambiar
   `playWhenReady`.

### Phase F - Pruebas y gates

1. Unitarios: dominio, prefs, `PlaybackEngine`, `SettingsViewModel`,
   `PlayerViewModel` (Reconnecting, Error con acción, retry, swap de player).
2. Compose: sección de reproducción (selección, persistencia visual, switch) y
   estados del panel (`Reconnecting`, `Error` con reintento/editar servidor).
3. Regresión: foco de audio, auriculares, temporizador, Cast y notificación.
4. Ejecutar `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`,
   `lintDebug`; `connectedDebugAndroidTest` con dispositivo/emulador.
5. Validación manual con [quickstart.md](./quickstart.md), incluidos los
   protocolos medibles SC-001, SC-005 y SC-007.

## Dependencies & Ordering

- Phase A es bloqueante para B, C y D.
- Phase B es bloqueante para D (conectividad y preferencias).
- Phase C es bloqueante para D y E (engine, starter y swap).
- Phase D es bloqueante para E (estados de recuperación).
- Phase E y F se ejecutan después; los tests de cada fase se escriben antes de
  la implementación correspondiente.

## Risks & Mitigations

| Riesgo | Impacto | Mitigación |
|---|---|---|
| El swap de player corta el audio o deja la `MediaSession` sin player | Alto | `MediaSession.setPlayer` en el hilo principal, recrear la fuente con `LocalPlaybackStarter`, test instrumentado de cambio de perfil y prueba manual. |
| Falso positivo del watchdog durante foco, pausa o buffering inicial | Alto | Ignorar `playWhenReady=false`, Cast y `IDLE/ENDED`; umbral específico de carga inicial; tests de estado. |
| Recuperación en bucle o consumo de batería sin red | Alto | `WaitingForNetwork` sin reintentos, backoff acotado y límite de 5/60 s; SC-007. |
| 401/403 no se renueva porque el player usa un OkHttp sin autenticador | Alto | `AuthenticateServerUseCase` antes del reintento; si falla, acción "Editar servidor". |
| Duplicar historial al recuperar o cambiar perfil | Medio | `LocalPlaybackStarter` con `recordHistory=false` en esos caminos; test de historial. |
| Valores de buffer que incumplen SC-001/SC-005 | Medio | Perfiles de R1 con arranque +4 s (≤5 s) y `afterRebuffer` ≤7 s; validación en quickstart. |
| `runBlocking` para leer el perfil inicial en el grafo Hilt | Bajo | Mismo patrón ya usado en `NetworkModule`; lectura única de DataStore; se documenta en research. |
| Perfil cambiado con el servicio detenido | Medio | `PlaybackEngine.ensureProfile` sincroniza el perfil persistido al crear el servicio; si el perfil cambió, recrea antes de la `MediaSession`. |
| Complejidad de testear ExoPlayer en JVM | Medio | Extraer política pura a dominio, usar `PlayerFactory` falso en `PlaybackEngineTest` y pruebas Compose/instrumentadas para la UI. |

## Complexity Tracking

No hay violaciones constitucionales. El único elemento que añade complejidad es
el intercambio de instancia de `ExoPlayer` para aplicar el perfil sin reiniciar:

| Decisión | Por qué | Alternativa descartada |
|---|---|---|
| `PlaybackEngine` + `MediaSession.setPlayer` | ExoPlayer no permite cambiar `LoadControl` en caliente y el usuario eligió aplicación inmediata | Wrapper de `LoadControl` con allocator compartido (frágil) y reinicio de app (mala UX) |

El player del engine es process-scoped: el servicio no lo libera en `onDestroy`,
solo ejecuta `stop()`/`clearMediaItems()`, que libera codecs y buffers y permite
reutilizar la instancia al recrear el servicio. La liberación definitiva ocurre
con el proceso.

## Next Step

`/speckit.tasks` para descomponer las fases A–F en tareas ejecutables y
verificables.
