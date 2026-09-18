# Quickstart: Volumen contextual único estilo Pocket Casts

**Feature**: 0037-jlizquierdo-20260918-contextual-volume | **Fecha**: 2026-09-18

## 1. Requisitos

- Android Studio / JDK 17, PowerShell 7.
- Dispositivo Android físico (el emulador no descubre receptores Cast) — probado en Redmi
  M2101K7AG (Android 17).
- Receptor Cast en la misma red (Chromecast/Google TV/altavoz) con control de volumen
  físico (mando del TV o Google Home para cambios externos).

## 2. Compilar e instalar

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug   # o instalar el APK desde Android Studio
```

Gates antes de validar en dispositivo: `.\gradlew.bat testDebugUnitTest detekt ktlintCheck
lintDebug`.

## 3. Validación manual (por historia de usuario)

### §3.1 US1 — Al conectar, el volumen del receptor no cambia (SC-001)

1. Ajusta el receptor al **30%** con el mando del TV o Google Home.
2. Reproduce una emisora en local en la app y conéctate al receptor.
3. **Verde**: la reproducción arranca en el receptor con volumen audible al 30%; ningún
   salto al 100%, ni en la barra del sistema ni en el sonido.
4. Repite desconectando y reconectando (incluye una pérdida de red si es posible): el
   volumen del receptor permanece inalterado.

### §3.2 US2 — Una sola franja de volumen contextual (SC-002/SC-003/SC-004)

1. Con la app sonando en **local**, pulsa las teclas de volumen: cambia solo el volumen
   del teléfono; la barra del sistema es la del teléfono.
2. Conéctate al Cast y pulsa las teclas: cambia **solo** el volumen del receptor y la
   barra del sistema muestra el nombre del dispositivo.
3. Abre el reproductor completo: **no** hay deslizador de volumen (ni aviso).
4. Desconéctate y pulsa las teclas: vuelven a controlar el teléfono al primer intento.
5. Con Cast activo y la app en segundo plano, pulsa las teclas: siguen controlando el
   receptor (FR-010).

### §3.3 US3 — Silencio coherente (SC-005)

1. Silencia la app en local, conecta al Cast: el receptor arranca silenciado y el botón
   sigue marcado; des-silencia: recupera exactamente el nivel previo.
2. Silencia el receptor con el mando del TV, con la app sin silencio, y conecta: el
   receptor **permanece** silenciado y el botón de la app lo refleja.
3. Con Cast activo y silenciado, sube volumen con las teclas: el audio se restablece al
   nuevo nivel (nativo del receptor).
4. Prueba MUTE desde la notificación con la app en segundo plano.
5. Con el silencio externo adoptado (paso 2), desconecta: la reproducción local retoma
   **con sonido** y el botón de silencio queda desactivado (FR-006).

### §3.4 US4 — El volumen visible es el real (SC-006)

1. Con Cast activo y la app abierta, cambia el volumen desde el mando del TV: la barra del
   sistema refleja el nuevo nivel en ≤ 2 s (repetir 5 veces).
2. Ajusta volumen con teclas muy rápidas: el receptor sigue el ritmo sin desincronizarse.

### §3.5 Regresión local y edge cases

1. Reproducción local sin Cast: play/stop des-silencia (regla spec 004), volumen del
   teléfono normal, mini-player y notificación intactos.
2. Auriculares Bluetooth + Cast activo: las teclas controlan el Cast.
3. Cast conectado en pausa: las teclas siguen ajustando el dispositivo.
4. **Audio**: cualquier síntoma de "no suena tras conectar" → **PARAR** y diagnosticar
   (regresión del fix v2 de 0036, research D6): no tocar el wiring del player de sesión.

## 4. Criterios de salida

- Tabla SC-001…SC-007 de `spec.md` en verde en el dispositivo de prueba.
- Gates CI verdes (`testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug`).
- Sin cambios de comportamiento fuera de volumen/silencio.
