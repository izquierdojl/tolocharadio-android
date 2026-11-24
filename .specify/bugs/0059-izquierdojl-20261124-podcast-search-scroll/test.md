# Bug Verification: cabecera de búsqueda fija en Descubrir de podcasts

- **Slug**: 0059-izquierdojl-20261124-podcast-search-scroll
- **Tested**: 2026-11-24
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

La reproducción automatizada del bug (altura forzada de 320 dp, desplazamiento desde el campo de búsqueda hasta el selector de país y la última fila del ranking) pasa tras corregir el test añadido por el fix. No aparecieron regresiones: 765 tests unitarios y 141 instrumentados pasan, junto con detekt, ktlint y lint. No se reprodujo manualmente el escenario con servidor real y teclado abierto; queda como riesgo residual.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Test instrumentado `conPocoAlto_seDesplazaTodaLaPantallaHastaElFinalDelRanking` en AVD `Medium_Phone_API_37.0` | pass | Con 320 dp de alto: «Buscar podcasts» visible al inicio, `performScrollToNode` alcanza el selector de país y «Podcast 8», y el campo de búsqueda desaparece al final del scroll. |
| Reproduction manual (app + servidor + teclado) | Sin servidor con `podcastDiscovery == itunes` ni dispositivo físico | not-run | Equivalente automatizado ejecutado; el flujo completo con teclado/fuentes grandes no se cubrió. |
| New / updated tests | `./gradlew connectedDebugAndroidTest` | pass | 141/141 tests instrumentados, 0 fallos (incluye el test de regresión corregido). |
| Regression suite (unit) | `./gradlew testDebugUnitTest` | pass | 765 tests en 143 suites, 0 fallos. |
| Lint / type-check | `./gradlew detekt ktlintCheck lintDebug` | pass | BUILD SUCCESSFUL, sin issues nuevos. |
| Estructura del layout | Revisión de `feature/podcasts/discover/*.kt` | pass | `DiscoverContent` tiene un único `LazyColumn`; no quedan `weight(1f)` en áreas de contenido (solo en filas de selectores y textos). |

## Output Excerpts

```text
./gradlew connectedDebugAndroidTest
Running tests on devices: Medium_Phone_API_37.0(AVD) - 17
BUILD SUCCESSFUL in 4m 42s
```

```text
tests="141" failures="0" errors="0" skipped="0"
DiscoverContentTest conPocoAlto_seDesplazaTodaLaPantallaHastaElFinalDelRanking PASS
SettingsContentTest conPocoAltoSePuedeLlegarAGraficasYAcercaDe PASS
```

```text
./gradlew testDebugUnitTest detekt ktlintCheck lintDebug → BUILD SUCCESSFUL
(765 tests unitarios, 0 fallos; detekt, ktlint y lint sin issues)
```

## Residual Risks

- No se validó el flujo real de la app con un servidor que publique `podcastDiscovery == itunes`, ni el efecto de teclado abierto / fuentes grandes (preguntas pendientes en el assessment).
- El emulador usado es API 37 con pantalla mediana; no se probaron pantallas pequeñas físicas.
- `specs/0053-.../plan.md:122` sigue describiendo el diseño anterior («no anida scrolls» con `weight(1f)`); la desviación está documentada en `fix.md`.

## Recommendation

Cerrar el bug: la reproducción automatizada ejercita el síntoma original y pasa, y ninguna suite presenta regresiones. Queda como trabajo de seguimiento no bloqueante la comprobación manual con teclado/fuentes grandes y, si se aprueba el cambio de UX, actualizar la spec 0053. La primera pasada de verificación falló por un problema del test (`performScrollTo` sobre un nodo no compuesto), corregido en `fix.md` antes de esta ejecución.
