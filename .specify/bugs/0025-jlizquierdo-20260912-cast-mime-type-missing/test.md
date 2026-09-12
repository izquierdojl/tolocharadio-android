# Bug Verification: mimeType obligatorio en el MediaItem para Cast

- **Slug**: 0025-jlizquierdo-20260912-cast-mime-type-missing
- **Tested**: 2026-09-12
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

Gates automatizados en verde y el crash deja de ser posible en la ruta de `CastPlayer.setMediaItem` (el `MediaItem` ya siempre lleva `mimeType`). Pendiente de confirmar reproducción real en el receptor Cast tras publicar el APK.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproducción (post-fix) | Instalar v1.7.2 y castear una emisora no HLS | not-run | Requiere receptor Cast; se hará tras publicar |
| New / updated tests | `gradlew.bat testDebugUnitTest` | pass | `StationMediaItemFactoryTest` |
| Regression suite | `gradlew.bat testDebugUnitTest assembleDebug` | pass | BUILD SUCCESSFUL |
| Lint / type-check | `gradlew.bat detekt ktlintCheck lintDebug` | pass | BUILD SUCCESSFUL |

## Output Excerpts

```
BUILD SUCCESSFUL
```

Stacktrace original (antes del fix):
```
java.lang.IllegalArgumentException: The item must specify its mimeType
	at androidx.media3.cast.DefaultMediaItemConverter.toMediaQueueItem
	at ...CastPlayerManager.connectToStation(CastPlayerManager.kt:285)
```

## Residual Risks

- El receptor podría no reproducir si el proxy exige `Authorization` (el player local lo añade, el receptor no puede).
- El `mimeType` es una pista; si no coincide con el stream real, el receptor podría rechazarlo.

## Recommendation

Publicar v1.7.2 y validar en dispositivo: si la emisora suena en el receptor, cerrar como verificado. Si el receptor no carga, investigar la auth del proxy para Cast (ver assessment, Open Questions).
