# Quickstart: Reproducción continua y reconexión automática

**Feature**: `0044-izquierdojl-20261101-live-playback-reconnect`
**Purpose**: validar de extremo a extremo el flujo continuo, el fallback HLS, la
reconexión, el precheck y las no-regresiones.

## Prerrequisitos

- JDK 17 y Android SDK con `compileSdk` 37.
- Dispositivo o emulador con API 26 o superior.
- Servidor TolochaRadio **0.29.0 o superior** accesible, con credenciales
  válidas y al menos una emisora reproducible.
- Para el escenario de compatibilidad: una instancia anterior a 0.29.0 o un
  proxy que devuelva 404/405 en `/live`.
- `adb` disponible y dispositivo autorizado (`adb devices`).
- Para las pruebas de red: Wi-Fi del emulador/dispositivo o modo avión.
- Las emisoras de referencia son Onda Cero (HLS) y Litera (AAC directo); la app
  las reproduce a través del proxy, no contra la URL pública directamente.

## Compilar e instalar

Linux/macOS:

```bash
# Fase 1/2: ruta continua activada por bandera
./gradlew assembleDebug -PlivePlayback=true

# Comportamiento anterior (ruta legacy)
./gradlew assembleDebug -PlivePlayback=false

adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug -PlivePlayback=true
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

En release, la bandera queda desactivada por defecto durante las fases 1–2; la
fase 3 cambia el valor por defecto a `true`. No hay ajuste visible en la app.

## Tests automáticos

```bash
./gradlew testDebugUnitTest
./gradlew detekt ktlintCheck lintDebug
./gradlew connectedDebugAndroidTest   # requiere dispositivo/emulador
```

Resultado esperado: todos los gates en verde, sin warnings nuevos sin
justificar. Los tests de dominio y ViewModel no dependen de red real.

## Escenarios de validación manual

### 1. Línea base y bandera

1. Instalar con `-PlivePlayback=false` y reproducir una emisora.
2. Instalar con `-PlivePlayback=true` y reproducir la misma emisora.
3. Comprobar en `adb logcat -s PlaybackRecovery` que la ruta continua se usa
   solo con la bandera activa.

**Esperado**: con la bandera desactivada se mantiene el comportamiento anterior;
con la bandera activada la app pide `/live`, reproduce audio progresivo y no
interpreta la respuesta como manifiesto. Probar una emisora por cada
`Content-Type` (`audio/aac`, `audio/mpeg`, `audio/aacp`) y una respuesta chunked
sin `Content-Length`; todas deben sonar como progresivo.

### 2. HLS de larga duración (Onda Cero)

1. Reproducir Onda Cero.
2. Dejarla sonando al menos 10 minutos.
3. Revisar que no hay recargas manuales ni saltos al inicio.

**Esperado**: audio continuo durante los 10 minutos, sin corte atribuible a la
caducidad de la sesión HLS del origen. La app no muestra un manifiesto ni pide
subrecursos HLS para la ruta continua.

### 3. Corte de red en AAC directo (Litera)

```bash
adb shell svc wifi disable
sleep 10
adb shell svc wifi enable
```

**Esperado**:
- La app muestra «Reconectando (n/6)…» si ya había sonado, o «Reintentando…» si
  el corte ocurre en el primer arranque.
- El audio vuelve solo en la misma emisora, en el punto en directo, en menos de
  20 segundos desde que vuelve la red, conservando el volumen.
- No se duplica la entrada de Historial y no hay que reiniciar la app.

### 4. Renovación de sesión HLS a los ~60 s

1. Reproducir Onda Cero con la bandera activa.
2. Mantenerla 3 minutos.
3. Verificar que no hay un corte visible al cumplirse cada minuto.

**Esperado**: el servidor renueva la sesión por dentro y la app sigue sonando;
no aparece `STATE_ENDED` ni se abre una segunda conexión.

### 5. Token caducado

1. En un entorno de pruebas, forzar un TTL corto del access token.
2. Reproducir una emisora y esperar a que caduque.
3. Provocar un corte o esperar el 401.

**Esperado**:
- La app renueva la sesión antes de reconectar si el token está a punto de
  caducar.
- Si la renovación falla, muestra «La sesión del servidor ha caducado. Edita el
  servidor para continuar.» con «Editar servidor».
- No hay reintentos ciegos del stream con un token inválido.

### 6. HLS no concatenable (415)

1. Reproducir una emisora HLS que el backend marque como no concatenable.
2. Observar la primera petición a `/live` y el fallback.

**Esperado**:
- Un único fallback a `/playback/{id}` con HLS.
- Si HLS reproduce el formato, el audio suena sin crash.
- Si no, aparece «Esta emisora usa un formato que este reproductor no admite» y
  no hay bucle de reintentos.
- El evento `event=live_fallback httpStatus=415` queda en `PlaybackRecovery`.

### 7. Servidor anterior a 0.29.0

1. Configurar una instancia antigua que devuelva 404/405 en `/live`.
2. Reproducir una emisora directa y una HLS.

**Esperado**: la app usa la ruta legacy de forma transparente, sin error visible
y sin degradar la reproducción actual.

### 8. Segundo plano y pantalla apagada

```bash
adb shell input keyevent 26   # bloquear pantalla
# esperar 10 minutos
adb shell input keyevent 26   # desbloquear
```

**Esperado**: la reconexión funciona con la pantalla apagada y la app en
segundo plano; al volver, el mini-player y el reproductor completo muestran el
estado real. No se abre un segundo player ni se duplica el audio.

### 9. Cambio de red Wi-Fi ↔ datos móviles

1. Reproducir una emisora por Wi-Fi.
2. Desactivar Wi-Fi para pasar a datos móviles.
3. Repetir en sentido contrario.

**Esperado**: una sola reconexión por cambio, sin conexiones paralelas, sin
audio duplicado y volviendo al directo.

### 10. Precheck de disponibilidad

1. Abrir la ficha de una emisora disponible y pulsar play.
2. Abrir una emisora no disponible.
3. Simular un backend lento (>2 s) o caído.

**Esperado**:
- Disponible: la ficha no retrasa el play más de 2 s por el precheck; si se
  pulsa play en los 5 s siguientes, se reutiliza el resultado y no se repite
  `/status`.
- No disponible: al abrir la ficha se muestra el motivo, el botón «Reproducir»
  se deshabilita o sustituye por el aviso y no arranca el player.
- Timeout o error de transporte: la app intenta reproducir y muestra
  `CONNECTING`, sin bloquear la interfaz.
- La ficha muestra el mismo `PlayerState` (`CONNECTING`/`PLAYING`/
  `RECONNECTING`/`FAILED`) y la acción «Reintentar» cuando corresponda.

### 11. No regresiones

Probar con la bandera activa:

- Llamada entrante o pérdida de foco: pausa y reanudación según spec 0019.
- Desconexión de auriculares/Bluetooth: pausa, sin reconexión automática.
- Temporizador de apagado: detiene la reproducción y cancela la recuperación.
- Chromecast: reproducir, cambiar de dispositivo y desconectar; el audio local
  no se duplica y Cast no adopta `/live`.
- Notificación: play/pause/stop/mute y botón «Reintentar» en error; refleja
  buffering/playing/paused y no duplica el texto detallado.
- Ficha de emisora: muestra el mismo estado que el reproductor y permite
  reintentar sin salir.
- Rotación y navegación: el estado del reproductor se conserva.
- Historial: una recuperación correcta no añade una entrada nueva.

## Protocolos de éxito medibles

### SC-001: 10 minutos HLS

Repetir 20 sesiones de 10 minutos con Onda Cero. **Criterio**: al menos 19/20
sesiones sin corte atribuible a la caducidad del origen.

### SC-002: 10 cortes de red

Repetir 10 cortes de 10 segundos. **Criterio**: al menos 9/10 recuperaciones
automáticas en menos de 20 segundos desde que vuelve la red, sin interacción.

### SC-003: matriz de fallos

Probar error de red, 5xx, cierre de socket, `STATE_ENDED`, estancamiento de 15 s,
token caducado y cambio de red. **Criterio**: el 100% termina en audio o en
`FAILED` con «Reintentar» en ≤120 segundos; ningún caso queda en spinner
indefinido.

### SC-004: 415

Probar al menos 5 emisoras HLS no concatenables. **Criterio**: el 100% inicia la
ruta de reserva o muestra el aviso en menos de 5 segundos, sin crash ni bucle.

### SC-005: segundo plano y red

Probar pantalla apagada 10 minutos, app en segundo plano 10 minutos y cambio
Wi-Fi ↔ datos móviles. **Criterio**: el 100% recupera o muestra error
accionable, sin audio duplicado.

### SC-006: precheck

Medir 20 comprobaciones de disponibilidad. **Criterio**: al menos 19/20
responden en ≤2 segundos; un `playable=false` no arranca el player.

### SC-007: diagnóstico

Revisar `adb logcat -s PlaybackRecovery` tras una sesión con fallos.
**Criterio**: el 100% de los eventos de intento/fallback incluye causa, intento
y código HTTP o de error; no aparecen tokens, correos, URLs completas ni títulos.

### SC-008: sesiones de 30 minutos

Repetir 10 sesiones de 30 minutos en las emisoras HLS antes problemáticas.
**Criterio**: al menos 9/10 sin interrupciones visibles. Se considera
interrupción visible un silencio audible de más de 1 segundo, un cambio a
`RECONNECTING`/`FAILED` o una intervención manual del usuario.

## Comandos útiles

```bash
# Estado de red y modo avión
adb shell svc wifi disable
adb shell svc wifi enable
adb shell cmd connectivity airplane-mode enable
adb shell cmd connectivity airplane-mode disable

# Logs del supervisor
adb logcat -s PlaybackRecovery

# Tests y gates
./gradlew testDebugUnitTest detekt ktlintCheck lintDebug
```

## Referencias

- [spec.md](./spec.md)
- [data-model.md](./data-model.md)
- [contracts/playback-live-api.md](./contracts/playback-live-api.md)
- [contracts/playback-live-ui.md](./contracts/playback-live-ui.md)
