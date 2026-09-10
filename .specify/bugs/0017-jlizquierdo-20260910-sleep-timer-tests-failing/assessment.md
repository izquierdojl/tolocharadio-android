# Bug Assessment: SleepTimer unit tests failing in build

- **Slug**: 0017-jlizquierdo-20260910-sleep-timer-tests-failing
- **Created**: 2026-09-10
- **Source**: pasted text ("Revisa los test, está fallando alguno y están fallando en la construcción")
- **Verdict**: valid
- **Severity**: high

## Report (verbatim)

User reported: "Revisa los test, está fallando alguno y están fallando en la construcción"

Build output (`gradlew test`):
- 270 tests completed, 7 failed (first run) / 8 failed (second run, added SettingsViewModelTest)
- All SleepTimer-related tests fail consistently

## Symptom

7 unit tests across `SleepTimerUseCaseTest` and `SleepTimerViewModelTest` fail with `AssertionError` or `ClassCastException`. An additional `SettingsViewModelTest` fails intermittently with `UncaughtExceptionsBeforeTest`. The build is broken on the current branch.

## Reproduction

1. Checkout branch `0016-jlizquierdo-20260909-notification-app-focus`
2. Run `.\gradlew.bat test` (with JAVA_HOME set to Android Studio JBR)
3. Observe7 SleepTimer test failures +1 SettingsViewModelTest failure

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/domain/SleepTimerUseCase.kt:52` — creates internal `CoroutineScope(SupervisorJob() + Dispatchers.Default)` that tests cannot control
- `app/src/test/java/com/izquierdojl/tolocharadio/domain/SleepTimerUseCaseTest.kt:65-85` — tests use `advanceTimeBy` on a `TestScope`, but the use case runs on `Dispatchers.Default` (real dispatcher), so time never advances
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerViewModelTest.kt:48-105` — same root cause; ViewModel delegates to the same uncontrolled use case
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt:71-76` — `users.patchMe(...)` may throw in test context

## Root Cause Hypothesis

**Confidence: high**

`SleepTimerUseCase` hardcodes its own `CoroutineScope(SupervisorJob() + Dispatchers.Default)` at line52. The unit tests create a `TestScope` with `StandardTestDispatcher` and call `advanceTimeBy()`, but the use case's internal coroutine runs on `Dispatchers.Default` (real thread pool), completely outside test control. The `advanceTimeBy` calls have zero effect on the use case's countdown loop.

This explains:
- **AssertionError** in countdown/expiration tests: state never changes because the real `delay(1000L)` runs on a real thread, not the test dispatcher
- **ClassCastException** in formatting tests: `uiState.value` is still `Inactive` (never transitions to `Active`), so casting to `SleepTimerUiState.Active` fails
- **UncaughtExceptionsBeforeTest** in ViewModel expiration tests: the coroutine completes on `Dispatchers.Default` after the test ends, leaking uncaught exceptions

The `SettingsViewModelTest` failure is a separate issue — `users.patchMe()` returns `ApiResult<UserDto>` and the relaxed mock may not handle the suspend call correctly within `viewModelScope.launch`.

## Proposed Remediation

**Preferred**: Inject the `CoroutineScope` (or `Dispatcher`) into `SleepTimerUseCase` instead of creating it internally.

```kotlin
@Singleton
class SleepTimerUseCase @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope, // or: @IoDispatcher dispatcher: CoroutineDispatcher
) {
    // Remove: private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // Use the injected scope instead
}
```

Then in tests, provide a `TestScope`:
```kotlin
private val testScope = TestScope(StandardTestDispatcher())
private val useCase = SleepTimerUseCase(testScope)
```

This follows the same DI pattern already used in the project (e.g., `@IoDispatcher`, `@ApplicationScope` qualifiers if they exist).

**Alternatives**:
- Pass a `CoroutineDispatcher` instead of full scope — simpler but requires wrapping `scope.launch` logic
- Expose the internal scope for testing — breaks encapsulation

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/domain/SleepTimerUseCase.kt` — accept injected scope
- `app/src/test/java/com/izquierdojl/tolocharadio/domain/SleepTimerUseCaseTest.kt` — pass `TestScope`
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerViewModelTest.kt` — pass `TestScope` to use case
- Hilt module — need to create `@ApplicationScope` qualifier (no existing dispatcher qualifiers found in project)

**Tests to add or update**:
- All7 failing SleepTimer tests should pass after injecting the test scope
- Verify `SettingsViewModelTest` separately (may need `coEvery` for `users.patchMe`)

## Risks & Considerations

- If `@ApplicationScope` qualifier doesn't exist, need to create one or use existing DI patterns
- The `SleepTimerUseCase` is `@Singleton` — the injected scope must also be singleton-scoped
- `SettingsViewModelTest` failure is independent and may need separate fix (mock setup for `users.patchMe`)

## Open Questions

- [NEEDS CLARIFICATION: Is the `SettingsViewModelTest` failure reproducible consistently or intermittent?]
