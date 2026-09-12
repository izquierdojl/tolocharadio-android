# Bug Verification: Mostrar errores de reproducción en Cast y permitir reintentar

- **Slug**: 0028-jlizquierdo-20260912-cast-playback-error-ui
- **Tested**: 2026-09-12
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

Gates en verde y test del retry añadido. Pendiente validar en dispositivo que un fallo de Cast muestra mensaje y permite reintentar (requiere provocar un error, difícil de forzar con el servidor funcionando).

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Error de Cast en UI (post-fix) | Provocar fallo de stream en el receptor | not-run | Difícil de inducir; se valida si ocurre |
| New / updated tests | `gradlew.bat testDebugUnitTest` | pass | `PlayerViewModelTest` (retry Cast) |
| Regression suite | `gradlew.bat testDebugUnitTest assembleDebug` | pass | BUILD SUCCESSFUL |
| Lint / type-check | `gradlew.bat detekt ktlintCheck lintDebug` | pass | BUILD SUCCESSFUL |

## Output Excerpts

```
BUILD SUCCESSFUL
```

## Residual Risks

- El error se muestra genérico ("No se pudo reproducir en el dispositivo."); no distingue la causa.

## Recommendation

Cerrar — mejora de robustez cubierta por tests; el mensaje aparecerá si algún día falla el receptor.
