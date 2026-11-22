# Bug Verification: Cortes intermitentes en emisoras HLS por timeout de lectura

- **Slug**: 0045-jlizquierdo-20261101-hls-live-cortes-intermitentes
- **Tested**: 2026-11-01 (dispositivo, registrado 2026-11-22)
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

El `readTimeout` del cliente de reproducción se eliminó para los streams en
directo y la verificación en dispositivo con el APK `v1.18.1` confirmó que las
emisoras HLS se mantienen estables, sin los cortes intermitentes anteriores.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Test específico | `./gradlew testDebugUnitTest --tests "...PlaybackHttpClientTest"` | pass | Cubre el timeout de lectura. |
| Suite completa | `./gradlew testDebugUnitTest` | pass | Sin regresiones. |
| Gates | `./gradlew detekt ktlintCheck lintDebug` | pass | |
| Build | `./gradlew assembleDebug` | pass | |
| Reproducción real | Onda Cero HLS con el backend v0.29.1, APK `v1.18.1` | pass | Segmentos con huecos de 11,1–11,8 s; sin cortes. |

## Output Excerpts

> Prueba en dispositivo: **correcta** (usuario, 2026-11-01). La reproducción de
> emisoras HLS con el APK `v1.18.1` se mantiene estable.

## Residual Risks

- El fix se apoya en el watchdog de `PlaybackRecoveryManager` para detectar
  ausencia real de progreso; cubierto por las specs 0042/0044.
- No se repitió la medición en esta sesión: se registra la verificación
  histórica del usuario que consta en `fix.md`.

## Recommendation

Cerrar el bug: verificado en dispositivo y con tests que impiden la regresión.
