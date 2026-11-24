# Bug Assessment: cabecera de búsqueda fija en Descubrir de podcasts

- **Slug**: 0059-izquierdojl-20261124-podcast-search-scroll
- **Created**: 2026-11-24
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: medium

## Report (verbatim or summarized)

> En la pantalla de podcast de buscar, el apartado de busqueda por texto y suscripción se queda fijo, dejando poco espacio a los selectores de podcast mas escuchados por pais, tipo,etc... Debería desplazarse toda la pantalla hacia arriba para dejar más espacio a los inferiores, revisar

No se aportó URL ni captura; el reporte se evalúa únicamente contra el código y las specs del repo.

## Symptom

En la pestaña «Descubrir» de podcasts (la que el reporte llama «buscar»), el bloque de búsqueda por texto, URL RSS y acciones de suscripción/OPML permanece anclado y no se desplaza; el título «Populares» y los selectores de país y género tampoco se desplazan porque están fuera del `LazyColumn` inferior. El espacio restante para ver el ranking y manejar sus filtros queda reducido, especialmente con pantallas bajas, fuentes grandes o teclado abierto.

## Reproduction

1. Abrir la app con un servidor que exponga la capacidad `podcasts` + `discovery == itunes` (para que «Populares» esté visible).
2. Ir a la pestaña inferior «Descubrir» de Podcasts.
3. Observar que el bloque «Buscar podcasts» / «URL RSS manual» / botones OPML y Suscripciones ocupa su altura natural de forma permanente.
4. Deslizar el dedo sobre ese bloque y sobre los selectores de país/género de «Populares»: no se desplaza nada; solo se desplazan las filas del ranking dentro del cajón que queda debajo.
5. [NEEDS CLARIFICATION: dispositivo, tamaño de pantalla y estado del teclado en el que se reprodujo] — el informe no lo especifica, pero el código muestra que el efecto empeora con menos altura disponible.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverContent.kt:60-109` — `Column(fillMaxSize())` sin `verticalScroll`; el bloque de búsqueda/RSS/OPML es un `Column` hijo con altura natural y sin peso.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverContent.kt:111-182` — el contenido inferior (resultados, ranking o vacío) recibe `weight(1f)`, por lo que absorbe solo el remanente; los estados de carga/vacío también usan `weight(1f)`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/TopChartsSection.kt:63-95` — título «Populares» y selectores `CountrySelector`/`GenreSelector` están fuera del `LazyColumn`, dentro del `Column` con `weight(1f)` heredado.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/TopChartsSection.kt:97-157` — carga, vacío y lista usan `weight(1f)`; el scroll real ocurre solo en el `LazyColumn` de las filas (`:141-147`).
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt:211-269` — `Scaffold` con `topBar`/`bottomBar` fijos; `NavHost` solo aplica `padding`, sin `imePadding` en esta pantalla.
- `specs/0053-izquierdojl-20261122-podcast-top-charts/plan.md:122` — decisión original: «La sección ocupa el espacio restante con `weight(1f)` y no anida scrolls», que es el patrón que produce el síntoma.
- `specs/0053-izquierdojl-20261122-podcast-top-charts/contracts/top-charts-ui.md:106-121` — contrato de composición; exige controles «siempre presentes y operativos», no que sean inamovibles.

## Root Cause Hypothesis

El contenedor raíz de `DiscoverContent` no tiene ningún modificador de desplazamiento (`VerticalScroll`/`LazyColumn`): la búsqueda, el RSS y las acciones de suscripción se dibujan a su altura natural y quedan anclados, mientras que el único área desplazable es el `LazyColumn` de resultados o el del ranking, ambos forzados a `weight(1f)`. Como el título y los selectores de «Populares» viven fuera de ese `LazyColumn`, no hay forma de desplazarlos hacia arriba para ganar espacio. La ausencia de `imePadding` agrava el caso al abrir el teclado. Confianza: **high** (el layout está explicitamente descrito en `plan.md:122` y coincide con el síntoma).

## Proposed Remediation

**Preferred**: unificar la pantalla en un único contenedor desplazable. En `DiscoverContent.kt`, reemplazar el `Column(fillMaxSize())` por un `LazyColumn` (o `Column` con `verticalScroll(rememberScrollState())`) que incluya, en este orden: `SectionHeader`, el bloque de búsqueda/RSS/OPML y el estado correspondiente (resultados / ranking / vacío). En `TopChartsSection.kt`, eliminar los `weight(1f)` de `:99-146` y dejar que título, selectores, avisos y filas sean ítems del mismo scroll (por ejemplo `item { … }` + `itemsIndexed(ui.items, …)`), conservando los `testTag` existentes (`TOP_CHARTS_SECTION`, `TOP_CHARTS_COUNTRY`, `TOP_CHARTS_GENRE`, `TOP_CHARTS_LIST`) y todas las claves y callbacks. La búsqueda y los botones siguen presentes y operativos (FR-012 de la spec 0046 y contrato de 0053), pero ya no roban espacio permanentemente.

**Alternatives**:
- Mantener dos zonas y convertir la cabecera en `stickyHeader` del `LazyColumn` de resultados/ranking: más simple, pero la cabecera seguiría ocupando espacio fijo y no permite «desplazar toda la pantalla hacia arriba», que es lo pedido.
- Hacer plegable el bloque de búsqueda al enfocar/desenfocar: evita reestructurar el layout, pero cambia el contrato «siempre presentes y operativos» y añade estado nuevo; no se recomienda.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverContent.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/TopChartsSection.kt`
- `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverContentTest.kt`
- `specs/0053-izquierdojl-20261122-podcast-top-charts/plan.md` (solo si se decide registrar la desviación de la decisión de UI en `:122`; no es código)

