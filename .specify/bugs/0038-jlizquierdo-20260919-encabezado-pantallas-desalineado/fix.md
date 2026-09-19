# Bug Fix: Encabezado desalineado en Mis emisoras / Favoritos / Historial

- **Slug**: 0038-jlizquierdo-20260919-encabezado-pantallas-desalineado
- **Fixed**: 2026-09-19
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Se anuló el doble inset superior en las tres pantallas afectadas pasando
`contentWindowInsets = WindowInsets(0, 0, 0, 0)` al `Scaffold` interno de
`CustomStationsScreen`, `FavoritesScreen` e `HistoryScreen`. El `Scaffold`
externo de `TolochaNavGraph` ya posiciona el contenido bajo la `TopAppBar`
(la app usa `enableEdgeToEdge`), así que el interno solo debe alojar el
`Snackbar` local. El encabezado queda ahora a 8dp de la barra como en
Configuración/Explorar, en lista y en cuadrícula.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/customstations/CustomStationsScreen.kt` | modified | `Scaffold` interno con `contentWindowInsets = WindowInsets(0, 0, 0, 0)` + import y comentario (bug 0038) |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt` | modified | mismo cambio en el `Scaffold` interno |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryScreen.kt` | modified | mismo cambio en el `Scaffold` interno |
| `app/src/androidTest/java/com/izquierdojl/tolocharadio/SectionHeaderSpacingTest.kt` | added test | regresión Compose del contrato estructural (lista + cuadrícula) |

## Diff Highlights (optional)

```kotlin
// En las 3 pantallas (ejemplo: CustomStationsScreen.kt):
import androidx.compose.foundation.layout.WindowInsets
...
    // El Scaffold externo (TolochaNavGraph) ya aplica los insets; el interno
    // solo aloja el Snackbar y no debe reañadir el inset superior (bug 0038).
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
```

## Tests Added or Updated

- `SectionHeaderSpacingTest.scaffoldInternoSinInsets_igualaReferenciaEnLista` — el patrón corregido deja el título a la misma Y que la referencia sin `Scaffold` interno (modo lista).
- `SectionHeaderSpacingTest.scaffoldInternoSinInsets_igualaReferenciaEnCuadricula` — lo mismo con cuerpo `LazyVerticalGrid` (el fallo también se daba en cuadrícula, confirmado por el usuario).
- `SectionHeaderSpacingTest.sectionHeader_respetaPaddingVerticalCanonico` — fija el padding canónico de 8dp de `SectionHeader` (spec 012, FR-006).
- `SectionHeaderSpacingTest.windowInsetsCero_noAportaPaddingSuperior` — fija que `WindowInsets(0, 0, 0, 0)` aporta 0dp de padding superior.

## Local Verification

- Commands run: `.\gradlew.bat assembleDebug` → OK.
- Commands run: `.\gradlew.bat compileDebugAndroidTestKotlin` → OK (el test nuevo compila).
- Commands run: `.\gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug` → BUILD SUCCESSFUL (tras corregir formato ktlint del test nuevo: firmas multilínea con coma final).
- `connectedDebugAndroidTest` → NO ejecutado: no hay dispositivo/emulador ni `adb` en este entorno. Pendiente en el siguiente paso (`/speckit.bug.test`).
- Manual checks: pendientes (ver Follow-ups).

## Deviations from Assessment

- El test no compone las pantallas completas como sugería el assessment, porque requieren Hilt (`hiltViewModel`) y el repo no tiene infra de Hilt testing (`hilt-android-testing`); añadirla para este fix cosmético sería desproporcionado. En su lugar, el test fija el contrato estructural con el mismo patrón exacto (`Scaffold` externo con `TopAppBar` + `Scaffold` interno con insets a cero) en ambos modos. Con `enableEdgeToEdge` activo, el caso pre-fix fallaría en dispositivo real (inset de status bar no nulo) y el post-fix pasa en cualquier entorno.

## Follow-ups

- Ejecutar `.\gradlew.bat connectedDebugAndroidTest` con dispositivo/emulador (incluye `SectionHeaderSpacingTest`).
- Verificación manual en emulador (spec 012 quickstart): comparar las 5 secciones en claro/oscuro; el espacio TopAppBar→título debe ser idéntico y ≤8dp, y el `Snackbar` (p. ej. "Favorita eliminada / Deshacer") debe seguir visible sobre el mini-player.
- `ServerListScreen` usa un `Scaffold` interno similar; quedó fuera de alcance a propósito (no reportado). Valorar bug aparte si se observa el mismo hueco.
