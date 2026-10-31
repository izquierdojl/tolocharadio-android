# Quickstart: Continuidad de reproducción y buffer ajustable

**Feature**: `0042-izquierdojl-20261031-audio-playback-recovery`
**Purpose**: validar de extremo a extremo la recuperación, los ajustes y las
no-regresiones sin depender de una red real controlada.

## Prerrequisitos

- JDK 17 y Android SDK con `compileSdk` 37.
- Dispositivo o emulador con API 26 o superior.
- Servidor TolochaRadio accesible con credenciales válidas y al menos una
  emisora reproducible.
- `adb` disponible y dispositivo autorizado (`adb devices`).
- Para las pruebas de red: Wi-Fi del emulador/dispositivo o modo avión.

## Compilar e instalar

Linux/macOS:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Tests automáticos

```bash
./gradlew testDebugUnitTest
./gradlew detekt ktlintCheck lintDebug
./gradlew connectedDebugAndroidTest   # requiere dispositivo/emulador
```

Resultado esperado: todos los gates en verde, sin warnings nuevos sin
justificar.

## Escenarios de validación manual

### 1. Línea base

1. Abrir la app, configurar el servidor y reproducir una emisora.
2. Verificar audio continuo, mini-player y notificación con la emisora.
3. Pausar, reanudar y detener desde la app y desde la notificación.

**Esperado**: reproducción y controles actuales sin cambios.

### 2. Corte breve de red (10 s)

1. Reproducir una emisora.
2. Desactivar Wi-Fi/datos durante 10 s.
3. Reactivar la red.

Comandos de referencia:

```bash
adb shell svc wifi disable
sleep 10
adb shell svc wifi enable
```

**Esperado**:
- La app muestra "Reconectando (n/5)…" o "Sin conexión…" en el reproductor.
- El audio vuelve solo en la misma emisora, en el punto en directo, en menos de
  10 s desde que vuelve la red.
- No se crea una entrada duplicada en Historial.
- No hay que cerrar ni reiniciar la app.

### 3. Modo avión prolongado (30-60 s)

1. Reproducir una emisora.
2. Activar modo avión durante 30-60 s.
3. Desactivarlo.

```bash
adb shell cmd connectivity airplane-mode enable   # API 30+
sleep 45
adb shell cmd connectivity airplane-mode disable
```

**Esperado**:
- Estado "Sin conexión" sin bucle de reintentos.
- Al volver la red, reanudación automática si el ajuste está activo.
- Sin consumo perceptible de batería atribuible a reintentos.

### 4. Reanudación desactivada

1. En Configuración, desactivar "Reanudar al recuperar conexión".
2. Reproducir una emisora y cortar la red 15 s.
3. Restaurar la red.

**Esperado**:
- La emisora queda en pausa; no suena sola.
- Pulsar play reconecta la emisora y vuelve al directo.

### 5. Cambio de perfil mientras suena

1. Reproducir una emisora.
2. En Configuración, cambiar de "Equilibrado" a "Máxima estabilidad".

**Esperado**:
- Aviso "Aplicando ajuste…" y una única reconexión.
- La misma emisora vuelve al directo.
- No se duplica el historial.
- La selección persiste tras cerrar y reabrir la app.

### 6. Diferencia de arranque entre perfiles

1. Limpiar datos de la app o detener la reproducción.
2. Con "Equilibrado", medir el tiempo desde play hasta el primer audio en una
   red limitada (por ejemplo, emulador con throttling).
3. Repetir con "Máxima estabilidad".

**Esperado**: la diferencia es de 5 s o menos. Si supera 5 s, ajustar los
valores de `MAX_STABILITY` antes de cerrar la feature.

### 7. Segundo plano y pantalla apagada

1. Reproducir una emisora.
2. Bloquear la pantalla (`adb shell input keyevent 26`).
3. Esperar 30 s, cortar y restaurar la red.
4. Desbloquear y abrir la app.

**Esperado**:
- La recuperación ocurre sin abrir la app.
- Al volver, el mini-player y el reproductor completo muestran el estado real.
- No hace falta salir y entrar de la app.

### 8. Error de credenciales (401/403)

1. Reproducir una emisora.
2. Invalidar el token/credenciales del servidor (cambiar la contraseña en el
   servidor o revocar la sesión).
