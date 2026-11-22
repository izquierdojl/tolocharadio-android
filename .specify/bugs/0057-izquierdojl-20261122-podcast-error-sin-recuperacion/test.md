# Bug Verification: Un corte de red en un episodio deja la reproducción en error terminal

- **Slug**: 0057-izquierdojl-20261122-podcast-error-sin-recuperacion
- **Tested**: 2026-11-22
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

La suite automatizada confirma que el error de red de un episodio deja de ser
terminal, que se reanuda en la posición guardada y que la recuperación de radio
no actúa mientras hay un episodio activo. La reproducción manual en un túnel
real no se ha podido ejercitar en este entorno, así que no se declara
`verified`.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Modo avión 3 min con episodio en streaming | not-run | Requiere dispositivo físico y salir de cobertura. |
| New / updated tests | `./gradlew testDebugUnitTest --tests "...EpisodePlaybackControllerTest" --tests "...PlaybackRecoveryManagerTest" --tests "...EpisodePlayerViewModelTest"` | pass | 5 + 10 + 20 tests, 0 fallos. |
| Regression suite | `./gradlew testDebugUnitTest` | pass | 730 tests, 0 fallos, 0 errores (133 clases). |
| Lint / type-check | `./gradlew detekt ktlintCheck` | pass | BUILD SUCCESSFUL. |
| Lint Android | `./gradlew lintDebug` | pass | BUILD SUCCESSFUL; informes en `app/build/reports/`. |

## Output Excerpts

```text
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 1m 38s
```

```text
> Task :app:detekt
> Task :app:ktlintCheck
BUILD SUCCESSFUL in 8s
```

```text
clases=133 tests=730 fallos=0 errores=0
```

## Residual Risks

- **Sin prueba en dispositivo**: el escenario original (caminar y perder
  cobertura) no se ha reproducido. Los tests cubren la lógica, no el
  comportamiento de radiofrecuencia real ni el ciclo de vida del servicio.
- **URL firmada caducada**: si la espera offline supera la vigencia de la URL,
  los reintentos usan la URL antigua y acabarán en `Error`. Queda como
  follow-up reemitirla antes de reanudar.
- **Cast no cubierto**: la recuperación se desactiva con sesión Cast; el
  receptor gestiona su propio buffer y errores.
- **Radio**: el aislamiento se apoya en `isEpisodeActive()`, que sigue siendo
  `true` en `Error`. Si el controlador no se reinicia (por ejemplo, tras matar
  el proceso), el estado se reconstruye al volver a reproducir.
- **Consumo en espera**: la espera offline es indefinida y no consume intentos;
  no se ha medido el impacto real en batería con la app en segundo plano.

## Recommendation

Cerrar el bug **tras una validación corta en dispositivo**: reproducir un
episodio en streaming, activar modo avión durante 3 minutos, desactivarlo y
comprobar que el audio vuelve en la misma posición y que no arranca la radio.
Si esa prueba falla, reabrir con el log `EpisodePlayback` (código y causa) y
volver a `/speckit.bug.assess`.
