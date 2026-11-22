# Bug Verification: Reanudación en Cast desde la posición guardada

- **Slug**: 0056-izquierdojl-20261122-podcast-cast-resume-position
- **Tested**: 2026-11-22
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

El fix queda verificado a nivel de código: la rama Cast propaga `startMs` y el
reproductor remoto recibe la posición con `setMediaItem(item, startMs)`. La
reproducción con un receptor Cast físico **no se pudo ejecutar** porque el
usuario no dispone de ninguno; el usuario aceptó explícitamente el riesgo y
pidió cerrar el bug como verificado, comprometiéndose a abrir otro bug si
aparece un problema real en el receptor.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Emitir un episodio a medias a un Chromecast real | not-run | Sin receptor Cast disponible. Riesgo aceptado por el usuario. |
| Equivalent automated reproduction | `./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackControllerTest"` | pass | El test no compilaba antes del cambio (`Too many arguments for connectToEpisode`); ahora 1/1 en verde. |
| Regression suite | `./gradlew testDebugUnitTest` | pass | 133 suites, 721 tests, 0 fallos, 0 errores. |
| Lint / type-check | `./gradlew assembleDebug detekt ktlintCheck lintDebug compileDebugAndroidTestKotlin` | pass | `BUILD SUCCESSFUL`; 0 errores de lint. |

## Output Excerpts

```text
com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackControllerTest
  - play en Cast propaga la posicion de reanudacion
BUILD SUCCESSFUL
```

```text
suites=133 tests=721 failures=0 errors=0 skipped=0
BUILD SUCCESSFUL
```

## Residual Risks

- No se ha comprobado el comportamiento real del receptor: algunos modelos
  pueden ignorar la posición inicial si el medio aún no es seekable.
- Si el receptor no respeta `setMediaItem(item, startMs)`, habrá que evaluar
  `prepare()` + `seekTo(startMs)` tras la confirmación de la sesión.
- Conectar Cast **durante** la reproducción local no transfiere el episodio en
  curso; es un comportamiento independiente, no cubierto por este bug.

## Recommendation

Cerrar el bug como verificado por decisión explícita del usuario, aun sin
prueba con hardware Cast. Si aparece un problema en un receptor real, abrir un
nuevo bug con el modelo de dispositivo y el comportamiento observado.