3. Provocar un corte de red o esperar a que el stream falle.

**Esperado**:
- La app intenta renovar la sesión automáticamente.
- Si no puede, muestra "La sesión del servidor ha caducado. Edita el servidor
  para continuar." con acción "Editar servidor".
- No hay reintentos ciegos del stream.
- "Editar servidor" abre el formulario del servidor activo.

### 9. Emisora no disponible (404/503)

1. Añadir una emisora personalizada con una URL que devuelva 404 o usar una
   emisora caída del catálogo.
2. Reproducirla.

**Esperado**:
- Reintentos acotados (máximo 5/60 s).
- Error accionable "La emisora no está disponible en este momento." con
  "Reintentar".
- Sin bucle infinito ni cierre de la app.

### 10. No regresiones

Probar con la feature activa:

- Llamada entrante o pérdida de foco: pausa y reanudación según spec 0019.
- Desconexión de auriculares/Bluetooth: pausa, sin reconexión automática.
- Temporizador de apagado: detiene la reproducción y cancela recuperación.
- Chromecast: reproducir, cambiar de dispositivo y desconectar; el audio local
  no se duplica y el buffer no interrumpe Cast.
- Notificación: play/pause/stop/mute y botón "Reintentar" en error.
- Rotación y navegación: el estado del reproductor se conserva.

## Protocolos de éxito medibles

Estos protocolos convierten los criterios SC-001, SC-005 y SC-007 en pasos
repetibles. Se ejecutan después de los escenarios funcionales.

### SC-001: 20 interrupciones transitorias

**Objetivo**: ≥ 18 de 20 recuperaciones automáticas en menos de 10 s, sin
duplicar historial ni reiniciar la app.

1. Reproducir una emisora.
2. Repetir 20 veces, alternando cortes de 5, 10 y 15 s:
   - desactivar Wi-Fi/datos;
   - esperar el corte;
   - reactivar la red;
   - medir el tiempo hasta que vuelve el audio.
3. Registrar por iteración: duración del corte, tiempo de recuperación, intento
   mostrado y si apareció una entrada nueva en Historial.
4. **Criterio**: 18/20 recuperaciones < 10 s; 0 duplicados de historial; 0
   reinicios de la app.

### SC-005: comparación A/B en red débil

**Objetivo**: "Máxima estabilidad" produce al menos un 50% menos de
interrupciones audibles que "Equilibrado", con ≤ 5 s extra de arranque.

1. En emulador, limitar la red:
   ```bash
   adb emu network speed gsm
   adb emu network delay 500
   ```
   En dispositivo físico, usar un router o una app de throttling equivalente.
2. Reproducir durante 5 minutos con "Equilibrado"; contar interrupciones
   audibles y medir el tiempo hasta el primer audio.
3. Repetir con "Máxima estabilidad".
4. **Criterio**: interrupciones de "Máxima estabilidad" ≤ 50% de las de
   "Equilibrado"; diferencia de arranque ≤ 5 s.

### SC-007: 30 minutos sin conectividad

**Objetivo**: 0 reintentos en bucle y sin consumo perceptible de batería.

1. Reproducir una emisora y activar modo avión.
2. Esperar 30 minutos.
3. Revisar `adb logcat -s PlaybackRecovery`: no debe haber
   `recovery_attempt` durante el periodo offline.
4. Comparar `adb shell dumpsys batterystats` antes y después; no debe haber un
   consumo atribuible a reintentos.
5. Desactivar modo avión; si el ajuste de reanudación está activo, la emisora
   debe volver sola.

## Logs de diagnóstico

```bash
adb logcat -s PlaybackRecovery
```

**Esperado**: eventos `recovery_attempt` con causa, intento, resultado,
duración y stationId anonimizado. No deben aparecer títulos, URLs, tokens ni
credenciales.

## Criterio de cierre

- Todos los escenarios anteriores pasan en dispositivo/emulador.
- Los protocolos SC-001, SC-005 y SC-007 se han ejecutado con los resultados
  esperados.
- `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck` y `lintDebug`
  están en verde.
- No hay regresiones en foco, auriculares, temporizador, Cast ni notificación.
- La diferencia de arranque de "Máxima estabilidad" es ≤ 5 s y la reanudación
  tras un corte cumple < 10 s.
