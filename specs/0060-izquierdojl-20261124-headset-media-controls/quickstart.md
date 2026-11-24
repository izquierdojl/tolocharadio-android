# Quickstart: validación de controles de auriculares

**Feature**: `0060-izquierdojl-20261124-headset-media-controls`
**Date**: 2026-11-24

## Requisitos

- Dispositivo Android 26+ (o emulador para la UI; las teclas físicas requieren
  dispositivo real).
- La app instalada con un servidor configurado y al menos un podcast.
- Un auricular con cable (3,5 mm o USB-C) y uno Bluetooth con botones
  anterior/siguiente.
- Opcional: un auricular Bluetooth sin botones laterales y un Chromecast para
  comprobar la degradación.

## Comprobaciones automáticas

Linux/macOS:

```bash
./gradlew testDebugUnitTest
./gradlew detekt ktlintCheck lintDebug
./gradlew assembleDebug
```

Windows (PowerShell 7):

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat detekt ktlintCheck lintDebug
.\gradlew.bat assembleDebug
```

Resultado esperado: `BUILD SUCCESSFUL` y 0 tests fallidos.

Tests que deben existir y pasar:

- `HeadsetControlSettingsTest` — opciones, defaults y saneado.
- `HeadsetControlPolicyTest` — matriz contenido × comando × ajustes y límites.
- `InstancePrefsTest` — persistencia, defaults y valores inválidos.
- `HeadsetControlSettingsCacheTest` — emisión del `StateFlow` y valor inicial.
- `HeadsetControlsControllerTest` — dedupe de repeticiones, pausa, radio,
  gestos desactivados, límites y comandos de sesión.
- `HeadsetVolumeGestureHandlerTest` — corta/larga, fondo, llamada, foco,
  cancelación y límites.
- `EpisodePlayerViewModelTest` — saltos con 5/15/30/60 s y pausa.
- `SettingsViewModelTest` — estado y setters de la sección.
- `HeadsetSettingsSectionTest` (Compose) — valores, cambio y switch.
- `EpisodeMiniPlayerTest` / `EpisodeFullPlayerSheetTest` — `contentDescription`
  de salto con los segundos configurados.

## Escenario 1 — Ajustes y persistencia (SC-005)

1. Abrir **Configuración → Controles de auriculares**.
2. **Esperado**: 15 s atrás y 30 s adelante por defecto; interruptor activo.
3. Cambiar a 5 s atrás y 60 s adelante; activar/desactivar el interruptor.
4. Cerrar la app, reabrirla y volver a Configuración.
5. **Esperado**: los valores y el estado del interruptor se conservan.
6. Cambiar los valores mientras suena un episodio; el siguiente salto usa el
   valor nuevo sin reiniciar la reproducción.
7. **Esperado**: la app no solicita permisos nuevos al abrir o ajustar los
   controles.

## Escenario 2 — Botón central (SC-001)

1. Reproducir una emisora con el auricular con cable.
2. Pulsar una vez el botón central.
3. **Esperado**: pausa en ≤ 500 ms; la notificación y la app muestran pausa.
4. Pulsar de nuevo.
5. **Esperado**: reanuda; en radio no cambia de emisora. No se exige volver al
   borde live.
6. Bloquear la pantalla y repetir.
7. **Esperado**: funciona sin desbloquear.
8. Repetir con el auricular Bluetooth y con un podcast pausado.
9. **Esperado**: el podcast reanuda en la misma posición.

## Escenario 3 — Volumen con cable (SC-002, SC-003)

1. Reproducir un podcast con la app visible y el auricular con cable.
2. Pulsar brevemente volumen arriba y abajo.
3. **Esperado**: cambia el volumen del sistema; no hay salto.
4. Mantener pulsado volumen arriba ≥ 0,5 s.
5. **Esperado**: avanza los segundos configurados (30 s por defecto), sin
   pausar ni reiniciar el audio.
6. Repetir con volumen abajo.
7. **Esperado**: retrocede los segundos configurados (15 s por defecto), sin
   bajar de 0.
8. Repetir cerca del final del episodio.
9. **Esperado**: la posición se limita al final y el episodio termina de forma
   normal.
10. Repetir con el episodio pausado.
11. **Esperado**: cambia la posición y sigue pausado.

## Escenario 4 — Bluetooth con botones laterales (SC-002, SC-011)

1. Emparejar un auricular Bluetooth con botones anterior/siguiente.
2. Reproducir un podcast.
3. Pulsar siguiente y anterior.
4. **Esperado**: salta los segundos configurados; no cambia de episodio.
5. Mantener pulsado volumen en el Bluetooth.
6. **Esperado**: el volumen lo gestiona el sistema; no se exige salto (limitación
   documentada).
7. Repetir con la pantalla apagada.
8. **Esperado**: anterior/siguiente siguen funcionando; el volumen es nativo.
9. Repetir con un auricular Bluetooth sin botones laterales.
10. **Esperado**: play/pausa funciona; no hay salto si el volumen no llega a la
    app (no es un fallo).
11. Desconectar y reconectar el auricular a mitad de un salto.
12. **Esperado**: sin cierre, sin salto duplicado y los controles vuelven a
    funcionar al reconectar.

## Escenario 5 — Radio en directo (SC-008)

1. Reproducir una emisora en directo.
2. Mantener pulsado volumen arriba/abajo y pulsar anterior/siguiente.
3. **Esperado**: no hay salto, ni reinicio, ni cambio de emisora; el volumen
   mantiene su comportamiento nativo.
4. Pulsar el botón central.
5. **Esperado**: pausa/reanuda con normalidad.

## Escenario 6 — Gestos desactivados (SC-004, SC-014)

1. Desactivar «Usar gestos de salto en el auricular».
2. Reproducir un podcast.
3. Mantener pulsado volumen y pulsar anterior/siguiente.
4. **Esperado**: no hay salto; el volumen es nativo y anterior/siguiente no
   hacen nada.
5. Usar los botones de salto del reproductor y de la notificación.
6. **Esperado**: siguen saltando los segundos configurados y las etiquetas del
   reproductor y la notificación muestran esos segundos; el interruptor solo
   afecta a los gestos del auricular.
7. Pulsar el botón central.
8. **Esperado**: sigue pausando y reanudando.

## Escenario 7 — Sin contenido y errores (SC-007)

1. Cerrar la reproducción y pulsar cualquier control del auricular.
2. **Esperado**: no se arranca audio.
3. Provocar un error de red en un episodio y ejecutar un salto.
4. **Esperado**: no hay salto ni interrupción; la UI de error sigue disponible.
5. Repetir 100 gestos en 30 minutos.
6. **Esperado**: sin cierres inesperados, bloqueos ni cortes de audio.

## Escenario 8 — Llamada y foco de audio (FR-020)

1. Reproducir un podcast y recibir una llamada.
2. Probar volumen y anterior/siguiente durante la llamada.
3. **Esperado**: no se salta por encima de la llamada; el volumen de llamada no
   se intercepta.
4. Al terminar la llamada, la reproducción se comporta según la política actual
   de foco de audio.

## Escenario 9 — Cast (FR-014)

1. Con un Chromecast conectado, reproducir un episodio.
2. Usar los botones de salto de la notificación.
3. **Esperado**: el salto se aplica al audio remoto con los segundos
   configurados.
4. Si el receptor no admite seek, no suena audio local duplicado ni se produce
   un efecto inesperado.

## Escenario 10 — Usabilidad de ajustes (SC-009)

1. Reclutar 5 personas que no conozcan la app.
2. Pedirles que encuentren «Controles de auriculares» y cambien retroceso y
   avance.
3. **Esperado**: todas lo consiguen en menos de 60 s sin ayuda.
4. Registrar tiempos y dificultades.

## Atajos de prueba con `adb`

En emulador o dispositivo de desarrollo se pueden simular teclas (no sustituyen
la matriz manual de auriculares):

```bash
adb shell input keyevent 85   # play/pausa
adb shell input keyevent 87   # siguiente
adb shell input keyevent 88   # anterior
adb shell input keyevent --longpress 24  # volumen arriba largo
adb shell input keyevent --longpress 25  # volumen abajo largo
```

**Esperado**: con un episodio activo y la app visible, 87/88 y volumen largo
saltan; 85 pausa/reanuda. Con radio, 87/88 no cambian de emisora.

## Matriz mínima de dispositivos

| Dispositivo | Qué valida |
|-------------|-------------|
| Cable 3,5 mm o USB-C | Volumen corto/largo, botón central, pausa |
| Bluetooth con anterior/siguiente | Saltos, botón central, pantalla apagada |
| Bluetooth sin laterales | Play/pausa y limitación de volumen documentada |
| Pantalla bloqueada | Botón central y anterior/siguiente |
| Chromecast (opcional) | Notificación y degradación de Cast |

## Diagnóstico

- Si no hay salto con volumen: comprobar que la app está visible, que hay un
  episodio activo, que los gestos están activados y que el auricular entrega las
  teclas a la app (probarlo con cable).
- Si el volumen Bluetooth no salta: es la limitación esperada; usar
  anterior/siguiente o los controles en pantalla.
- Si anterior/siguiente cambia de episodio: revisar que la sesión está
  interceptando `KEYCODE_MEDIA_NEXT/PREVIOUS` y los comandos de la sesión.
- Si la notificación no refleja los segundos: comprobar que se reconstruyen los
  `CommandButton` al emitir `HeadsetControlSettingsCache.settings`.
- Logs: filtrar por `HeadsetControls`; no deben contener PII.
