# Bug Verification: Descubrimiento Cast bloqueado por falta de ACCESS_LOCAL_NETWORK (Android 17)

- **Slug**: 0023-jlizquierdo-20260912-chromecast-local-network-permission
- **Tested**: 2026-09-12
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

Los checks automatizados (tests unitarios nuevos + suite completa + detekt/ktlint/lint + `assembleDebug`) pasan en verde. Sin embargo, la reproducción real del bug —que el selector de Cast liste dispositivos en la red— **no se ha podido ejercitar**: requiere un dispositivo físico con un receptor Google Cast en la misma Wi-Fi, y no hay ningún dispositivo conectado (`adb devices` vacío). El emulador no sirve para validar discovery real por NAT/mDNS. Por tanto la corrección queda **pendiente de verificación en dispositivo físico**.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Abrir app en dispositivo físico Android 17 + receptor Cast en la misma Wi-Fi y pulsar el botón Cast | not-run | Sin dispositivo conectado; el emulador no descubre dispositivos reales (NAT/mDNS). Requiere prueba manual del usuario. |
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
adb devices
List of devices attached
(ninguno)
```

## Residual Risks

- **Descubrimiento real no verificado**: el fix se apoya en la exigencia oficial de Android 17 (`ACCESS_LOCAL_NETWORK`) y en la lógica de permisos por API level, pero no se ha observado el picker con dispositivos reales.
- **Permiso a nivel de dispositivo**: Google Play Services debe tener "Dispositivos cercanos" concedido; no es controlable desde la app.
- **Emulador**: no reproduce el escenario; no debe usarse como evidencia.
- **Servidor Tolocha en LAN**: si el usuario aloja el servidor en su red local, el mismo permiso es necesario para la conectividad base.

## Recommendation

Mantener en `partial` hasta la prueba en dispositivo físico. El usuario va a testear el APK en hardware real: si el selector lista el receptor Cast tras conceder "Dispositivos cercanos", cerrar el bug como verificado. Si sigue vacío, reabrir con logs (`CastPlayerManager`) y re-ejecutar `/speckit.bug.assess`.
