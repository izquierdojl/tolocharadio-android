# Data Model: Temporizador de apagado (Sleep Timer)

**Date**: 2026-09-08
**Feature**: 014-sleep-timer

## Entidades

### SleepTimerState (sellado)

Estado del temporizador de apagado. Transitorio (no persiste).

```
SleepTimerState
├── Inactive                          # Sin temporizador activo
└── Active(
│       durationMinutes: Int,         # Duración seleccionada (15, 30, 45, 60, 90)
│       remainingSeconds: Long,       # Segundos restantes (decrementa cada segundo)
│       expiresAtEpochMs: Long        # Instante de expiración (epoch ms)
│   )
```

**Transiciones de estado**:

```
Inactive ──[start]──► Active
Active ──[cancel]──► Inactive
Active ──[expires]──► Inactive
Active ──[restart with new duration]──► Active
```

**Validaciones**:
- `durationMinutes` DEBE ser uno de: 15, 30, 45, 60, 90
- `remainingSeconds` DEBE ser >= 0
- `expiresAtEpochMs` DEBE ser > System.currentTimeMillis()

### SleepTimerDuration (enum)

Opciones de duración predefinidas.

```
SleepTimerDuration
├── MINUTES_15   (15)
├── MINUTES_30   (30)
├── MINUTES_45   (45)
├── MINUTES_60   (60)
└── MINUTES_90   (90)
```

## Relaciones

```
SleepTimerViewModel
├── observes: Flow<SleepTimerState>      (del UseCase)
└── delegates stop(): PlayerViewModel    (al expirar)

SleepTimerUseCase
├── emits: Flow<SleepTimerState>         (estado actual)
└── accepts: start(duration), cancel()   (comandos)

TolochaNavGraph
├── observes: SleepTimerViewModel.state  (para el badge)
└── calls: SleepTimerViewModel.start/cancel (desde el menú)
```

## Persistencia

**Ninguna**. El temporizador es transitorio:
- Se pierde al cerrar la app (edge case de la spec)
- Se pierde al destruir el ViewModel (rotación de pantalla lo mantiene porque el VM es a ámbito de Activity)

## Consideraciones de threading

- El countdown se ejecuta en `Dispatchers.Default` (no bloquea el Main)
- La actualización de `remainingSeconds` se emite en `Dispatchers.Main` (para la UI)
- La expiración (llamada a `PlayerViewModel.stop()`) DEBE ocurrir en el Main dispatcher
