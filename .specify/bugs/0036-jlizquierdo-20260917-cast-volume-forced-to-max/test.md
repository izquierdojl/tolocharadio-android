# Bug Verification: Chromecast volume forced to 100% after connection

- **Slug**: 0036-jlizquierdo-20260917-cast-volume-forced-to-max
- **Tested**: 2026-09-17
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

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

- On-device reproduction was not performed. The fix is logically sound (reordering `bind()` before `startVolumeEvents()` ensures the receiver volume is read first), but real-world validation with a Chromecast is recommended before closing.
- Edge case: if `readVolume()` returns `null` during `bind()`, `_castVolume` stays at 1.0 and the bug persists. The assessment flagged this; a follow-up with `Float.NaN` initialization is suggested.

## Recommendation

Hold — fix verified at unit-test level; needs on-device validation with a physical Chromecast before closing.
