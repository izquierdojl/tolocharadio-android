# Bug Verification: El tiempo de escucha usa avance real y no la posición

- **Slug**: 0051-izquierdojl-20261120-podcast-listening-time
- **Tested**: 2026-11-20
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

El mecanismo corregido queda validado con pruebas automatizadas: tras escuchar
unos segundos y saltar al minuto 10, el reporte envía sólo el tiempo escuchado
(`listenedMs = 3 s`), no la posición (`600 s`); la parada, el fin de episodio y
el keepalive también usan el acumulado, y la suite completa sigue en verde. No
se ejecutó la reproducción manual en dispositivo contra el servidor real porque
habría contaminado las estadísticas de la cuenta; por eso el resultado es
`partial`, no `verified`.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Escuchar 5 min, saltar al minuto 90, parar y mirar Gráficas en dispositivo/servidor real | skipped | No ejecutado para no alterar las estadísticas reales; el assessment ya lo había evitado. |
| Equivalent automated reproduction | `./gradlew testDebugUnitTest --tests "...EpisodePlayerViewModelTest"` | pass | `un seek no infla el tiempo escuchado`: 3 s escuchados + salto a 600 s → reporta `positionSec=600`, `listenedMs=3000`. |
| New / updated tests | `./gradlew testDebugUnitTest --tests "...ListeningTimeTrackerTest" --tests "...EpisodePlayerViewModelTest" --tests "...ProgressRepoTest"` | pass | 6 + 16 + 5 tests, 0 fallos. |
| Regression suite | `./gradlew testDebugUnitTest` | pass | 122 suites, 641 tests, 0 fallos, 0 errores. |
| Build | `./gradlew assembleDebug` | pass | `BUILD SUCCESSFUL`. |
| Lint / static analysis | `./gradlew detekt ktlintCheck lintDebug` | pass | `BUILD SUCCESSFUL`; `lintDebug`: 0 errores, 93 warnings y 1 hint preexistentes. |
| Connected / instrumented tests | `connectedDebugAndroidTest` | skipped | No existe un test instrumentado de este flujo y requeriría datos reales; no aporta al cálculo, que es lógica pura. |

## Output Excerpts

```text
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 6s
```

```text
suites=122 tests=641 failures=0 errors=0 skipped=0
ListeningTimeTrackerTest: tests=6 failures=0 errors=0
EpisodePlayerViewModelTest: tests=16 failures=0 errors=0
ProgressRepoTest: tests=5 failures=0 errors=0
```

Comportamiento fijado por el test clave:

```kotlin
coVerify {
    progress.report(
        episodeId = 1L,
        sessionId = any(),
        sequence = any(),
        positionSec = 600L,      // posición tras el salto
        durationSec = 1_800L,
        listenedMs = 3_000L,     // sólo 3 s realmente escuchados
        completed = null,
    )
}
```

```text
> Task :app:lintDebug
BUILD SUCCESSFUL
0 errors, 93 warnings, 1 hint
```

## Residual Risks

- La reproducción manual en dispositivo contra un servidor real no se ejecutó;
  la equivalencia se apoya en el test del ViewModel y en el contrato del
  backend, no en una observación de las gráficas.
- Las estadísticas ya registradas con el cálculo antiguo siguen infladas; el
  fix sólo afecta a los reportes nuevos.
- El acumulador usa tiempo de contenido (paridad con la web): a 2×, 30 minutos
  reales cuentan como 60 minutos de contenido. Si el producto espera tiempo de
  reloj, habría que cambiar la semántica.
- Si el proceso se congela más de 10 s entre muestras, ese tramo no se suma
  (ventana anti-salto). Es un compromiso deliberado para no contar seeks.
- Marcar manualmente un episodio como reproducido sigue enviando la duración
  completa como tiempo escuchado; queda como decisión de producto en
  follow-ups del fix.

## Recommendation

Mantener el fix y cerrar el bug a nivel de código: el defecto original (enviar
la posición como tiempo escuchado) está cubierto por tests que fallarían con la
implementación anterior, y no hay regresiones en la suite ni en los checks
estáticos. Para dar el visto bueno end-to-end, falta una comprobación manual
segura: reproducir un episodio largo, escuchar unos minutos, saltar, parar y
confirmar que el incremento de las gráficas coincide con lo escuchado. No se
recomienda reabrir ni repetir `/speckit.bug.assess` con la evidencia actual.

## Cierre

El usuario asume la verificación manual en entorno real y acepta el cierre del
bug con el resultado `partial` de este informe (2026-11-20).

