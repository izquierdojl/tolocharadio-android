# Bug Verification: Chromecast volume forced to 100% after connection

- **Slug**: 0036-jlizquierdo-20260917-cast-volume-forced-to-max
- **Tested**: 2026-09-17
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial (v1) → pendiente verificación on-device con v2

## Summary

The unit test that pins the fix passes, and the full PlaybackVolumeControllerTest suite plus detekt/ktlintCheck all pass with zero failures. However, the original reproduction requires a physical Chromecast device, which was not available — so on-device validation was not performed.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| New test (bug pinning) | `testDebugUnitTest --tests "...bind nunca deja el volumen en el default 100 por ciento"` | pass | Verifies `bind()` overrides default 1.0 with receiver volume immediately |
| Full PlaybackVolumeControllerTest suite | `testDebugUnitTest --tests "com.izquierdojl.tolocharadio.feature.player.PlaybackVolumeControllerTest"` | pass | 14 tests, all green, zero regressions |
| detekt | `.\gradlew.bat detekt` | pass | No issues |
| ktlintCheck | `.\gradlew.bat ktlintCheck` | pass | No issues |
| On-device reproduction | Manual with physical Chromecast | skipped | Requires physical Chromecast device not available in this environment |

## Output Excerpts

```
> Task :app:testDebugUnitTest UP-TO-DATE
BUILD SUCCESSFUL in 1s

> Task :app:detekt
> Task :app:ktlintCheck
BUILD SUCCESSFUL in 17s
```

## Residual Risks

- **v1 (primer fix, reordenar bind/collector) NO resolvió el bug en dispositivo**: el receptor saltaba al 100% nada más conectar. Root cause más profundo documentado en fix.md §Deviations: `emitDeviceVolume()` escribía al receptor vía `CastPlayer.setDeviceVolume`.
- **v2** (wrapper CastDeviceVolumePlayer + notificación sin escritura): pendiente de verificación on-device.
- Edge case: si `readVolume()` retorna null/stale 1.0 en `bind()`, la UI muestra 100% hasta el primer eco — solo display, el receptor ya no recibe escrituras.

## Recommendation

Hold — fix verified at unit-test level; needs on-device validation with a physical Chromecast before closing.
