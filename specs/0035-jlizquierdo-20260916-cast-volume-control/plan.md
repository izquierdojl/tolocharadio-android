# Implementation Plan: Control de volumen del dispositivo Chromecast

**Branch**: `0035-jlizquierdo-20260916-cast-volume-control` | **Date**: 2026-09-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0035-jlizquierdo-20260916-cast-volume-control/spec.md`

## Summary

Que las teclas de volumen del móvil y el control deslizante del reproductor controlen el
volumen real del dispositivo Cast conectado (experiencia Pocket Casts: barra de volumen del
sistema identificando el dispositivo), con sincronización de cambios externos, silencio
coherente y degradación elegante si el receptor no admite volumen.

Hoy el slider del full-player escribe `CastPlayer.volume`, que en media3 1.4.1 es **no-op**
(verificado en fuente), y no existe ningún enrutado de teclas ni uso de
`CastSession.setVolume`. Tras las pruebas en dispositivo real (research D11), se han
identificado tres bloqueadores:

1. **media3 1.4.1 + Android 17**: el `VolumeProviderCompat` no entrega callbacks al wrapper.
2. **CastSession.setVolume / RemoteMediaClient.setStreamVolume**: el receptor rechaza
   silenciosamente ambos transportes.
3. **Ruteo de teclas**: `session=null` en `MediaSessionService` cuando `state=NONE`.

Enfoque técnico actualizado (ver research D11 para alternativas):

1. **`PlaybackVolumeController`** (nuevo, `feature/player/`): fuente única de silencio y
   volumen remoto; estado `muted`, `castVolume`, `castVolumeSupported`; aplica al reproductor
   activo (ExoPlayer o Cast) y persiste el silencio al cambiar de salida (FR-015).
2. **`RemoteVolumeDevice` + `CastSessionVolumeDevice`** (nuevos, `cast/`): adaptador sobre
   `CastSession.setVolume/getVolume/setMute/isMute` (volumen de **dispositivo**, no de
   stream) y `Cast.Listener.onVolumeChanged` para sincronización.
3. **`CastDeviceVolumePlayer`** (nuevo, `cast/`): wrapper `Player` sobre `CastPlayer` que
   expone los comandos de volumen de dispositivo (`COMMAND_*_DEVICE_VOLUME`) y un
   `DeviceInfo` remoto con `maxVolume = 100`. Con esto, el `MediaSession` de media3 1.4.1
   crea automáticamente su `VolumeProviderCompat` remoto (verificado en
   `PlayerWrapper.createVolumeProviderCompat` + `MediaSessionLegacyStub.onDeviceInfoChanged`):
   las teclas físicas se enrutan al Cast y aparece la barra de volumen del sistema, sin
   implementar un provider propio ni subir de versión media3. Si el receptor no admite
   volumen, el wrapper pasa a `DeviceInfo` local y la sesión reinstala el player para
   devolver las teclas al volumen del móvil (FR-009).
4. **UI**: slider del full-player enlazado al estado real (no `remember` local), etiqueta con
   nombre del dispositivo, aviso si no está soportado; botón de silencio del mini-player y
   comando MUTE de la notificación enrutados por el controlador.
5. **Sin dependencias nuevas, sin cambios de backend, sin tocar la reproducción.**

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17, `minSdk` 26 / `targetSdk` 37, AGP 9.3.2

**Primary Dependencies**: Media3 1.4.1 (`media3-exoplayer`, `media3-session`, `media3-cast`), `play-services-cast-framework` 21.5.0, Hilt 2.60.1, Coroutines/Flow, Compose Material 3

**Storage**: N/A — el volumen y el silencio son estado de sesión, no se persisten; no hay cambios de contrato ni Room/DataStore

**Testing**: JUnit4 + MockK + Turbine + `kotlinx-coroutines-test` (tests JVM de
`PlaybackVolumeController` y `CastDeviceVolumePlayer`, actualización de `PlayerViewModelTest`);
gates `detekt ktlintCheck lintDebug` y `testDebugUnitTest`

**Target Platform**: Android (móvil); el receptor Cast puede ser Chromecast, Google TV o altavoz compatible

**Project Type**: Mobile app (Android), módulo único `app`

**Performance Goals**: ajuste audible en < 1 s (SC-003); reflejo de cambios externos en < 2 s
(SC-004); sin bloqueos de UI ni reinicio del stream durante ajustes (SC-007)

**Constraints**: media3 1.4.1 no soporta volumen en `CastPlayer` (no-op; no se sube de versión);
las llamadas de `CastSession.setVolume` y `RemoteMediaClient.setStreamVolume` son rechazadas
silenciosamente por el receptor "Dormitorio principal" (ver research D11.1); el callback del
`VolumeProviderCompat` de media3 1.4.1 no se entrega en Android 17 (research D11.2); las teclas
físicas no se rutean porque `session=null` en `MediaSessionService` cuando `state=NONE`
(research D11.3); las llamadas Cast se hacen en el hilo principal; sin PII en logs;
sin pantallas nuevas

**Scale/Scope**: una sesión Cast simultánea; 4 archivos nuevos de producción, 5 modificados,
2 archivos de test nuevos y 1 actualizado

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Justificación |
|-----------|--------|---------------|
| I. MVVM + Clean por capas | ✅ | `PlaybackVolumeController` (estado + reglas de volumen/silencio) se inyecta con Hilt en `PlayerViewModel`, `RadioPlaybackService` y `CastPlayerManager`; la UI Compose solo pinta `StateFlow` y delega. Sin `Context` de Activity en ViewModel. |
| II. Kotlin-First, Compose M3 y Media3 | ✅ | Se usa Media3 (`MediaSession`/`Player`) y Cast SDK ya presentes; UI Compose M3 sin XML. Sin dependencias nuevas. |
| III. Calidad Test-First (NON-NEGOTIABLE) | ✅ | Red-Green: tests JVM del controlador (mapeo, silencio, persistencia de silencio, degradación) y del wrapper (comandos, mapeo 0–100, eventos) antes de la implementación; `PlayerViewModelTest` actualizado para el nuevo enrutado. Sin Robolectric (no está en el proyecto): la lógica Android se aísla tras `RemoteVolumeDevice`. |
| IV. Streaming Robusto y Manejo de Errores | ✅ | Ajustar volumen no altera el estado de reproducción (FR-012); si un ajuste no se confirma se corrige en silencio al valor real (FR-014); dispositivo sin soporte → aviso no bloqueante + control oculto, sin desconectar (FR-009); logs con tag+code, sin PII ni credenciales. |
| V. Simplicidad Modular (YAGNI) | ✅ | Sin dependencias, módulos ni persistencia nuevos; un único controlador con 3 consumidores reales (VM, servicio, manager). Se descarta subir media3 solo por esta feature (ver Complexity Tracking). |

**Resultado**: sin violaciones. Re-evaluado tras el diseño (Phase 1): se mantiene sin
violaciones (ver `research.md` para las decisiones y alternativas descartadas).

## Project Structure

### Documentation (this feature)

```text
specs/0035-jlizquierdo-20260916-cast-volume-control/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── cast-volume.md
├── checklists/
│   └── requirements.md  # /speckit.specify output
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/feature/player/
├── PlaybackVolumeController.kt   # NEW: estado y reglas de volumen/silencio (local + Cast)
├── PlayerViewModel.kt            # MODIFIED: expone estado del controlador, delega toggleMute
├── PlayerUi.kt                   # MODIFIED: slider real + etiqueta + aviso no soportado
└── RadioPlaybackService.kt       # MODIFIED: comando MUTE enrutado al controlador

