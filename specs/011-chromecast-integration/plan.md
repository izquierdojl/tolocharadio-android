# Implementation Plan: Chromecast Integration

**Branch**: `011-chromecast-integration` | **Date**: 2026-09-07 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/011-chromecast-integration/spec.md`

## Summary

Integrar Chromecast en Tolocha Radio para permitir al usuario enviar el audio de la radio a un dispositivo Chromecast (TV o altavoz). El botón de Cast se añade en la TopAppBar existente. La transferencia de audio utiliza el módulo `media3-cast` de Media3, que proporciona un `CastPlayer` compatible con la interfaz `Player` de Media3, permitiendo reutilizar la arquitectura existente del PlayerViewModel. Se usa el Default Media Receiver (CC1AD845) sin necesidad de registro en Google.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17, Android minSdk 26, targetSdk 37

**Primary Dependencies**:
- Media3 1.4.1 (exoplayer, session, ui, datasource-okhttp) — extensible con `media3-cast`
- Google Play Services Cast Framework (`com.google.android.gms:play-services-cast-framework`)
- Hilt 2.60.1 (DI)
- Jetpack Compose (BOM 2025.01.00)
- Retrofit 2.11.0 + OkHttp 4.12.0 (network)

**Storage**: Room (local DB), DataStore (preferences), no cambios necesarios para Cast

**Testing**: JUnit 4.13.2, MockK 1.13.12, Turbine 1.1.0, Espresso 3.7.0, Compose UI Test

**Target Platform**: Android (dispositivos con Google Play Services — requerido por Cast SDK)

**Project Type**: Mobile app (Android, single-module)

**Performance Goals**:
- Transferencia de audio a Chromecast: < 5 segundos (SC-002)
- Reconexión local tras desconexión: < 3 segundos (SC-004)

**Constraints**:
- Dispositivos Chromecast deben estar en la misma red local que el dispositivo Android
- Solo pantallas principales autenticadas (no login/register/onboarding)
- Default Media Receiver (CC1AD845) — sin receiver personalizado

**Scale/Scope**: App personal/educativa, un único usuario, 12 requisitos funcionales, 5 user stories

## Constitution Check

*No constitution file found (`.specify/memory/constitution.md` does not exist). Skipping gate check.*

## Project Structure

### Documentation (this feature)

```text
specs/011-chromecast-integration/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── cast/                           # NEW — Chromecast module
│   ├── CastOptionsProvider.kt      # OptionsProvider del SDK Cast
│   ├── CastPlayerManager.kt        # Gestión de CastPlayer y switching local/remoto
│   └── CastSessionListener.kt      # Listener de eventos de sesión Cast
├── di/
│   └── PlayerModule.kt             # MODIFIED — Proveer CastPlayerManager
├── feature/player/
│   ├── PlayerViewModel.kt          # MODIFIED — Usar CastPlayerManager para switching
│   ├── PlayerUi.kt                 # MODIFIED — Indicador Cast en mini-player
│   └── RadioPlaybackService.kt     # MODIFIED — Desactivar media session Cast
├── core/ui/navigation/
│   └── TolochaNavGraph.kt          # MODIFIED — Añadir MediaRouteButton en TopAppBar
└── AndroidManifest.xml             # MODIFIED — Añadir metadata Cast SDK
```

**Structure Decision**: Single-module Android app. La feature se integra en la estructura existente con un nuevo paquete `cast/` para la lógica específica de Chromecast, y modificaciones mínimas en los archivos existentes del player y la navegación.
