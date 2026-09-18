# Contract: Volumen contextual único

**Feature**: 0037-jlizquierdo-20260918-contextual-volume | **Fecha**: 2026-09-18

## §1. Player de la sesión (quién representa el volumen)

| Situación | Player de la sesión | Volumen de teclas/barra del sistema |
|-----------|---------------------|-------------------------------------|
| Reproducción local | `ExoPlayer` | Volumen del teléfono (nativo) |
| Sesión Cast activa | `CastPlayer` (crudo, media3 1.11.0) | Volumen del dispositivo Cast, barra identificada con su nombre (nativo) |
| Receptor sin soporte de volumen | igual (sin gestión especial) | El ajuste no tiene efecto; reproducción y sesión intactas (FR-011) |

- **PROHIBIDO** envolver el player de la sesión con wrappers de volumen (regresión 0036
  fix v2, research D6).
- **PROHIBIDO** que la app escriba volumen en el receptor como efecto lateral de
  conectar/desconectar/reconectar (FR-001).

## §2. `PlaybackVolumeController` (silencio)

API pública conservada: `muted`, `isRemoteActive`, `bind()`, `unbind()`, `toggleMute()`,
`setMuted()`, `resetMute()`.

### Semántica de `bind(device)` (FR-005/FR-006)

| Estado de la app al conectar | Acción sobre el receptor | `muted` resultante |
|------------------------------|--------------------------|--------------------|
| app silenciada | `writeMuted(true)` (carry-over) | `true` |
| app sin silencio, receptor silenciado | **no escribe nada**; lee y representa | `true` (eco) |
| app sin silencio, receptor sin silencio | no escribe nada | `false` |

### Semántica de `unbind()`

- Silencio con origen **usuario** (botón/notificación): se aplica al `ExoPlayer` local
  (carry-over inverso, FR-006).
- Silencio con origen **eco** (adoptado de un receptor silenciado externamente): se
  descarta — `muted → false` y el local conserva su volumen; la reproducción local retoma
  con sonido (FR-006, edge case B2).
- En ningún caso escribe en el receptor.

### Eco (`observe` → cambio en el receptor)

- Cambio de silencio del receptor (mando del TV, Google Home): sincroniza `muted` en
  silencio, sin avisos, y marca el origen como **eco**.
- Cambio de volumen del receptor: la app **no** mantiene estado de volumen; nada que
  sincronizar (lo representa la barra del sistema).

## §3. UI (franja única)

- El reproductor completo (full-player) **NO** contiene control de volumen con Cast activo
  (FR-004; sustituye FR-003/FR-004 de la 0035).
- El botón de silencio (mini-player, full-player y comando MUTE de la notificación) se
  conserva y delega en `toggleMute()` (FR-006).
- Sin snackbars ni avisos de volumen (FR-011, Q1).

## §4. Regresión local (fuera de Cast)

- Sin Cast, el volumen del teléfono se controla como siempre (teclas + barra del sistema).
- `play`/`stop` siguen llamando `resetMute()` (regla de la spec 004).
- El volumen multimedia del teléfono nunca cambia por efecto del control remoto (FR-002).
