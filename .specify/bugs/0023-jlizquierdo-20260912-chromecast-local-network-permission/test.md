# Bug Verification: Descubrimiento Cast bloqueado por falta de ACCESS_LOCAL_NETWORK (Android 17)

- **Slug**: 0023-jlizquierdo-20260912-chromecast-local-network-permission
- **Tested**: 2026-09-12
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

Los checks automatizados (tests unitarios nuevos + suite completa + detekt/ktlint/lint + `assembleDebug`) pasan en verde y la reproducción en **dispositivo físico Android 17** confirma que el selector de Cast ya lista el receptor de la red y la reproducción funciona. Bug resuelto.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Abrir app en dispositivo físico Android 17 + receptor Cast en la misma Wi-Fi y pulsar el botón Cast | pass | Confirmado por el usuario (v1.6.0): el selector lista el receptor y la reproducción funciona. |
| New / updated tests | `gradlew.bat testDebugUnitTest` | pass | Incluye `CastPermissionsTest` (API 26-30, 31-32, 33-36, 37). |
| Regression suite | `gradlew.bat testDebugUnitTest assembleDebug` | pass | BUILD SUCCESSFUL; APK debug generado. |
| Lint / type-check | `gradlew.bat detekt ktlintCheck lintDebug` | pass | BUILD SUCCESSFUL. |

## Output Excerpts

```
> Task :app:testDebugUnitTest
> Task :app:assembleDebug
BUILD SUCCESSFUL in 25s
```

```
> Task :app:detekt
> Task :app:ktlintCheck
> Task :app:lintDebug
BUILD SUCCESSFUL in 4s
```

```
Verificación manual (usuario, dispositivo físico Android 17, APK v1.6.0):
el selector de Cast lista el receptor de la red y la reproducción funciona.
```

## Residual Risks

- **Permiso a nivel de dispositivo**: Google Play Services debe tener "Dispositivos cercanos" concedido; no es controlable desde la app. Primer punto a revisar si otro usuario no ve dispositivos.
- **Emulador**: no reproduce el escenario; no debe usarse como evidencia.
- **Servidor Tolocha en LAN**: si el usuario aloja el servidor en su red local, el mismo permiso es necesario para la conectividad base.

## Recommendation

Cerrar el bug — verificado en dispositivo físico Android 17: el selector de Cast descubre el receptor de la red y la reproducción funciona.