app/src/main/java/com/izquierdojl/tolocharadio/cast/
├── RemoteVolumeDevice.kt         # NEW: interfaz del dispositivo remoto (testable)
├── CastSessionVolumeDevice.kt    # NEW: adaptador CastSession + Cast.Listener
├── CastDeviceVolumePlayer.kt     # NEW: Player wrapper con comandos de volumen de dispositivo
└── CastPlayerManager.kt          # MODIFIED: bind/unbind del controlador, player de sesión

app/src/main/java/com/izquierdojl/tolocharadio/di/
└── PlayerModule.kt               # MODIFIED: provee PlaybackVolumeController (+ scope Main)

app/src/test/java/com/izquierdojl/tolocharadio/
├── feature/player/PlaybackVolumeControllerTest.kt  # NEW: Red-Green
├── cast/CastDeviceVolumePlayerTest.kt              # NEW: Red-Green
└── feature/player/PlayerViewModelTest.kt           # MODIFIED: mute/volumen vía controlador
```

**Structure Decision**: la lógica vive en `feature/player/` (junto al ViewModel y el servicio
que la consumen) y los adaptadores Cast en `cast/` (paquete existente, ya importado por
`feature/player`). No se crean módulos ni capas nuevas; no hay cambios de backend.

## Complexity Tracking

> No hay violaciones de la constitución. Se justifica la única pieza con complejidad
> significativa:

| Decisión | Por qué es necesaria | Alternativa más simple rechazada |
|----------|----------------------|----------------------------------|
| `CastDeviceVolumePlayer` (wrapper `Player` sobre `CastPlayer`) | media3 1.4.1 crea el `VolumeProviderCompat` remoto **solo** a partir de `Player.getDeviceInfo()` + comandos de volumen del player de la sesión; `CastPlayer` 1.4.1 los declara pero son no-op. El wrapper es el punto de extensión oficial (issue androidx/media #328) y es localizado (~120 líneas delegadas). | Subir media3 a una versión con soporte nativo de volumen en `CastPlayer` (PRs #2089/#2279): descartado por blast radius (ExoPlayer/sesión/HLS/Cast en toda la app) para una feature; queda como simplificación futura. Implementar un `VolumeProvider` propio: imposible sin API pública en media3 1.4.1. |
| `PlaybackVolumeController` como fuente única de silencio | FR-015 exige que el silencio sobreviva al cambio local ↔ Cast y el comando MUTE de la notificación debe afectar al dispositivo activo; hoy el silencio vive en el VM y la notificación escribe `player.volume` (no-op en Cast). | Duplicar estado Cast/local en VM y servicio: dos fuentes de verdad que se desincronizan (el bug que la feature corrige). |
