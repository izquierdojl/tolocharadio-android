# Bug Verification: Configuración sin scroll y opciones de buffer plegables

- **Slug**: 0043-jlizquierdo-20261031-configuracion-scroll-buffer-plegable
- **Tested**: 2026-10-31
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

El equivalente automatizado de la reproducción pasa en un emulador API 37: el
contenido de Configuración se desplaza hasta "Gráficas" y "Acerca de", y las
opciones de buffer aparecen plegadas por defecto y se despliegan al pulsar la
cabecera. No se pudo completar la reproducción manual end-to-end (abrir la app
con un servidor configurado y navegar a Configuración) porque el entorno no
dispone de credenciales de servidor y la app queda bloqueada en "Añadir
servidor". No se detectaron regresiones en unit tests ni en los gates.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | `SettingsContentTest` en emulador API 37 (`connectedDebugAndroidTest`) | pass | Equivalente automatizado: `SettingsContent` con 320 dp, `performScrollTo` hasta "Gráficas" y "Acerca de", clics verificados |
| Reproduction (manual) | Abrir app real → Configuración → scroll → Gráficas/Acerca de | not-run | La app instalada queda en "Añadir servidor"; sin credenciales no se alcanza Configuración |
| New / updated tests (buffer) | `connectedDebugAndroidTest -P...class=...PlaybackSettingsTest` | pass | 5/5 tests, 0 fallos |
| New / updated tests (scroll) | `connectedDebugAndroidTest -P...class=...SettingsContentTest` | pass | 1/1 test, 0 fallos |
| Regression suite (unit) | `./gradlew testDebugUnitTest` | pass | 414 tests, 0 failures, 0 errors |
| Lint / detekt / ktlint | `./gradlew detekt ktlintCheck lintDebug` | pass | BUILD SUCCESSFUL |
| Build | `./gradlew assembleDebug assembleDebugAndroidTest` | pass | BUILD SUCCESSFUL |

## Output Excerpts

```text
com.izquierdojl.tolocharadio.feature.settings.PlaybackSettingsTest
tests="5" failures="0" errors="0" skipped="0" time="11.546"

com.izquierdojl.tolocharadio.feature.settings.SettingsContentTest
tests="1" failures="0" errors="0" skipped="0" time="3.519"
  testcase: conPocoAltoSePuedeLlegarAGraficasYAcercaDe
```

```text
> Task :app:connectedDebugAndroidTest
Running tests on devices: Medium_Phone_API_37.0(AVD) - 17
BUILD SUCCESSFUL in 10s
```

```text
> Task :app:testDebugUnitTest
> Task :app:detekt
> Task :app:ktlintCheck
> Task :app:lintDebug
> Task :app:assembleDebug
> Task :app:assembleDebugAndroidTest
BUILD SUCCESSFUL in 1s
```

```text
# app real instalada en el emulador
text="Añadir servidor"
text="Configura un servidor para acceder a tu contenido."
```

## Residual Risks

- La pantalla real `SettingsScreen` dentro del `Scaffold`/`NavigationBar` no se
  navegó manualmente; el test de scroll cubre `SettingsContent`, que es el
  contenedor con `verticalScroll`, pero no la integración visual completa.
- No se comprobó en dispositivo real que el último elemento no quede tapado por
  la barra inferior o el mini-player.
- No se verificó la rotación ni la persistencia del estado plegado con
  `rememberSaveable` en la app real.
- Solo se probó en emulador API 37; no en API 26 ni en pantallas de altura
  reducida reales.
- La navegación efectiva a Gráficas/Acerca de se validó por callbacks, no por
  navegación end-to-end.

## Recommendation

Mantener el bug como **parcial** hasta hacer una verificación manual en un
entorno con un servidor configurado: abrir Configuración, confirmar que el
buffer aparece plegado, desplegarlo, y hacer scroll hasta "Gráficas" y "Acerca
de" comprobando que ambas filas responden y no quedan tapadas por la barra
inferior. La evidencia automatizada es sólida y no hay regresiones; si el
equipo acepta la cobertura instrumentada como suficiente, se puede cerrar el
bug sin cambios adicionales.
