# Bug Fix: cabecera de búsqueda fija en Descubrir de podcasts

- **Slug**: 0059-izquierdojl-20261124-podcast-search-scroll
- **Fixed**: 2026-11-24
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

`DiscoverContent` usa ahora un único `LazyColumn` para toda la pantalla: cabecera, búsqueda/RSS/OPML, resultados y ranking (incluidos sus selectores) se desplazan juntos. `TopChartsSection` aporta sus ítems mediante `topChartsItems` y conserva un envoltorio independiente para sus tests, eliminando los `weight(1f)` que anclaban el contenido inferior.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverContent.kt` | modified | `Column` + `weight(1f)` → un único `LazyColumn` con `item {…}`/`items(…)`; etiquetas `TOP_CHARTS_SECTION`/`TOP_CHARTS_LIST` aplicadas solo cuando el ranking está visible. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/TopChartsSection.kt` | modified | Extraída `LazyListScope.topChartsItems`; sin `weight`, con `padding` en carga/vacío; `TopChartsSection` queda como `LazyColumn` envolvente para uso aislado. |
| `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverContentTest.kt` | modified | Helper con altura opcional y test de regresión con `performScrollToNode` sobre `TOP_CHARTS_LIST` (corregido tras la primera verificación). |

## Diff Highlights (optional)

```kotlin
// DiscoverContent.kt
val rankingVisible = ui.submittedQuery.isBlank() && topChartsUi.visible
LazyColumn(
    modifier = Modifier.fillMaxSize().then(
        if (rankingVisible) Modifier.testTag(TolochaTestTags.TOP_CHARTS_LIST) else Modifier,
    ),
    contentPadding = PaddingValues(bottom = 16.dp),
) {
    item { SectionHeader(title = "Descubrir", …) }
    item { Column(Modifier.padding(horizontal = 16.dp)) { /* búsqueda, RSS, OPML */ } }
    topChartsItems(…) // o items(ui.results, …), según el estado
}
```

```kotlin
// TopChartsSection.kt
@Suppress("LongMethod")
fun LazyListScope.topChartsItems(…) {
    item { Text("Populares", …) }
    item { Row { CountrySelector(…); GenreSelector(…) } }
    // carga / error / aviso / vacío como item; ranking con itemsIndexed(…)
}
```

## Tests Added or Updated

- `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverContentTest.kt::conPocoAlto_seDesplazaTodaLaPantallaHastaElFinalDelRanking` — con 320 dp de alto, comprueba que «Buscar podcasts» está visible al inicio, que se puede llegar al selector de país y a la última fila del ranking desplazando el `LazyColumn` etiquetado (`performScrollToNode(hasTestTag(…))` / `performScrollToNode(hasText(…))`), y que el campo de búsqueda ya no está compuesto al final.
- Tests existentes de `DiscoverContentTest` y `TopChartsSectionTest` — sin cambios de aserción; todos pasan en emulador.

## Local Verification

- Commands run: `./gradlew compileDebugAndroidTestKotlin testDebugUnitTest detekt ktlintCheck lintDebug` → **BUILD SUCCESSFUL** (765 tests unitarios en 143 suites, 0 fallos; detekt, ktlint y lint sin issues nuevos).
- Commands run: `./gradlew connectedDebugAndroidTest` en AVD `Medium_Phone_API_37.0` → **BUILD SUCCESSFUL** (141 tests instrumentados, 0 fallos; incluye el nuevo test de regresión).
- Commands run: `./gradlew compileDebugAndroidTestKotlin detekt ktlintCheck` → **BUILD SUCCESSFUL** tras corregir el test.
- Manual checks: revisión del layout: ya no hay `weight(1f)` en áreas de contenido ni `LazyColumn` anidados en la ruta de Descubrir.

## Deviations from Assessment

- Ninguna desviación funcional: se siguió la remediación preferida (un único contenedor desplazable, sin `weight` y conservando tags/callbacks).
- `TopChartsSection` se mantuvo como composable envolvente con su propio `LazyColumn` porque `TopChartsSectionTest` lo usa de forma aislada; los ítems reales se extrajeron a `topChartsItems` para compartirlos con `DiscoverContent`.
- No se enmendó `specs/0053-izquierdojl-20261122-podcast-top-charts/plan.md:122` («no anida scrolls» con `weight(1f)`): se documenta aquí la desviación, como permitía el assessment, para no ampliar el alcance del bug.
- Los estados de carga/vacío del ranking usan `padding` en lugar de `weight(1f)` + centrado vertical; el centrado no tenía efecto al eliminar la altura ponderada.
- Segunda pasada (tras `/speckit.bug.test`): el test usaba `performScrollTo()` sobre un nodo aún no compuesto por `LazyColumn`; se sustituyó por `performScrollToNode` sobre el contenedor etiquetado. Solo cambió el test, no el código de producción.

## Follow-ups

- Si el cambio de UX se confirma, actualizar `specs/0053-.../plan.md:122` y `contracts/top-charts-ui.md` para reflejar el scroll único.
- [NEEDS CLARIFICATION del assessment] Comprobar en dispositivo real con teclado abierto y fuente grande; si el teclado tapa el último selector, valorar `imePadding` en el `Scaffold` como bug separado.
