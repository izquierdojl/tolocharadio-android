# Bug Verification: Los episodios completados salen de la cola

- **Slug**: 0050-izquierdojl-20261120-podcast-queue-completed-returns
- **Tested**: 2026-11-20
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

El fix queda revisado y aceptado: los tests cubren la retirada de la cola al
terminar y al parar cerca del final, la suite completa pasa sin regresiones y el
APK con el cambio instala y arranca. No se ejecutó la reproducción destructiva
en dispositivo para no completar un episodio real de la cola; el usuario aceptó
cerrar la revisión con este alcance y abrir otro bug si el síntoma reaparece.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Completar/parar al 98 % un episodio real de la cola en el emulador | skipped | Habría modificado la cola y el progreso reales de la cuenta. El usuario aceptó cerrar la revisión sin esta prueba. |
| Equivalent automated reproduction | `./gradlew cleanTestDebugUnitTest testDebugUnitTest` | pass | 13/13 tests de `EpisodePlayerViewModelTest`, incluidos finalización, parada al 98 %, parada a medias y fallo de red. |
| New / updated tests | `./gradlew cleanTestDebugUnitTest testDebugUnitTest` | pass | 629 tests, 0 fallos. |
| Regression suite | `./gradlew cleanTestDebugUnitTest testDebugUnitTest` | pass | Suite unitaria completa en verde. |
| Lint / type-check | `./gradlew detekt ktlintCheck compileDebugAndroidTestKotlin lintDebug` | pass | `BUILD SUCCESSFUL`; `lintDebug`: 0 errores, 93 warnings y 1 hint (pre-existentes). |
| APK build/install | `./gradlew assembleDebug` + `adb install -r app/build/outputs/apk/debug/app-debug.apk` | pass | Instalación correcta conservando servidor y sesión. |
| App launch smoke | `adb shell am start -n com.izquierdojl.tolocharadio/.MainActivity` + Logcat | pass | La app arranca y renderiza contenido; sin `FATAL EXCEPTION`. |
| Connected UI tests | `connectedDebugAndroidTest` | skipped | No se ejecutó la suite instrumentada; no aporta cobertura del flujo de red del reproductor. |

## Output Excerpts

```text
suites=121 tests=629 failures=0 errors=0 skipped=0
```

```text
EpisodePlayerViewModelTest tests=13 failures=0 errors=0
  - el auto-avance reanuda el siguiente episodio
  - al terminar sin siguiente retira el episodio y limpia la tarjeta
  - un fallo al retirar de la cola no impide el auto-avance
  - parar cerca del final retira el episodio si el servidor lo completa
  - parar a mitad no retira el episodio de la cola
  - un reporte periodico de un completado no lo retira de la cola
```

```text
> Task :app:lintDebug
BUILD SUCCESSFUL
0 errors, 93 warnings, 1 hint
```

```text
Performing Streamed Install
Success
```

```text
FATAL EXCEPTION: 0
```

## Residual Risks

- No se completó un episodio real en el dispositivo; la verificación de la
  pantalla de cola tras la finalización queda cubierta solo por los tests del
  ViewModel y por la paridad con la implementación web.
- La parada al 98 % depende del auto-completado del servidor (≥95 % de posición
  y ≥80 % de escucha acumulada). El test simula la respuesta `completed = true`;
  no se validó contra el servidor real para no alterar la cuenta.
- El keepalive de segundo plano sigue sin procesar el `completed` de la
  respuesta; si el servidor completa en segundo plano, la retirada de la cola
  puede retrasarse hasta el siguiente refresco.
- `nextQueueEpisodeId()` mantiene el comportamiento preexistente de elegir el
  primer elemento distinto del terminado, no estrictamente el siguiente en
  orden.

## Recommendation

Cerrar el bug: revisión aceptada por el usuario, con tests y compilación en
verde y sin regresiones conocidas. Si el episodio vuelve a aparecer en la cola,
abrir un bug nuevo con el episodio, el porcentaje de escucha y el estado de la
cola en ese momento.
