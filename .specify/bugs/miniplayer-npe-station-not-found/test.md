# Bug Verification: NullPointerException in MiniPlayer when station is unavailable

- **Slug**: miniplayer-npe-station-not-found
- **Tested**: 2026-09-08
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The NPE crash is fixed. Automatic retry was causing an infinite loop and has been removed entirely — the player now stops at the first error and shows it to the user. Manual retry via the refresh button still works.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Manual test by user on browser | pass | NPE fixed, no more crash |
| Retry loop fix | Manual test by user | pass | Infinite loop eliminated — single attempt only |
| New / updated tests | `testDebugUnitTest --tests PlayerViewModelTest` | pass | 11/11 passed |
| Compilation | `compileDebugKotlin` | pass | Clean build, no errors |

## Output Excerpts

**PlayerViewModelTest:**
```
BUILD SUCCESSFUL in 43s
33 actionable tasks: 15 executed, 18 up-to-date
```

## Residual Risks

- No automatic retry means transient network errors require user action (tap retry button). This is acceptable per user preference.
- The `MediaRouteButton` crash reported in the same logcat is a separate issue.

## Recommendation

Close the bug — verified. NPE fixed, infinite retry loop eliminated.
