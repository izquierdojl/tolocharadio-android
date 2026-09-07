# Quickstart: Enhanced Audio Player UX

**Feature**: 010-enhanced-audio-player
**Date**: 2026-09-07

## Prerequisites

- Android Studio con AGP 9.3.2+
- Emulador o dispositivo con API 26+
- Instancia de backend TolochaRadio accesible (configurada en `InstancePrefs`)

## Setup

```bash
# Clonar y checkout la feature branch
git checkout 010-enhanced-audio-player

# Compilar
./gradlew assembleDebug
```

## Validation Scenarios

### V1: Notificación nativa con controles (US-1)

1. Abrir la app y reproducir cualquier emisora
2. Minimizar la app (botón home)
3. **Expected**: Notificación del sistema con nombre de emisora, favicon, controles play/pause y stop
4. Pulsar pause en la notificación
5. **Expected**: Audio se pausa, notificación permanece visible con botón de play
6. Pulsar play en la notificación
7. **Expected**: Audio se reanuda, icono cambia a pause
8. Pulsar stop en la notificación
9. **Expected**: Audio se detiene, notificación desaparece

### V2: Información completa de emisora (US-2)

1. Reproducir una emisora
2. Tocar el logo/favicon en el mini-player
3. **Expected**: Bottom sheet con: nombre, país, idioma, tags, codec, bitrate, votos, clicks, homepage
4. Tocar el enlace de homepage
5. **Expected**: Navegador externo se abre con la URL de la emisora
6. Deslizar el sheet hacia abajo
7. **Expected**: Sheet se cierra, reproducción continúa sin interrupción

### V3: Persistencia del mini-player (US-3)

1. Reproducir una emisora
2. Navegar a Explorar → Favoritos → Historial
3. **Expected**: Mini-player visible en todas las pantallas
4. Pulsar botón home (minimizar app)
5. Volver a abrir la app
6. **Expected**: Mini-player reaparece con nombre, favicon y estado correctos (playing o paused)
7. Reproducir emisora, pausar, minimizar, volver
8. **Expected**: Mini-player muestra estado pausado con botón play

### V4: Consistencia visual (US-4)

1. Reproducir emisora y observar transiciones:
   - Idle → Buffering: indicador de carga aparece
   - Buffering → Playing: indicador desaparece, icono pause
   - Playing → Paused: icono play
   - Error: mensaje en español + botón retry
2. **Expected**: Transiciones < 300ms, sin parpadeos
3. Provocar error (desconectar red)
4. **Expected**: Mensaje de error claro en español con botón de reintento funcional

### V5: Controles externos

1. Reproducir emisora
2. Usar controles de auriculares Bluetooth o controles de pantalla de bloqueo
3. **Expected**: Play/pause/stop funcionan correctamente
4. Recibir llamada telefónica mientras se reproduce
5. **Expected**: Audio se pausa automáticamente, se reanuda al terminar llamada

## Test Commands

```bash
# Tests unitarios del player
./gradlew :app:testDebugUnitTest --tests "*PlayerViewModel*"

# Tests de UI Compose
./gradlew :app:connectedDebugAndroidTest --tests "*PlayerUi*"

# Lint + Detekt
./gradlew lintDebug detekt
```

## References

- [Spec](./spec.md)
- [Data Model](./data-model.md)
- [Station Info UI Contract](./contracts/station-info-ui.md)
- [Constitution](../../.specify/memory/constitution.md)
