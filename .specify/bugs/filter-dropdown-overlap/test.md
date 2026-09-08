# Bug Verification: Filter dropdown overlap in Explore screen

- **Slug**: filter-dropdown-overlap
- **Tested**: 2026-09-08
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

The fix is correctly applied — `ExposedDropdownMenuBox` has been replaced with an inline `LazyColumn` in `FilterComboBox.kt`. However, verification is incomplete because unit tests cannot be run (JAVA_HOME not set) and manual reproduction requires a physical device or emulator.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Fix applied | Read `FilterComboBox.kt` | pass | Inline list confirmed; no `ExposedDropdownMenuBox` remains |
| Unused imports removed | Read `FilterComboBox.kt` | pass | `DropdownMenuItem`, `ExposedDropdownMenuBox`, `ExposedDropdownMenuDefaults`, `MenuAnchorType` removed |
| New imports added | Read `FilterComboBox.kt` | pass | `ListItem`, `clickable`, `heightIn` present |
| API unchanged | Read `FilterComboBox.kt` | pass | Same signature: `value`, `onValueChange`, `label`, `catalogList`, `modifier` |
| Unit tests | `./gradlew testDebugUnitTest` | not-run | JAVA_HOME not set; no JDK found in environment |
| Lint / detekt | `./gradlew detekt` | not-run | Requires JAVA_HOME |
| Manual reproduction | Device/emulator | not-run | Requires Android device or emulator |

## Output Excerpts

**Code verification** — `FilterComboBox.kt` lines 83-112 now use:
```kotlin
Column(modifier = modifier.fillMaxWidth()) {
    OutlinedTextField(...)
    if (filtered.isNotEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)...) {
            items(filtered.take(50)) { option ->
                ListItem(
                    headlineContent = { Text(option) },
                    modifier = Modifier.clickable { ... },
                )
            }
        }
    }
}
```

No popup-based components remain. Touch events will register directly on the `ListItem` within the sheet's coordinate space.

## Residual Risks

- **No runtime verification**: The fix could not be tested on-device. Edge cases (keyboard dismissing, scroll behavior with many items, accessibility) are unverified.
- **UX change**: Users now see an inline list instead of a dropdown popup. This is functionally equivalent but visually different.
- **No regression suite run**: Existing ViewModel tests were not executed due to missing JDK.

## Recommendation

**Hold — needs device verification.** The code change is structurally correct and eliminates the root cause (popup coordinate conflict), but the original symptom was a UI interaction bug that requires on-device testing to fully confirm. Run tests when JAVA_HOME is available, then manually verify filter selection on a device or emulator.
