# Implementation Plan: Reproductor flotante inferior

**Branch**: `004-floating-player` | **Date**: 2026-09-05 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-floating-player/spec.md`

## Summary

Elevar el mini-player actual (nombre + play/pausa + detener, bajo la barra) a un **panel fijo sobre la barra inferior de navegación**: izquierda con avatar reutilizado (`StationArtwork` + Coil/emblema) y 2 líneas (`nombre` + `panelSubtitle` puro y testeado); derecha con play/pausa, mute por volumen local y copiar `station.url` al portapapeles. Cambios núcleo: **un único `PlayerViewModel` compartido a ámbito de Activity** (hoy hay uno por destino y el estado global es imposible), `isMuted` + `loadJob`/`cancelLoad()` en el VM, y reordenar el slot `bottomBar` (panel encima de la `NavigationBar`). Sin dependencias nuevas, sin BD/red nuevas, mute efímero en memoria. Ver [research.md](research.md) (R1–R5), [data-model.md](data-model.md), [contracts/mini-panel-ui.md](contracts/mini-panel-ui.md), [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Kotlin 2.1.x (único lenguaje de producción)

**Primary Dependencies**: Compose BOM 2025.01.00 + Material3, Media3 1.4.1 (`ExoPlayer` singleton existente + `MediaSessionService` sin cambios), Coil 2.6.0 (`StationArtwork` reutilizado), Hilt (VM compartido a ámbito de Activity), Coroutines + `Flow`. Sin dependencias nuevas.

**Storage**: N/A — mute efímero en `StateFlow` en memoria (reset en `play()`/`stop()`); sin cambios Room/DataStore.

**Testing**: JUnit + Turbine + MockK para `PlayerViewModel` (mute, reset, `cancelLoad`) y helpers puros (`panelSubtitle`, `resolveCopyLink`) con regla Red-Green; Compose Test para el panel sujeto a la limitación conocida del entorno (API 37 incompatible con `compose-ui-test` → verificación manual en emulador como en specs 002/003).

**Target Platform**: Android `minSdk = 26`, target estable (sin cambios de manifest salvo los ya existentes).

**Project Type**: mobile-app, módulo único `app` organizado por feature (`feature/player/`, `core/ui/navigation/`, `core/ui/components/`).

**Performance Goals**: panel reconoce emisora en <2 s con red normal (SC-001); quitar silencio instantáneo sin reconexión (SC-005); copiar con feedback inmediato.

**Constraints**: copiar **solo** `station.url` (jamás URL del proxy ni token al portapapeles); ViewModel sin `Context` de Activity (portapapeles en capa UI); HTTPS-only y Bearer-en-header sin cambios; textos de usuario en español, sin PII en logs.

**Scale/Scope**: 1 instancia de panel compartida; ~6 destinos con barra inferior + ficha; decenas de emisoras, sin impacto de paginación.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. MVVM + Clean por capas**: UI (`MiniPanel`) sin lógica — estado vía `StateFlow` del VM compartido; mute/cancel como métodos del VM; portapapeles (framework) en composable con helper puro testeable; Hilt para el VM; sin `Context` de Activity en VM. маленьких cambios confinados a `feature/player/` + `core/ui/navigation/`.
- [x] **II. Stack Kotlin-First / Compose M3 / Media3**: solo Kotlin, UI 100 % Compose M3 existente; mute vía `exoPlayer.volume` (Media3, sin `MediaPlayer` crudo); Coil para favicon; corrutinas + Flow; `minSdk=26` intacto; cero dependencias nuevas.
- [x] **III. Calidad Test-First**: tests JUnit+Turbine+MockK obligatorios para VM y helpers (Red-Green antes de implementar); Compose Test del panel o verificación manual documentada por la limitación API 37; Lint + ktlint + Detekt sin errores como gate.
- [x] **IV. Streaming robusto**: `PlayerState` sellado intacto + modificador `isMuted`; error con mensaje ES accionable + reintento (sin texto técnico); sin cambios de red/reintentos (se reutiliza backoff existente); foco audio/Bluetooth y `MediaSessionService` intactos.
- [x] **V. Simplicidad (YAGNI)**: sin módulos nuevos, sin persistencia de mute, sin ecualizador/cola/temporizador (fuera de alcance FR-011); se reutilizan `StationArtwork`, `ExoPlayer` singleton y `SnackbarHost`.

**Re-check post-Phase 1 (2026-09-05)**: el diseño mantiene los 5 gates — no se introduce red, BD, permisos, dependencias ni pantallas nuevas; solo estado en memoria + UI. Sin violaciones que justificar.

## Project Structure

### Documentation (this feature)

```text
specs/004-floating-player/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── mini-panel-ui.md # Contrato UI del panel (props, intents, layout por estado, a11y)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/example/tolocharadio/
├── feature/player/
│   ├── PlayerViewModel.kt       # CAMBIO: isMuted + toggleMute, loadJob + cancelLoad, reset mute en play/stop
│   ├── PlayerUi.kt              # CAMBIO: MiniPlayer → MiniPanel (avatar+2 líneas | play/mute/copiar; layout error reintentar+copiar; tap izquierdo abre sheet)
│   └── RadioPlaybackService.kt  # sin cambios
├── core/ui/navigation/
│   └── TolochaNavGraph.kt       # CAMBIO: VM compartido a ámbito de Activity; Column(panel sobre NavigationBar) en bottomBar; SnackbarHost
├── core/ui/components/
│   └── CommonUi.kt              # reutiliza StationArtwork (sin cambios previstos)
└── di/PlayerModule.kt           # sin cambios (ExoPlayer singleton existente)

app/src/test/java/com/example/tolocharadio/
└── feature/player/
    ├── PlayerViewModelTest.kt   # AMPLÍA: mute on/off+reset, cancelLoad→Idle, error oculta mute
    └── PanelHelpersTest.kt      # NUEVO: panelSubtitle (completa/parcial/vacía), resolveCopyLink (url/blanco)

app/src/androidTest/java/com/example/tolocharadio/
└── PlayerPanelTest.kt           # panel persistente al navegar + copiar con confirmación (o verificación manual si el entorno lo impide)
```

**Structure Decision**: módulo único `app` existente (sin multi-módulo, YAGNI); el VM compartido se scopea a la Activity para servir al panel global y a todas las pantallas sin reestructurar la navegación.

## Complexity Tracking

> Sin violaciones constitucionales que justificar — tabla vacía por diseño.
