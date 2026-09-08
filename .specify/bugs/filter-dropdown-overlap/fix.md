# Bug Fix: Filter dropdown overlap in Explore screen

- **Slug**: filter-dropdown-overlap
- **Fixed**: 2026-09-08
- **Status**: applied

## Summary

Two changes were made:
1. Replaced `ExposedDropdownMenuBox` with inline `LazyColumn` to fix coordinate mapping issues in `ModalBottomSheet`
2. Added "Aceptar" (Accept) button so users can type and review before applying the filter

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/FilterComboBox.kt` | modified | Replaced popup-based dropdown with inline list |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreScreen.kt` | modified | Added Accept button; filter only applies on explicit user action |

## Diff Highlights

### FilterComboBox.kt
**Before**: Used `ExposedDropdownMenuBox` with popup dropdown
**After**: Uses `Column` + `LazyColumn` with inline list items

Key behavior:
- Typing updates local `textValue` only (no callback)
- Clicking a suggestion updates `textValue` AND calls `onValueChange`
- No immediate filter application

### ExploreScreen.kt
**Before**: `onValueChange` immediately applied filter and closed sheet
**After**: `onValueChange` updates `selectedValue` state; "Aceptar" button applies filter

Key flow:
1. User opens filter chip → bottom sheet appears
2. User types → local text updates, list filters
3. User clicks suggestion → text field filled (optional)
4. User clicks "Aceptar" → filter applied, sheet closes

## Tests Added or Updated

- No new tests added (existing ViewModel tests cover filter logic; UI tests would require device/emulator)

## Local Verification

- Commands run: `./gradlew testDebugUnitTest` → **skipped** (JAVA_HOME not set in environment)
- Manual checks: Code reviewed for correctness

## Deviations from Assessment

Added "Aceptar" button requirement not in original assessment — this addresses the user's feedback that typing should not immediately trigger search.

## Follow-ups

- Run full test suite when Java environment is available
- Consider adding Compose UI tests for filter selection flow
