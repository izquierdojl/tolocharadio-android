# Quickstart: Control de volumen del dispositivo Chromecast

**Feature**: `0035-jlizquierdo-20260916-cast-volume-control` | **Date**: 2026-09-16

Guía de validación de la feature (unit tests + comprobación manual end-to-end). Detalles de
diseño en [plan.md](./plan.md), [data-model.md](./data-model.md) y
[contracts/cast-volume.md](./contracts/cast-volume.md).

## Prerrequisitos

- Windows + PowerShell 7, JDK 17.
- Dispositivo Android físico (el emulador no descubre receptores Cast reales) con la app
  instalada y un servidor configurado (`Configuración → Servidores`).
- Un dispositivo compatible con Cast en la **misma red WiFi** (Chromecast, Google TV,
  altavoz Nest u otro).
- Permisos de descubrimiento Cast concedidos (ubicación / dispositivos WiFi cercanos /
  red local) al abrir la app.

## 1. Gates automáticos (obligatorios)

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat detekt ktlintCheck lintDebug
.\gradlew.bat assembleDebug
```

Esperado: todo en verde. Los tests nuevos cubren el controlador, el wrapper de `Player` y el
ViewModel (ver `research.md` D10).

## 2. Compilar e instalar en el dispositivo

```powershell
.\gradlew.bat installDebug
```

Esperado: la app arranca en `Idle` y la reproducción local sigue funcionando igual que antes
(sin regresión de la spec 004/011).

## 3. Escenarios manuales

### 3.1 Teclas físicas con Cast conectado (US1, FR-001/002, SC-001/002/009)

1. Reproduce una emisora y conecta a un dispositivo Cast desde el botón Cast.
2. Con el audio sonando en el receptor, pulsa **subir/bajar volumen** en el móvil.

Esperado:

- Aparece la barra de volumen del sistema con la identificación de sesión remota (icono).
- El volumen audible del receptor sube/baja un paso por pulsación.
- El volumen multimedia del propio móvil **no** cambia (repítelo 10 veces: SC-001).
- Alternativa automatizada de teclas: `adb shell input keyevent 24` (subir) y `25` (bajar).

3. Desconecta del Cast y pulsa volumen: vuelve a controlar el volumen local del móvil
   (SC-009).

### 3.2 Slider del reproductor completo (US2, FR-003/004/006, SC-003/005)

1. Con Cast conectado, abre el reproductor completo desde el mini-player.
2. Observa el slider: debe mostrar el **nivel real** del receptor (no siempre 100 %).
3. Arrástralo de 0 a 100 % sin soltar y confirma el cambio audible fluido (< 1 s, SC-003).
4. Cierra el panel, cambia el volumen desde el mando del receptor y vuelve a abrirlo: el
   slider muestra el nivel actualizado (no un valor fijo).

Esperado: sin saltos, sin reinicio del stream y sin interrupciones de reproducción (SC-007).

### 3.3 Cambios desde el propio receptor (US3, FR-005, SC-004)

1. Con la app abierta en el full-player, baja el volumen con el mando del TV o la app Google
   Home.
2. Comprueba que el slider de la app se actualiza en ≤ 2 s.

### 3.4 Silencio (US4, FR-007/008/015, SC-006)

1. Con Cast conectado, pulsa **Silenciar** en el mini-player: el receptor deja de sonar y el
   icono queda en silencio.
2. Pulsa de nuevo: se restaura exactamente el nivel previo.
3. Silencia y sube volumen con la tecla: el audio vuelve al nuevo nivel (des-silencia).
4. Silencia en local, conecta al Cast: el receptor arranca silenciado y el botón sigue
   marcado (FR-015); desconecta y comprueba el comportamiento inverso.
5. Pulsa **Mute** en la notificación multimedia con Cast conectado: silencia el receptor.

### 3.5 Casos límite

- **Receptor sin soporte de volumen** (p. ej. Chromecast dongle sin control CEC): tras ~3 s
  sin eco, el slider desaparece y aparece el aviso "Este dispositivo no permite ajustar el
  volumen desde el móvil"; la reproducción continúa y **las teclas vuelven a controlar el
  volumen del móvil** (FR-009).
- **Pulsaciones rápidas**: mantén pulsada la tecla o púlsala repetidamente; el volumen remoto
  sigue el ritmo sin perder pulsaciones y sin falsos avisos de no soportado (FR-005, FR-009).
- **Ajuste fallido transitorio**: simula un corte breve de red pulsando volumen varias veces;
  al volver la red, el slider se corrige al nivel real sin avisos (FR-014).
- **Pérdida de sesión durante un ajuste**: apaga/enruta el receptor fuera de la red; la UI no
  se bloquea y se aplica el fallback local existente (spec 0031/0032).
- **App en segundo plano**: con reproducción Cast activa, pulsa volumen desde la pantalla de
  inicio o con la pantalla apagada; sigue controlando el receptor (FR-011).
- **Cast conectado en pausa**: los controles de volumen siguen disponibles (FR-013).

## 4. Criterios de aceptación rápidos

| Criterio | Verificación |
|----------|--------------|
| SC-001 | 10 pulsaciones de tecla → 10 cambios remotos, 0 cambios locales |
| SC-002 | La barra del sistema aparece identificando la sesión remota |
| SC-003 | Slider 0→100 % audible en < 1 s |
| SC-004 | Cambio externo reflejado en ≤ 2 s (repetir 5 veces) |
| SC-005 | Reabrir full-player muestra el nivel real |
| SC-006 | Silenciar/des-silenciar restaura el nivel exacto |
| SC-007 | Ningún ajuste pausa ni reinicia el stream |
| SC-008 | Repetir 3.1–3.4 en 2 receptores distintos (p. ej. Nest y Google TV) |
| SC-009 | Tras desconectar, la primera pulsación vuelve a ajustar el móvil |

## 5. Diagnóstico

- Logs: `adb logcat -s CastPlayerManager PlaybackVolumeController` — nunca deben contener
  credenciales ni PII (solo tag + causa + `code`/`status`).
- Si las teclas no aparecen con Cast conectado, comprueba que el template de la
  `MediaSession` expone los comandos de dispositivo (`contracts/cast-volume.md` §1) y que el
  wrapper está instalado como player de la sesión.
