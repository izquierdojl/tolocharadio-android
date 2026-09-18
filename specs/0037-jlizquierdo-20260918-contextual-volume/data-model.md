# Data Model: Volumen contextual único estilo Pocket Casts

**Feature**: 0037-jlizquierdo-20260918-contextual-volume | **Fecha**: 2026-09-18

El volumen deja de ser estado de la app: es propiedad de la salida (teléfono o receptor
Cast) y lo representa la barra del sistema. La app solo conserva el **silencio** como
estado propio. Sin persistencia (estado de sesión), sin cambios de Room/DataStore ni de
backend.

## Estado

### `PlaybackVolumeController` (antes/después)

| Campo | 0035 (actual) | 0037 | Motivo |
|-------|---------------|------|--------|
| `muted: StateFlow<Boolean>` | sí | **sí** (conservado) | Silencio único de la app, aplicado a la salida activa (FR-006) |
| origen del silencio (interno: usuario / eco) | — (bool plano) | **nuevo** | Solo el silencio iniciado por el usuario sobrevive al desconectar; el adoptado del receptor (eco externo) se descarta (FR-006, edge case B2) |
| `castVolume: StateFlow<Float>` | sí | **eliminado** | El volumen real lo lee/representa el sistema vía player nativo (FR-004, research D1) |
| `castVolumeSupported: StateFlow<Boolean>` | sí | **eliminado** | Q1: sin degradación especial (FR-011, research D8) |
| `notices: SharedFlow<Unit>` | sí | **eliminado** | Idem: no hay avisos de volumen |
| `pendingVolume` + ventana de confirmación | sí | **eliminado** | La confirmación por eco es nativa (`onDeviceVolumeChanged`) |
| `device: RemoteVolumeDevice?` | sí | **sí** (conservado) | Salida remota activa para silencio |
| `isRemoteActive` | sí | **sí** (conservado) | ¿El silencio se aplica al receptor? |

### `RemoteVolumeDevice` (contrato del dispositivo remoto)

| Operación | 0035 | 0037 |
|-----------|------|------|
| `readVolume()/writeVolume()` | sí | **eliminado** (nadie escribe/lee volumen desde la app) |
| `readMuted()/writeMuted()` | sí | **sí** |
| `observe()/stopObserving()` | sí | **sí** (eco de cambios de silencio del receptor) |

### Entidades de la spec

- **Salida activa**: teléfono (ExoPlayer local) o dispositivo Cast conectado. Determina el
  destino de las teclas (nativo, cambia con el player de la sesión) y de `writeMuted`.
- **Silencio de la app**: `muted`, única pieza de estado de la feature. Se aplica a la
  salida activa, sobrevive al cambio de salida (FR-006) y se sincroniza con el eco del
  receptor (US3.2).

## Invariantes

1. Conectar/desconectar Cast **nunca** escribe volumen en el receptor (FR-001).
2. `bind()` escribe el silencio al receptor **solo** si la app estaba silenciada; en caso
   contrario **lee** el silencio real del receptor y lo representa (FR-005/FR-006).
3. El eco del receptor (`observe` → cambio de silencio) actualiza `muted` sin avisos y
   marca su origen como **eco** (FR-008 análogo para silencio).
4. El silencio tiene **origen**: usuario (botón/notificación) o eco externo. Solo el de
   usuario sobrevive al cambio de salida (`unbind()` lo aplica al `ExoPlayer` local); el
   adoptado del receptor se descarta al desconectar: `muted → false` y el local conserva
   su volumen (FR-006, edge case B2).
5. El player de la sesión es siempre un player "crudo" de media3 (ExoPlayer o CastPlayer),
  sin wrappers (research D6).
