# Implementation Plan: Volumen contextual único estilo Pocket Casts

**Branch**: `0037-jlizquierdo-20260918-contextual-volume` | **Date**: 2026-09-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0037-jlizquierdo-20260918-contextual-volume/spec.md`

## Summary

Una sola franja de volumen, contextual según la salida, al estilo Pocket Casts: local →
volumen del teléfono; Cast conectado → solo volumen del dispositivo (teclas + barra del
sistema, sin control in-app). Al conectar, el volumen del receptor nunca cambia (muere el
bug 0036: hoy salta al 100%).

Enfoque técnico (research D1-D8): el stack real es **media3 1.11.0**, cuyo `CastPlayer`
soporta volumen de dispositivo de forma nativa — la sesión ya crea el `VolumeProviderCompat`
remoto con el player crudo (teclas + barra del sistema funcionan hoy sin código propio). La
feature es **sustractiva**:

1. Eliminar el pipeline de escritura `startVolumeEvents`/`emitDeviceVolume` de
   `CastPlayerManager` (root cause del bug 0036: `bind()` lee `getVolume()=1.0` por defecto
   y el collector lo escribe al receptor).
2. Eliminar el deslizador `CastVolumeControl` del full-player y las exposiciones de
   volumen del ViewModel (franja única).
3. Reducir `PlaybackVolumeController` al silencio: `bind()` solo escribe `writeMuted(true)`
   si la app estaba silenciada (nunca des-silencia al conectar, FR-005); sin estado de
   volumen, sin detección de receptor sin volumen (Q1: modelo Pocket Casts).
4. Eliminar el wrapper muerto `CastDeviceVolumePlayer` (+ tests) y la maquinaria de
   degradación (`castVolumeSupported`, avisos, `reinstallSessionPlayerAsLocal`).
5. Anotar los artefactos de la 0035 como superseded donde corresponda.

Regla dura (research D6): **nunca envolver el player de la sesión** — el `CastPlayer` crudo
permanece como player de la sesión; la vía del fix v2 de 0036 (que rompió el audio) no se
vuelve a tomar.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17, `minSdk` 26 / `targetSdk` 37, AGP 9.3.2

**Primary Dependencies**: Media3 **1.11.0** (`media3-exoplayer`, `media3-session`,
`media3-cast` — ya en `gradle/libs.versions.toml`, SIN cambios), `play-services-cast-framework`
21.5.0, Hilt 2.60.1, Coroutines/Flow, Compose Material 3

**Storage**: N/A — el silencio es estado de sesión; sin persistencia ni cambios de
Room/DataStore/backend

**Testing**: JUnit4 + MockK + Turbine + `kotlinx-coroutines-test` (tests JVM de
`PlaybackVolumeController` y `PlayerViewModel`; se eliminan los tests del wrapper); gates
`detekt ktlintCheck lintDebug` y `testDebugUnitTest`

**Target Platform**: Android (móvil); receptor: Chromecast, Google TV o altavoz compatible

**Project Type**: Mobile app (Android), módulo único `app`

**Performance Goals**: ajuste audible < 1 s y eco ≤ 2 s — ambos nativos del `CastPlayer`
1.11.0 (la app no interviene); sin bloqueos de UI ni reinicio del stream

**Constraints**: PROHIBIDO envolver el player de la sesión (research D6); PROHIBIDO escribir
volumen al receptor como efecto lateral de conectar/desconectar (FR-001); media3 queda en
1.11.0 (sin cambios de versiones ni dependencias nuevas); sin pantallas nuevas; llamadas
Cast en hilo principal; sin PII en logs

**Scale/Scope**: 6 archivos de producción modificados, 2 eliminados (wrapper + su test),
2 archivos de test actualizados, 3 anotaciones de documentación (artefactos 0035); **LOC
neto negativo** (se elimina más de lo que se añade)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Justificación |
|-----------|--------|---------------|
| I. MVVM + Clean por capas | ✅ | `PlaybackVolumeController` (silencio) sigue en `feature/player`, inyectado con Hilt en VM/servicio/manager; la UI Compose solo pinta `StateFlow` y delega. Sin `Context` de Activity en ViewModel. |
| II. Kotlin-First, Compose M3 y Media3 | ✅ | Se usa media3 1.11.0 ya presente (volumen nativo del `CastPlayer`); UI Compose M3 sin XML; sin dependencias nuevas ni cambios de versión. |
| III. Calidad Test-First (NON-NEGOTIABLE) | ✅ | Red-Green: antes de tocar código, reescribir `PlaybackVolumeControllerTest` (bind no des-silencia, carry-over, eco de silencio, sin escrituras de volumen) y actualizar `PlayerViewModelTest` (mute vía controlador, sin exposiciones de volumen); los tests del wrapper se eliminan junto a la clase muerta. |
| IV. Streaming Robusto y Manejo de Errores | ✅ | Conectar nunca altera volumen ni silencio del receptor salvo carry-over explícito (FR-001/FR-005); receptor sin volumen → reproducción intacta sin avisos (FR-011); `runCatching` en escrituras de mute; logs tag+causa sin PII. |
| V. Simplicidad Modular (YAGNI) | ✅ | La feature ELIMINA abstracciones sin consumidores (wrapper, detección por eco, pipeline de emisión) — complejidad negativa. Sin módulos, dependencias ni persistencia nuevos. |

**Resultado**: sin violaciones. Re-evaluado tras el diseño (data-model,
contracts/contextual-volume.md): se mantiene sin violaciones.

## Project Structure

### Documentation (this feature)

```text
specs/0037-jlizquierdo-20260918-contextual-volume/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/
│   └── contextual-volume.md
├── checklists/
│   └── requirements.md  # /speckit.specify output
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── cast/
│   ├── CastPlayerManager.kt          # MODIFIED: fuera pipeline volumen + soporte/degradación + KDoc honesto
│   ├── RemoteVolumeDevice.kt         # MODIFIED: contrato solo silencio (read/writeMuted + observe)
│   ├── CastSessionVolumeDevice.kt    # MODIFIED: adaptador solo silencio (CastSession.setMute/isMute + Listener)
│   └── CastDeviceVolumePlayer.kt     # DELETED: código muerto desde 0035 (research D4)
└── feature/player/
    ├── PlaybackVolumeController.kt   # MODIFIED: reducido a silencio (data-model §Estado)
    ├── PlayerViewModel.kt            # MODIFIED: fuera castVolume/setCastVolume/avisos; mute intacto
    ├── PlayerUi.kt                   # MODIFIED: fuera CastVolumeControl + snackbars de volumen
    └── RadioPlaybackService.kt       # SIN CAMBIOS (CUSTOM_COMMAND_MUTE ya delega en toggleMute)

