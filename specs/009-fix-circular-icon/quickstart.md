# Quickstart: verificar el fix del icono circular (009-fix-circular-icon)

Guía de validación end-to-end (SC-001–SC-004). Sin código nuevo; solo build + inspección visual.

## Prerrequisitos

- Rama `009-fix-circular-icon` con el fix aplicado.
- Emulador o dispositivo con Android 8+ (API 26+) y un launcher que permita máscara circular (p. ej. Pixel Launcher o "Forma de icono" en opciones de desarrollador / fondo y estilo).
- `local.properties` con SDK configurado.

## Pasos

1. **Compilar e instalar**
   ```powershell
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Esperado: build OK y app instalada (gate III: compila + Lint sin errores).

2. **Máscara circular (SC-001)** — Poner forma de icono circular en el sistema; abrir cajón de apps, pantalla de inicio y Ajustes → Apps → TolochaRadio.
   Esperado: sol, sierra y antena con su punto 100% visibles, sin tocar el borde del círculo. Repetir en 2 densidades (p. ej. un emulador mdpi/xhdpi y un dispositivo real, o cambiando densidad con `adb shell wm density`).

3. **Matriz resto de formas (SC-002/SC-003)** — Cambiar a squircle, gota y cuadrada y repetir la inspección; activar "Iconos con tema" (themed icons) y comprobar la variante mono.
   Esperado: emblema centrado con aire uniforme en las 4 formas; silueta mono reconocible; cero regresiones.

4. **Legacy / tamaño pequeño (FR-004/FR-005)** — Ver el icono a 48dp (vista de ajustes/notificación o zoom-out del launcher).
   Esperado: sierra legible y sol visible; en launchers que usen el webp legacy, misma composición sin recorte.

## Criterio de aceptación

- [X] SC-001: 100% del emblema visible en circular en ≥ 2 densidades.
- [X] SC-002: sin recortes en ≥ 3 formas (circular, squircle, cuadrada).
- [X] SC-003: 0 regresiones en la matriz circular/squircle/gota/cuadrada.
- [X] SC-004: verificación completa < 10 minutos.

Detalles de composición y tolerancias: ver [data-model.md](data-model.md); decisiones de geometría y regeneración: ver [research.md](research.md).
