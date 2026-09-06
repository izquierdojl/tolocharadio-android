# UI Contract: Mini-panel inferior (`MiniPanel`)

**Spec**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

Contrato del panel fijo sobre la `NavigationBar` (slot `bottomBar` del `Scaffold`). El panel es presentacional: todo estado viene del `PlayerViewModel` compartido; el panel no guarda nada.

## Props (solo lectura)

| Prop | Tipo | Regla |
|------|------|-------|
| `state` | `PlayerState` (`Idle/Buffering/Playing/Paused/Error`) | `Idle` → el panel no se compone |
| `isMuted` | `Boolean` | Solo relevante en `Buffering/Playing/Paused`; en `Error` se ignora (mute oculto) |
| `title` | `station.name`, 1 línea, ellipsis | Nunca vacío visible (si llegara vacío, fallback `"Emisora"`) |
| `subtitle` | `panelSubtitle(station)` | Formato `"{país} · {idioma} · {codec} {bitrate} kbps"` / `"Emisora de radio"` |
| `artwork` | `StationArtwork(station, 48.dp)` reutilizado | Decorativo (`contentDescription=null`; el nombre lo anuncia el texto) |

## Intents (salida del panel)

| Intent | Origen | Efecto en `PlayerViewModel` / UI |
|--------|--------|----------------------------------|
| `onToggle` | Botón principal en `Playing/Paused` | `toggle()` |
| `onCancelLoad` | Botón principal en `Buffering` | `cancelLoad()` → `Idle` (oculta el panel) |
| `onRetry` | Botón principal en `Error` | `retry()` |
| `onMute` | Botón silencio (`Playing/Paused/Buffering`) | `toggleMute()`; icono `VolumeOff` si `isMuted`, `VolumeUp` si no |
| `onCopy` | Botón copiar (todos los estados salvo `Idle`) | `resolveCopyLink(station)` → escribe al portapapeles + snackbar `"Enlace copiado"`; si `null` → snackbar `"enlace no disponible"` |
| `onOpenFull` | Toque en zona izquierda (avatar/títulos) | Abre `FullPlayerSheet` con la misma emisora (conserva `Detener`) |

## Layout por estado

| Estado | Izquierda | Derecha |
|--------|-----------|---------|
| `Buffering` | spinner + título + subtítulo | indicador de espera (cancela) + mute + copiar |
| `Playing` | avatar + título + subtítulo | pausa + mute + copiar |
| `Paused` | avatar + título + subtítulo | reanudar + mute + copiar |
| `Error` | mensaje breve ES | **reintentar + copiar** (mute oculto, FR-003b) |

Los botones de la derecha jamás abren el full-player; la zona izquierda jamás ejecuta play/mute/copiar.

## Accesibilidad (ES)

Botones: `"Pausar" / "Reanudar" / "Cancelar carga" / "Reintentar"`, `"Silenciar" / "Activar sonido"`, `"Copiar enlace"`. Panel: anuncio `"Sonando: {nombre}"` (`"Pausado: {nombre}"` si `Paused`).
