# Bug Verification: La barra de navegación inferior solo muestra "Explorar"

- **Slug**: 0040-jlizquierdo-20260925-bottom-nav-solo-explorar
- **Tested**: 2026-09-25
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

El síntoma original ya no se reproduce: el usuario ha validado visualmente en
dispositivo que la barra inferior vuelve a mostrar los 5 destinos y se ve
correcta. Los gates obligatorios y la compilación del test de UI están en verde.
El test instrumentado de regresión no se pudo ejecutar aquí (no hay
emulador/dispositivo conectado al SDK de este entorno); queda como red de
seguridad para `connectedDebugAndroidTest` en CI/local con dispositivo.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Verificación visual del usuario en dispositivo: abrir la app y observar la barra inferior | pass | El usuario confirma que ahora se ven correctamente los 5 botones (Explorar, Favoritos, Historial, Mis emisoras, Configuración) |
| New / updated tests | `.\gradlew.bat assembleDebugAndroidTest` (compila `TolochaNavigationBarTest`) | pass (compila) / test instrumentado not-run | Sin dispositivo conectado (`adb devices` vacío); no se ejecutó el test, solo se compiló |
| Regression suite (unit) | `.\gradlew.bat testDebugUnitTest` | pass | BUILD SUCCESSFUL |
| Lint / detekt / ktlint | `.\gradlew.bat detekt ktlintCheck lintDebug` | pass | BUILD SUCCESSFUL |
| Build | `.\gradlew.bat assembleDebug` | pass | BUILD SUCCESSFUL |

## Output Excerpts

- `assembleDebugAndroidTest testDebugUnitTest detekt ktlintCheck lintDebug`:
  `BUILD SUCCESSFUL in 2m 53s` — 89 actionable tasks.
- `assembleDebug`: `BUILD SUCCESSFUL in 1m 33s`.
- `adb devices`: `List of devices attached` (sin dispositivos) → test instrumentado
  no ejecutado en este entorno.

## Residual Risks

- El test de UI `TolochaNavigationBarTest` no se ha ejecutado todavía; conviene
  lanzarlo con dispositivo/emulador (`connectedDebugAndroidTest`) para dejarlo en
  verde y evitar regresiones silenciosas.
- El tooltip al mantener pulsado (spec 012 AC3) se conserva; no se ha verificado
  de nuevo su posición al anclarse ahora al slot completo, aunque visualmente el
  usuario no reporta problema.

## Recommendation

Cerrar el bug como **verificado**: la reproducción original (solo Explorar
visible) ya no ocurre en dispositivo real y los gates están en verde. Como
seguimiento, ejecutar el test de UI en un entorno con dispositivo para fijarlo en
verde. No se requieren cambios de dependencias ni de gates.
