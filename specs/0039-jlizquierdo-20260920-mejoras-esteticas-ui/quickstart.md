# Quickstart: Mejoras estéticas UI

**Feature**: `0039-jlizquierdo-20260920-mejoras-esteticas-ui` | **Date**: 2026-09-20

Guía de validación manual y automática. Sin código de implementación: los detalles viven en `contracts/ui-contratos.md` y `data-model.md`.

## Requisitos previos

- Rama `0039-jlizquierdo-20260920-mejoras-esteticas-ui`, un servidor configurado y al menos 2 favoritas.

## Escenarios manuales

1. **Favoritas sin menú + arrastre**: abre favoritas con 2+ emisoras; verifica que no hay botón de más opciones en ninguna fila; arrastra una emisora a otra posición, suelta, sale y vuelve; el orden persiste y aparece el aviso de guardado habitual.
2. **Panel mínimo**: reproduce una emisora; el mini-player muestra solo play/pausa y silenciar (sin copiar enlace). Provoca un error (emisora no disponible) y verifica que aparece reintentar.
3. **Ficha con sección Enlace**: con una emisora sonando, pulsa el panel para abrir la ficha; localiza la sección "Enlace" encima de la homepage; pulsa Compartir y verifica que el sistema recibe el enlace real; pulsa Copiar y verifica "Enlace copiado" pegándolo fuera de la app. Con una emisora sin URL, verifica "Enlace no disponible".
4. **Encabezados**: navega por Explorar, Favoritas e Historial; cada una muestra título + subtítulo ("Descubre emisoras de todo el mundo." / "Tus emisoras guardadas, en tu orden." / "Lo último que has escuchado.") con la misma apariencia.

## Validación automática (gates obligatorios)

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat detekt ktlintCheck lintDebug
.\gradlew.bat connectedDebugAndroidTest   # requiere dispositivo/emulador (flujos C1–C4)
```

Resultado esperado: compilación OK, unit tests OK, sin errores de Detekt/ktlint/Lint y tests de UI de favoritas, panel y ficha en verde.
