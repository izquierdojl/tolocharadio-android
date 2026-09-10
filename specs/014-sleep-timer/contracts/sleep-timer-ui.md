# UI Contract: Sleep Timer Button

**Date**: 2026-09-08
**Feature**: 014-sleep-timer

## Componente: SleepTimerButton

### Ubicación

En `TolochaNavGraph.kt`, dentro de `TopAppBar.actions`, ANTES del botón de Servidores:

```kotlin
// Orden de actions:
1. Cast (MediaRouteButton)
2. ViewModeToggle (solo en VIEW_MODE_ROUTES)
3. SleepTimerButton     ← NUEVO
4. Servers (Dns icon)
```

### Comportamiento del botón

| Estado | Icono | Badge | onClick |
|--------|-------|-------|---------|
| Inactivo | `Icons.Outlined.Timer` | Ninguno | Abre menú de duraciones |
| Activo | `Icons.Filled.Timer` | `X min` restante | Abre menú con tiempo restante + cancelar |

> El badge muestra solo minutos (sin segundos) y se actualiza como máximo una vez por minuto (FR-009: menor consumo de CPU/batería).

### Menú desplegable (DropdownMenu)

**Estado Inactivo** — muestra opciones de duración:

```
┌─────────────────────────┐
│  15 minutos             │
│  30 minutos             │
│  45 minutos             │
│  60 minutos             │
│  90 minutos             │
└─────────────────────────┘
```

**Estado Activo** — muestra tiempo restante + opción de cancelar:

```
┌─────────────────────────┐
│  ⏱ Quedan 23 minutos   │
│  ─────────────────────  │
│  Cancelar temporizador  │
└─────────────────────────┘
```

### Accessibility

- `contentDescription` del icono: "Temporizador de apagado" (inactivo) / "Temporizador activo, quedan X minutos" (activo)
- El badge NO necesita `contentDescription` adicional (el icono ya indica el estado)
- Opciones del menú: texto legible directo

### States de la UI

| State | Descripción |
|-------|-------------|
| `SleepTimerUiState.Inactive` | Sin timer activo, botón sin badge |
| `SleepTimerUiState.Active(remainingMinutes)` | Timer activo, badge con "X min" |

### Interacciones

```
User taps button (Inactive)
  └─► Show DropdownMenu with 5 duration options
       └─► User selects duration
            └─► Call sleepTimerViewModel.start(duration)

User taps button (Active)
  └─► Show DropdownMenu with remaining time + cancel
       └─► User taps "Cancelar"
            └─► Call sleepTimerViewModel.cancel()
```

### Tema

- Usa `MaterialTheme.colorScheme.onSurfaceVariant` para el icono inactivo
- Usa `MaterialTheme.colorScheme.primary` para el icono activo
- Badge: `MaterialTheme.colorScheme.error` (rojo, indica urgencia/temporizador)
- Menú: `MaterialTheme.colorScheme.surfaceContainer` (consistente con TopAppBar)