app/src/test/java/com/izquierdojl/tolocharadio/
├── cast/CastDeviceVolumePlayerTest.kt           # DELETED (con la clase)
├── feature/player/PlaybackVolumeControllerTest.kt  # REWRITTEN: semántica solo-silencio (Red)
└── feature/player/PlayerViewModelTest.kt        # UPDATED: sin volumen, mute vía controlador

specs/0035-jlizquierdo-20260916-cast-volume-control/
├── spec.md / plan.md / tasks.md     # ANNOTATED: supersede de FR-003/FR-004/FR-009 y nota media3 1.11.0
```

**Structure Decision**: misma estructura de paquetes que 0035 (`cast/` adaptadores,
`feature/player/` lógica + UI); no se crean archivos de producción nuevos, solo se
modifican/eliminan. DI (`PlayerModule.kt`, `@VolumeScope`) sin cambios: el controlador
conserva constructor y consumidoras.

## Complexity Tracking

> No hay violaciones de la constitución. La feature tiene **complejidad negativa**:
> elimina el wrapper `CastDeviceVolumePlayer` (111 líneas + test), el pipeline
> `startVolumeEvents`/`emitDeviceVolume`, la ventana de confirmación por eco y los avisos
> de la 0035, sustituyéndolos por el soporte nativo de media3 1.11.0 que ya está en el
> binario. Única decisión con riesgo: tocar `createCastPlayer()` (zona sensible por los
> bugs 0031/0036); se mitiga con la regla dura de no envolver el player de la sesión y la
> regla de parar ante cualquier síntoma de audio roto (quickstart §3.5.4).
