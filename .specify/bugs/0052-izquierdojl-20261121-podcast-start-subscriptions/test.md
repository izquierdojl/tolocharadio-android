# Bug Verification: Sección inicial de Podcasts y selector de arranque

- **Slug**: 0052-izquierdojl-20261121-podcast-start-subscriptions
- **Tested**: 2026-11-21
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

La reproducción manual en el emulador confirma que el fallo ya no se produce:
con la sección inicial en Podcasts y la pantalla de arranque en Suscripciones, la
app abre Suscripciones tanto al reiniciar como al cambiar de Radio a Podcasts; el
selector se muestra como botones segmentados. Las pruebas automatizadas también
pasan: 668 tests unitarios, el test instrumentado del selector y los gates de
compilación, detekt, ktlint y lint.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Manual en `Medium_Phone_API_37.0` | pass | Con servidor configurado: abre Suscripciones al reiniciar y al pulsar Podcasts; también se comprobó Cola y el arranque en Radio. |
| New / updated tests | `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.izquierdojl.tolocharadio.feature.settings.SettingsPodcastStartTest` | pass | 1 test, 0 fallos; selector segmentado y callback de Suscripciones. |
| Unit test suite | `./gradlew testDebugUnitTest` | pass | 668 tests, 0 fallos, 0 errores. |
| Lint / type-check | `./gradlew detekt ktlintCheck lintDebug` | pass | Gates en verde. |
| Build | `./gradlew assembleDebug` | pass | APK debug instalado en el emulador. |
| Emulator boot | `emulator -avd Medium_Phone_API_37.0 -no-snapshot-load -no-snapshot-save -no-audio` | pass | `emulator-5554`, API 37, `sys.boot_completed=1`. |
| App launch | `adb shell am start -n com.izquierdojl.tolocharadio/.MainActivity` | pass | Abre sin crash; muestra "Añadir servidor" porque no hay servidor configurado. |

## Output Excerpts

```text
adb devices
List of devices attached
emulator-5554    device
```

```xml
<testsuites tests="1" failures="0" errors="0" skipped="0" time="0.000">
  <testsuite name="com.izquierdojl.tolocharadio.feature.settings.SettingsPodcastStartTest"
             tests="1" failures="0" errors="0" skipped="0" time="2.656">
    <testcase name="elSelectorDePodcastMuestraLasCincoOpcionesYSeleccionaSuscripciones" ... />
  </testsuite>
</testsuites>
```

```text
tests=668 failures=0 errors=0 skipped=0
BUILD SUCCESSFUL
topResumedActivity=... com.izquierdojl.tolocharadio/.MainActivity
```

Captura del estado actual: `/tmp/opencode/tolocha-0052-inicio.png` (pantalla
"Añadir servidor").

## Manual Verification

Confirmado por la persona usuaria en el emulador `Medium_Phone_API_37.0`:

1. Configuración → **Sección inicial** → Podcasts.
2. **Pantalla de arranque en podcast** → **Suscripciones** (selector segmentado
   en dos filas, no desplegable).
3. Cerrar por completo la app y abrirla de nuevo → abre **Suscripciones**.
4. Estando en Radio, pulsar **Podcasts** en la cabecera → abre **Suscripciones**,
   no Inicio.
5. Cambiar la pantalla a **Cola** → el siguiente arranque y el cambio de modo
   abren **Cola**.
6. Cambiar la sección inicial a Radio y reiniciar → abre la pantalla de arranque
   de Radio, sin errores.

## Residual Risks

- La revisión visual con fuente al 200 % y TalkBack del selector de dos filas no
  se ha documentado explícitamente.
- No hay test instrumentado de navegación de arranque completo; la puerta
  `shouldApplyStartDestination` está cubierta por unidad.
- El comportamiento depende de que `/config` informe `podcasts=true`; con un
  servidor sin podcasts el arranque cae a Radio, que es el comportamiento
  esperado.

## Recommendation

**Close the bug** — verificado end-to-end: el síntoma original no reproduce y las
pruebas automatizadas siguen en verde.
