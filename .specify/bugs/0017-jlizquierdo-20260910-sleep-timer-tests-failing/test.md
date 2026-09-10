# Bug Verification: SleepTimer unit tests failing in build

- **Slug**: 0017-jlizquierdo-20260910-sleep-timer-tests-failing
- **Tested**: 2026-09-10
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The original symptom (7 SleepTimer test failures + 1 intermittent `SettingsViewModelTest` failure breaking `gradlew test`) no longer reproduces. The full unit test suite passes cleanly (232 tests, 0 failures/errors) after the fix, including a non-incremental rerun. The real root cause of the `SettingsViewModelTest` failure turned out to be an uncaught `MockKException` leaked by `PlayerViewModelTest`, now fixed at its source.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | `.\gradlew.bat test --rerun-tasks` (JAVA_HOME = Android Studio JBR) | pass | The assessment's reproduction command; BUILD SUCCESSFUL, no failures |
| New / updated tests | `.\gradlew.bat testDebugUnitTest --tests ...SleepTimerUseCaseTest --tests ...SleepTimerViewModelTest --tests ...PlayerViewModelTest --tests ...SettingsViewModelTest` | pass | BUILD SUCCESSFUL; the four classes touched by the fix |
| Regression suite | `.\gradlew.bat testDebugUnitTest --rerun-tasks` | pass | 232 tests total, 0 failures/errors, 0 skipped |
| Lint / type-check | — | skipped | No lint task run (not configured in the routine workflow); main + unit-test Kotlin sources compiled successfully as part of the test runs |

## Output Excerpts

```
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 1m 18s
```

Per-class XML results from `app/build/test-results/testDebugUnitTest/`:

```
SleepTimerUseCaseTest:     tests=7  failures=0 errors=0 skipped=0
SleepTimerViewModelTest:   tests=8  failures=0 errors=0 skipped=0
PlayerViewModelTest:       tests=11 failures=0 errors=0 skipped=0
SettingsViewModelTest:     tests=7  failures=0 errors=0 skipped=0
ServerRepositoryTest:      tests=6  failures=0 errors=0 skipped=0
TOTAL:                     tests=232 failures+errors=0
```

## Residual Risks

- `SleepTimerViewModel` uses `stateIn(WhileSubscribed)`; the tests now subscribe before asserting. A future refactor of that sharing strategy could require test updates.
- `PlayerViewModelTest` relies on a default `awaitCancellation()` stub for `PlaybackRepo.status`; tests overriding it must keep doing so explicitly.
- The `ServerRepositoryTest` `UncaughtExceptionsBeforeTest` seen before was a downstream symptom of the same leaked exception; it has not reappeared in consecutive clean runs, but the leak mechanism (uncaught coroutine exceptions reported into a later `runTest`) is worth watching if new tests launch coroutines in `viewModelScope`.
- Only JVM unit tests were exercised; no instrumentation/device tests were run (not part of the reported symptom).

## Recommendation

Close the bug — verified end-to-end. `/speckit.bug.fix` produced a passing build and the original failing tests (`SleepTimerUseCaseTest`, `SleepTimerViewModelTest`, `SettingsViewModelTest`) all pass, with the entire 232-test unit suite green across two clean runs.
