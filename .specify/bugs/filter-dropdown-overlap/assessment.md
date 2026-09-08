# Bug Assessment: Filter dropdown overlap in Explore screen

- **Slug**: filter-dropdown-overlap
- **Created**: 2026-09-08
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: medium

## Report (verbatim)

Los filtros por pais e idioma buscan mal, se indica una letra y al pulsar sobre la segunda se sobrepone la búsqueda y pulsa debajo. Hay algo mal organizado o estructurado. Revisalo y mejoremos este aspecto.

## Symptom

When typing a letter in the country or language filter fields inside the `ModalBottomSheet`, the dropdown list appears but clicking on an item does not work correctly. The touch/click goes "below" the intended target, causing the search to overlap and the selection to fail. The user cannot properly select a filter option from the dropdown.

## Reproduction

1. Open the Explore screen
2. Click on the "País" (Country) or "Idioma" (Language) filter chip
3. The bottom sheet opens with the filter selector
4. Type a letter (e.g., "E") in the text field
5. The dropdown shows filtered results
6. Try to click on a result item
7. The click goes to the wrong position (below the intended target)

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/FilterComboBox.kt:85-121` — The `ExposedDropdownMenuBox` inside the bottom sheet causes coordinate mapping issues
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreScreen.kt:142-193` — The `ModalBottomSheet` wraps the `FilterComboBox` which may interfere with dropdown positioning

## Root Cause Hypothesis

Confidence: high

The `ExposedDropdownMenuBox` component from Material3 has known issues when used inside a `ModalBottomSheet`. The dropdown menu is rendered as a popup that uses absolute coordinates, but when nested inside a modal sheet, the touch event coordinates are not correctly mapped to the dropdown items. This causes the classic "click below" behavior where the user taps on an item but the click registers on the item below it (or not at all).

The issue is that `ExposedDropdownMenuBox` uses `Popup` internally which calculates position relative to the window, but `ModalBottomSheet` also uses a `Popup`/overlay mechanism that can interfere with the coordinate system.

## Proposed Remediation

**Preferred**: Replace `ExposedDropdownMenuBox` with a custom implementation that uses a `DropdownMenu` (not `ExposedDropdownMenuBox`) positioned correctly, or use a simple `LazyColumn` with filter items displayed directly in the bottom sheet without a dropdown popup.

Specifically:
1. In `FilterComboBox.kt`, replace the `ExposedDropdownMenuBox` with a simpler approach:
   - Use a regular `OutlinedTextField` for input
   - Below it, show a `LazyColumn` or `Column` with filtered items (not in a popup)
   - This avoids the popup coordinate issue entirely

**Alternatives**:
- Use `DropdownMenu` directly with explicit anchor positioning instead of `ExposedDropdownMenuBox`
- Keep the current approach but add `Modifier.clipToBounds()` and adjust the popup positioning (less reliable)

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/FilterComboBox.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreScreen.kt` (if the bottom sheet wrapper needs adjustment)

**Tests to add or update**:
- UI test for filter selection inside bottom sheet
- Test that typing filters and selecting items works correctly

## Risks & Considerations

- Changing the filter UI may affect user experience (visual change)
- The new implementation should maintain accessibility (keyboard navigation, screen readers)
- Performance: showing a list directly instead of a dropdown may use more vertical space, but inside a bottom sheet this is acceptable
- Need to ensure the list scrolls properly if there are many items (currently limited to 50 with `.take(50)`)

## Open Questions

- None — the root cause is clear from the code structure.
