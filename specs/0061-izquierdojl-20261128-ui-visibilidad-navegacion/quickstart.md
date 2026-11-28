# Quickstart: Visibilidad UI y navegación

**Feature**: `0061-izquierdojl-20261128-ui-visibilidad-navegacion` | **Date**: 2026-11-28

Guía de validación manual y automática. Sin código de implementación: los detalles viven en [contracts/ui-contracts.md](contracts/ui-contracts.md), [contracts/design-tokens.md](contracts/design-tokens.md) y [data-model.md](data-model.md).

## Requisitos previos

- Rama `0061-izquierdojl-20261128-ui-visibilidad-navegacion`.
- Un servidor configurado con emisoras y, si se prueba el panel de podcast, al menos un podcast con un episodio reproducible.
- Dispositivo o emulador Android; para Cast, un receptor disponible en la red.
- Para los comandos, usar `./gradlew` en Linux/macOS o `.\gradlew.bat` en Windows PowerShell 7.

## Escenarios manuales

### 1. Tarjetas y emisora activa (SC-001, SC-002, SC-006, SC-009)

1. Abre Explorar en modo oscuro con al menos 6 tarjetas.
2. Comprueba que dos tarjetas contiguas se distinguen por superficie y borde, y que los chips y el texto secundario se leen sin esfuerzo.
3. Reproduce una emisora. Vuelve a Explorar, Favoritos, Historial y Mis emisoras: la tarjeta/fila activa muestra el badge y el botón cambia a pausa.
4. Pulsa el botón de la tarjeta activa: debe pausar; vuelve a pulsarlo: debe reanudar; no debe abrir la ficha.
5. Pulsa el resto de la tarjeta: debe abrir la ficha.
6. Provoca un favicon roto o usa una emisora sin favicon: debe verse el emblema, no un hueco.

### 2. Panel de radio (SC-003, SC-004, SC-010)

1. Reproduce una emisora y mide el panel: no debe superar 64 dp.
2. Comprueba que solo hay carátula, nombre, estado, play/pausa y silenciar; no debe aparecer país/idioma/códec/bitrate.
3. Pausa: el estado debe decir `Pausado`; reanuda: `En directo`.
4. Con Cast conectado, el nombre del dispositivo debe seguir visible.
5. Provoca un error: debe aparecer reintentar/editar servidor y ocultarse silenciar. Durante la carga, debe aparecer cancelar.

### 3. Panel de podcast (SC-003, SC-004)

1. Reproduce un episodio y mide el panel: no debe superar 112 dp.
2. Comprueba que hay como máximo dos líneas de texto, play/pausa centrado y grande, saltos a los lados, tiempos transcurrido/restante, progreso y Detener.
3. Prueba un episodio sin duración: solo debe aparecer el tiempo transcurrido y no debe dibujarse la barra.
4. Prueba un título largo: debe elidirse en una línea sin empujar los tiempos ni los controles.
5. Comprueba que los saltos anuncian los segundos configurados y que Detener cierra el episodio sin abrir el reproductor completo.

### 4. Barra inferior y navegación (SC-005, SC-001)

1. En modo radio, comprueba las etiquetas `Explorar`, `Favoritos`, `Historial` y `Mis emisoras` con fuente por defecto en 360 dp.
2. En modo podcast, comprueba `Inicio`, `Descubrir`, `Suscripciones`, `Cola` y `Favoritos` en 360 dp.
3. Cambia de sección: la seleccionada debe verse por el indicador, el color y el peso; el cambio debe ser un fade suave. Con TalkBack, cada destino debe anunciarse una sola vez.
4. Activa la reducción de animaciones del sistema: el cambio debe ser inmediato.
5. Ajusta la fuente al 200 % y usa una pantalla de 320 dp: las etiquetas no deben solaparse; si se eliden, TalkBack debe leer el nombre completo.
6. Comprueba que el orden y el número de destinos no han cambiado.

### 5. Modo claro (SC-007)

1. Cambia a modo claro y repite los escenarios 1 y 2.
2. Comprueba que las tarjetas tienen borde y elevación, que el panel se distingue y que el hueco de carátula es claro, no un bloque verde oscuro.
3. Cambia de tema con la app abierta: no deben quedar superficies mezcladas ni textos ilegibles.

### 6. Cabecera y barra (SC-001)

1. Comprueba un divisor visible bajo la cabecera y sobre la barra inferior.
2. Verifica que no se ha perdido altura útil ni se han movido los botones de menú, modo o Cast.

## Validación automática

```bash
# Linux/macOS
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew detekt ktlintCheck lintDebug
./gradlew connectedDebugAndroidTest   # requiere dispositivo/emulador

# Windows PowerShell 7
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat detekt ktlintCheck lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

Resultado esperado:

- Compilación OK y sin errores de Detekt/ktlint/Lint.
- `testDebugUnitTest` incluye `ContrastTest` y `PlayerStateMapperTest` en verde.
- `connectedDebugAndroidTest` incluye `CommonUiTest`, `MiniPlayerTest`, `EpisodeMiniPlayerTest`, `TolochaNavigationBarTest` y `FavoritesScreenTest` en verde.
- Sin regresiones en reproducción, navegación ni accesibilidad.

## Comprobaciones de contraste

`ContrastTest` debe verificar, como mínimo, los pares de [contracts/design-tokens.md](contracts/design-tokens.md):

- Texto secundario/tarjeta y texto de chip/chip ≥4.5:1 en ambos temas.
- Borde/tarjeta ≥1.5:1 en ambos temas.
- Tarjeta/fondo ≥1.3:1 en oscuro.
- Contenido seleccionado/indicador de navegación ≥4.5:1 y indicador/barra ≥3:1 en ambos temas.

Si un par no cumple, el test debe fallar con el nombre del token y los dos colores implicados.

## Notas de entorno

- El repo está configurado para Windows + PowerShell 7, pero CI y este entorno Linux usan `./gradlew`.
- Los scripts de `.specify/scripts/powershell` requieren `pwsh`; este entorno no lo tiene. Los artefactos de plan se han generado manualmente siguiendo el template y la constitución.
