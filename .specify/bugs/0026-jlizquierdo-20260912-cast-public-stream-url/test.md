# Bug Verification: Cast reproduce con la URL pública de la emisora

- **Slug**: 0026-jlizquierdo-20260912-cast-public-stream-url
- **Tested**: 2026-09-12
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

Gates en verde. Pendiente confirmar en dispositivo que el receptor reproduce con la URL pública.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproducción (post-fix) | Castear una emisora con v1.7.3 | not-run | Se validará tras publicar |
| New / updated tests | `gradlew.bat testDebugUnitTest` | pass | `StationMediaItemFactoryTest` |
| Regression suite | `gradlew.bat testDebugUnitTest assembleDebug` | pass | BUILD SUCCESSFUL |
| Lint / type-check | `gradlew.bat detekt ktlintCheck lintDebug` | pass | BUILD SUCCESSFUL |

## Output Excerpts

```
BUILD SUCCESSFUL
```

Diagnóstico (sin `/Authorization`, el proxy devuelve 401):
```
https://radio.jlizquierdo.com/api/v1/playback/test -> 401
{"error":{"code":"UNAUTHORIZED","message":"No autorizado","status":401}}
```

## Residual Risks

- Emisoras HTTP en claro podrían ser bloqueadas por contenido mixto en el receptor.
- Emisoras HLS pueden requerir CORS en manifiesto/segmentos.
- Streams que requieran UA/Referer podrían no reproducirse directos.

## Recommendation

Publicar v1.7.3 y validar en dispositivo. Si una emisora concreta no suena, revisar si es http/HLS o requiere UA, y valorar el fallback por proxy con URL firmada en el servidor.
