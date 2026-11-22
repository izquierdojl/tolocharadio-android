# Bug Verification: No se pierde la posición del episodio al detenerlo externamente

- **Slug**: 0054-izquierdojl-20261122-podcast-resume-position-lost
- **Tested**: 2026-11-22
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

La persona usuaria comprobó manualmente en el emulador que, tras perder la
reproducción de un episodio de la cola con progreso parcial, la siguiente
selección reanuda correctamente y no empieza desde el principio. Los tests
automatizados cubren la causa raíz (parada externa sin reportar posición 0) y
la suite completa pasa sin regresiones.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Prueba manual de la persona usuaria en el emulador | pass | «Lo he comprobado yo y parece que funciona correctamente». |
| Equivalent automated reproduction | `./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModelTest"` | pass | Los 3 tests nuevos reproducen la parada externa y el keepalive; 19/19 en verde. |
| New / updated tests | Igual que la fila anterior | pass | `una parada externa cierra la tarjeta y no reporta posicion cero`, `parar tras una parada externa no reporta cero`, `el auto-avance conserva el programa aunque el controlador pase a idle`. |
| Regression suite | `./gradlew testDebugUnitTest` | pass | 132 suites, 720 tests, 0 fallos, 0 errores. |
| Lint / type-check | `./gradlew assembleDebug detekt ktlintCheck lintDebug` | pass | `BUILD SUCCESSFUL`; `lintDebug` 0 errores (97 warnings y 1 hint preexistentes, ninguno en los ficheros tocados). |

## Output Excerpts

```text
com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModelTest: tests=19 failures=0 errors=0 skipped=0
  - una parada externa cierra la tarjeta y no reporta posicion cero
  - parar tras una parada externa no reporta cero
  - el auto-avance conserva el programa aunque el controlador pase a idle
```

```text
suites=132 tests=720 failures=0 errors=0 skipped=0
BUILD SUCCESSFUL
```

## Residual Risks

- Al tomar la radio el reproductor se conserva la posición del último reporte
  periódico; puede haber hasta 30 s de desviación (no un reinicio desde 0).
- Un progreso ya corrompido a 0 antes del fix no se recupera; solo se evita que
  vuelva a ocurrir.
- El camino Cast sigue empezando en 0 (anotado como follow-up independiente).

## Recommendation

Cerrar el bug: verificado manualmente por la persona usuaria y cubierto por
tests que fallaban antes del cambio. Abrir un assessment aparte para la
reanudación al emitir a Cast si se quiere abordar ese caso.
