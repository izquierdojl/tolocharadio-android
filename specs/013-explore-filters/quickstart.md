# Quickstart: Filtros avanzados de Explorar

**Date**: 2026-09-07
**Feature**: 013-explore-filters

## Prerequisites

- Android Studio installed
- Android SDK 35 (minSdk 26)
- Running TolochaRadio server instance
- App configured with valid server URL

## Validation Scenarios

### Scenario 1: Filter by Country

1. Open the app and navigate to **Explorar**
2. Wait for filter lists to load (spinner disappears)
3. Tap on the **País** dropdown
4. Type "Esp" in the dropdown
5. Select "Spain" from the suggestions
6. Tap **Buscar**
7. **Expected**: All displayed stations have country "Spain"

### Scenario 2: Filter by Language

1. Navigate to **Explorar**
2. Tap on the **Idioma** dropdown
3. Select "Spanish" from the list
4. Tap **Buscar**
5. **Expected**: All displayed stations have language "Spanish"

### Scenario 3: Filter by Genre/Tag

1. Navigate to **Explorar**
2. Tap on the **Género** dropdown
3. Type "jazz"
4. Select "jazz" from suggestions
5. Tap **Buscar**
6. **Expected**: All displayed stations have "jazz" in their tags

### Scenario 4: Combined Filters

1. Navigate to **Explorar**
2. Select country "Spain"
3. Select language "Spanish"
4. Select genre "rock"
5. Type "FM" in the name field
6. Tap **Buscar**
7. **Expected**: Results match ALL criteria (Spain + Spanish + rock + name contains "FM")

### Scenario 5: Clear Filters

1. Apply any filters as in scenarios above
2. Tap **Limpiar filtros**
3. **Expected**: All filter fields empty, results reload without filters

### Scenario 6: Degraded Mode (Network Error)

1. Disable network connection
2. Navigate to **Explorar**
3. **Expected**: Filter fields show "No se pudo cargar la lista" warning
4. Type a country name manually (e.g., "Spain")
5. Tap **Buscar**
6. **Expected**: Search executes with manual value (may show cached results or error)

### Scenario 7: Loading State

1. Navigate to **Explorar**
2. **Expected**: Filter fields are disabled with spinner while lists load
3. After lists load (<2 seconds), fields become enabled
4. **Expected**: Can now interact with filter dropdowns

### Scenario 8: No Results

1. Apply filters that match no stations (e.g., country "Atlantis")
2. Tap **Buscar**
3. **Expected**: "Sin resultados" message with option to clear filters

## Test Commands

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.feature.explore.ExploreViewModelsTest"

# Run instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest
```

## Expected Outcomes

- All 8 scenarios pass
- Filter lists load in <2 seconds
- No regressions in existing search functionality
- "Cargar más" pagination still works
- View mode toggle (list/grid) still works
- Favorites toggle still works
