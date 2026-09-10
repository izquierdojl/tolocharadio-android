# Quickstart: validación de accesos directos del icono

**Feature**: `0018-jlizquierdo-20260910-app-shortcut-recent-stations` | **Date**: 2026-09-10

Guía para validar la feature de extremo a extremo. Los detalles de forma de los datos están en [data-model.md](./data-model.md) y [contracts/](./contracts/); aquí solo hay pasos ejecutables.

## Requisitos previos

- Dispositivo o emulador Android API 26+ con un lanzador que muestre accesos directos al mantener pulsado el icono (Pixel Launcher, Nova, Lawnchair, etc.).
- Build de debug apuntando a una instancia de TolochaRadio con historial: `local.properties` → `tolocha.baseUrl=https://<instancia>`.
- Cuenta con al menos 4-5 emisoras distintas en el historial (reproducir algunas para generarlo).
- `adb` disponible y dispositivo autorizado (`adb devices`).

## Compilar, instalar y probar

```powershell
# Compilar e instalar
./gradlew installDebug

# Tests unitarios de la feature (rápidos, sin dispositivo)
./gradlew testDebugUnitTest --tests "*shortcuts*" --tests "*PlayerViewModelTest*"

# Suite completa + gates de calidad (equivalente a CI)
./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug
```

## Inspeccionar los accesos publicados (sin lanzador)

```powershell
# Lista publicada por la app (API 24+)
adb shell cmd shortcut get-shortcuts --user 0 com.izquierdojl.tolocharadio

# Alternativa: volcado completo del servicio de shortcuts
adb shell dumpsys shortcut | Select-String "tolocharadio|hist-"
```

Resultado esperado: hasta 5 entradas `hist-<stationId>`, con rank creciente según recencia y sin duplicados.

## Escenario A — Ver las emisoras recientes (US1, FR-001/002/003, SC-001)

1. Reproduce 5 emisoras distintas desde Explorar.
2. Ve a la pantalla de inicio y **mantén pulsado** el icono de TolochaRadio.
3. Verifica: aparecen hasta 4-5 emisoras, la última reproducida primero, sin repetidas ni emisoras ajenas.
4. Reproduce dos veces una misma emisora entre medias y confirma que no se duplica.

## Escenario B — Reproducción directa (US2, FR-005/006, SC-002)

1. **App cerrada**: desliza la app fuera de recientes (`adb shell am force-stop com.izquierdojl.tolocharadio`), mantén pulsado el icono, pulsa una emisora.
   - Esperado: la app arranca, suena la emisora y se abre el reproductor a pantalla completa en ≤ 5 s.
2. **Segundo plano**: reproduce una emisora, ve a inicio, pulsa otra en el menú del icono.
   - Esperado: la app pasa a primer plano con el full player y cambia de emisora sin cierres.
3. **Primer plano**: con la app abierta en Configuración, pulsa un acceso del icono.
   - Esperado: reproduce y muestra el full player.
4. Pulsa el acceso de la emisora **que ya está sonando**.
   - Esperado: no se reinicia ni se duplica (FR-013).

Sin lanzador a mano puedes simular el intent:

```powershell
adb shell am force-stop com.izquierdojl.tolocharadio
adb shell am start -n com.izquierdojl.tolocharadio/.MainActivity -a com.izquierdojl.tolocharadio.OPEN_STATION --es station_id "<STATION_ID>" --es station_name "Prueba"
```

## Escenario C — Sincronización con el historial (US3, FR-009, SC-004)

1. Con el menú mostrando 4 emisoras, reproduce una nueva desde la app.
2. Vuelve a inicio y mantén pulsado el icono: la nueva aparece primera (≤ 5 s).
3. Elimina una emisora del Historial y comprueba que desaparece del menú.
4. Limpia todo el historial: el menú deja de mostrar accesos de emisora.
5. Cambia el historial desde la web en otro dispositivo, trae la app a primer plano y comprueba que el menú se actualiza.

## Escenario D — Offline (FR-015, clarificación 2026-09-10)

1. Con el menú ya poblado, activa modo avión (`adb shell cmd connectivity airplane-mode enable`).
2. Abre la app: debe seguir mostrando la última lista conocida (sin avisos).
3. Pulsa un acceso directo: mensaje de error de reproducción con reintento, sin cierres ni pantallas en blanco.
4. Desactiva modo avión y pulsa **Reintentar** en el panel: reproduce.

## Escenario E — Privacidad y cuentas (US4, FR-010, SC-003)

1. Con emisoras visibles, cierra sesión desde Configuración.
2. Comprueba con `adb shell cmd shortcut get-shortcuts --user 0 com.izquierdojl.tolocharadio` que no queda ningún `hist-*`.
3. Inicia sesión con una cuenta sin historial: el menú no muestra emisoras.
4. Cambia de servidor/instancia desde Servidores y verifica que los accesos de la cuenta anterior desaparecen.

## Escenario F — Sesión caducada (FR-007)

1. Invalida la sesión (p. ej. cambia la contraseña desde la web para revocar el refresh, o espera a que caduque).
2. Pulsa un acceso directo del icono.
3. Esperado: si el refresh es posible, reproduce sin pedir nada; si no, se abre Login con el aviso "Tu sesión ha caducado. Inicia sesión de nuevo." y no se intenta reproducir.

## Escenario G — Degradación y límites (FR-012, SC-007)

1. Repite el paso "mantener pulsado el icono" en al menos 3 lanzadores distintos.
   - Esperado: nunca hay más accesos de los que el lanzador permite y las acciones propias del sistema siguen ahí.
2. Desactiva los accesos directos del lanzador (si el lanzador lo permite): la app abre y funciona con normalidad.

## Evidencia sugerida para la PR

- Salida de `adb shell cmd shortcut get-shortcuts` tras cada escenario.
- Captura del menú del icono en dos lanzadores.
- Salida en verde de `./gradlew testDebugUnitTest detekt ktlintCheck lintDebug`.
- Nota sobre favicons: si un lanzador no soporta iconos bitmap, cae al icono de la app (comportamiento esperado, FR-008).