**Tests to add or update**:
- Test instrumentado de regresión en `DiscoverContentTest.kt` que componga `DiscoverContent` con altura forzada (patrón de `SettingsContentTest.kt:23-60`, bug 0043) y compruebe `performScrollTo()` sobre el selector de país y sobre el final del ranking.
- Ajustar `DiscoverContentTest.kt` y `TopChartsSectionTest.kt` si al unificar el scroll cambia la jerarquía que usan sus `testTag`/matchers; verificar que `sinBusqueda_muestraPopulares` y `conBusqueda_muestraResultadosYOcultaPopulares` siguen pasando.
- No se requieren cambios en ViewModels: el estado y los callbacks se conservan.

## Risks & Considerations

- Cambía la decisión de diseño documentada en `specs/0053-.../plan.md:122` («no anida scrolls» con `weight(1f)`): hay que decidir si se enmienda la spec o se registra como desviación en el reporte de fix.
- `TopChartsSection` se usa de forma aislada en `TopChartsSectionTest.kt`; quitar el `weight(1f)` puede alterar la altura de sus estados de carga/vacío en esos tests.
- `weight(1f)` solo es válido en `ColumnScope`/`RowScope`; al migrar a `LazyColumn` hay que eliminarlo también de los estados de carga/vacío para evitar un error de compilación.
- Un único `LazyColumn` con 50 filas mantiene la recomposición eficiente; usar `Column.verticalScroll` renderizaría las 50 filas de una vez (peor rendimiento y accesibilidad con fuentes grandes).
- El teclado: el `Scaffold` del nav graph no aplica `imePadding`; si el teclado sigue tapando el último selector, puede necesitarse una corrección separada fuera del alcance de este bug.
- Sin impacto en red, datos, autenticación ni secciones sensibles; no se añaden dependencias.

## Open Questions

- [NEEDS CLARIFICATION: ¿ocurre también con el teclado cerrado, o solo al enfocar «Buscar podcasts»/«URL RSS manual»?]
- [NEEDS CLARIFICATION: ¿debe el título «Descubrir» y el bloque de búsqueda desaparecer completamente al desplazarse, o basta con que se desplace con el resto? El reporte pide «que se desplace toda la pantalla hacia arriba», lo que implica lo primero.]
- ¿Se enmienda `specs/0053-.../plan.md:122` o se deja constancia solo en `.specify/bugs/<slug>/fix.md`?
