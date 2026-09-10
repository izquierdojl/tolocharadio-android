# Bug Fix: SleepTimer unit tests failing in build

- **Slug**: 0017-jlizquierdo-20260910-sleep-timer-tests-failing
- **Fixed**: 2026-09-10
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

`SleepTimerUseCase` now receives an injected `@ApplicationScope CoroutineScope` (Hilt) instead of creating its own `CoroutineScope(SupervisorJob() + Dispatchers.Default)`, so tests can drive the countdown with virtual time. The SleepTimer tests were updated to use the injected `TestScope`. Additionally, a latent test leak in `PlayerViewModelTest` (an unstubbed `PlaybackRepo.status` call throwing `MockKException` inside a coroutine after the test ended) was fixed at its source; it was surfacing as `UncaughtExceptionsBeforeTest` in `SettingsViewModelTest` and, intermittently, in `ServerRepositoryTest`.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/domain/SleepTimerUseCase.kt` | modified | Constructor takes `@ApplicationScope scope: CoroutineScope`; removed internal real-dispatcher scope; `release()` delegates to `cancel()` |
| `app/src/main/java/com/izquierdojl/tolocharadio/di/ApplicationScope.kt` | added | `@Qualifier` annotation for the application scope |
| `app/src/main/java/com/izquierdojl/tolocharadio/di/CoroutineModule.kt` | added | Hilt `SingletonComponent` module providing `@ApplicationScope CoroutineScope` |
| `app/src/test/java/com/izquierdojl/tolocharadio/domain/SleepTimerUseCaseTest.kt` | modified | Passes `TestScope(StandardTestDispatcher())`; countdown test advances 3500 ms to complete 3 ticks |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerViewModelTest.kt` | modified | Passes `TestScope(UnconfinedTestDispatcher())`; `uiState` tests collect into a list (activates `WhileSubscribed` and captures mid-countdown values) |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` | modified | Added `@Before` default stub `coEvery { playback.status(any()) } coAnswers { awaitCancellation() }` |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModelTest.kt` | modified | Explicit `coEvery { users.patchMe(any(), any()) } returns ApiResult.Ok(...)` |

## Diff Highlights

```kotlin
// SleepTimerUseCase.kt
@Singleton
class SleepTimerUseCase
    @Inject
    constructor(
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        // countdownJob = scope.launch { ... }  // uses injected scope
```

```kotlin
// PlayerViewModelTest.kt — prevents MockKException leaking from a coroutine
@Before
fun setup() {
    coEvery { playback.status(any()) } coAnswers { awaitCancellation() }
}
```

```kotlin
// SleepTimerViewModelTest.kt — activate stateIn(WhileSubscribed) and read
val states = mutableListOf<SleepTimerUiState>()
val subscription = launch { viewModel.uiState.collect { states.add(it) } }
viewModel.start(SleepTimerDuration.MINUTES_15)
advanceTimeBy(3500L)
assertEquals("14:57", (states.last() as SleepTimerUiState.Active).remainingFormatted)
subscription.cancel()
```

## Tests Added or Updated

- `SleepTimerUseCaseTest` (7 tests) — countdown now deterministic with injected `TestScope`; 3500 ms = 3 completed `delay(1000)` ticks.
- `SleepTimerViewModelTest` (8 tests) — `uiState` assertions subscribe first, so `WhileSubscribed` starts and intermediate values are observable.
- `PlayerViewModelTest` — no behavioral change; default suspending stub eliminates the leaked uncaught exception.
- `SettingsViewModelTest` — explicit `patchMe` stub removes reliance on relaxed-mock behavior.

## Local Verification

- Commands run: `.\gradlew.bat testDebugUnitTest` (JAVA_HOME = Android Studio JBR) → BUILD SUCCESSFUL, 232 tests, 0 failures.
- Commands run: `.\gradlew.bat test --rerun-tasks` → BUILD SUCCESSFUL (clean, non-incremental execution of the assessment's original command).
- Manual checks: XML reports inspected under `app/build/test-results/testDebugUnitTest/` to confirm the removed failures (`expected:<897> but was:<898>`, `ClassCastException` on `SleepTimerUiState.Inactive`, `UncaughtExceptionsBeforeTest`).

## Deviations from Assessment

1. **Assessment hypothesis for `SettingsViewModelTest` was incorrect.** It was not caused by the `users.patchMe` sealed `ApiResult` mock. The real cause was `PlayerViewModelTest` leaking an uncaught `MockKException` (`no answer found for PlaybackRepo.status(...)`) from a coroutine started by `play()`; `runTest` in the next executed test reported it as `UncaughtExceptionsBeforeTest`. Fixed at the source in `PlayerViewModelTest`.
2. **Dispatcher choice for the ViewModel tests.** The assessment suggested `StandardTestDispatcher`; the ViewModel test scope uses `UnconfinedTestDispatcher` so `stateIn(SharingStarted.WhileSubscribed)` collects eagerly while asserting mid-countdown values. The UseCase test keeps `StandardTestDispatcher` with explicit `advanceTimeBy`.
3. **`SleepTimerUseCase.release()`** now calls `cancel()` only; it no longer cancels the injected scope (it is application-scoped and shared).

## Follow-ups

- Optional: silence the KT-73255 annotation-target warning on `SleepTimerUseCase` by using `@param:ApplicationScope` (or adding `-Xannotation-default-target=param-property`).
- Pre-existing, unrelated Gradle deprecation warnings (`android.builtInKotlin=false`, `android.newDsl=false`) remain untouched.
