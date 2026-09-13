# Phase 1 — Quickstart: Reordenación intuitiva de favoritos

**Feature**: 0034-jlizquierdo-20260913-favorites-reorder-animation
**Date**: 2026-09-13

Guía de validación end-to-end. No incluye código de implementación; los detalles viven en
`plan.md`, `data-model.md` y `contracts/favorites-reorder-ui.md`.

## Requisitos previos

- Windows + PowerShell 7 con `gradlew.bat` (CI usa `./gradlew`).
- JDK 17.
- Para prueba manual: dispositivo/emulador Android (API 26+) y un **servidor** TolochaRadio
  con credenciales configuradas y, al menos, **2 emisoras favoritas**.

## Compilar y verificar gates

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat detekt ktlintCheck lintDebug
```

Resultado esperado: los tres comandos en verde.

## Tests automatizados

### Unitarios (obligatorios, Red-Green)

```powershell
.\gradlew.bat testDebugUnitTest --tests "*FavoritesViewModelTest"
```

Escenarios que deben pasar (ver contrato):

- `moveUp`/`moveDown` intercambian con la vecina y disparan el guardado.
- En el extremo, no hay movimiento ni escritura; el ítem se reporta deshabilitado.
- Con `offline = true`, `moveUp`/`moveDown` son no-op.
- El fallo de guardado restaura el orden confirmado y emite mensaje.

### UI Compose (flujo crítico, requiere dispositivo/emulador)

```powershell
.\gradlew.bat connectedDebugAndroidTest --tests "*FavoritesScreenTest"
```

Escenarios que deben pasar:

- El menú de desbordamiento ofrece "Mover arriba"/"Mover abajo".
- "Mover arriba" en la segunda fila la mueve a la primera.
- Con una sola favorita o `offline`, no aparecen asa ni menú de movimiento.
- En modo cuadrícula no hay asa.

## Validación manual (dispositivo)

1. Abrir **Favoritos** en vista de lista con ≥2 favoritas en línea.
2. **Arrastre en vivo (US1)**: mantener el dedo sobre el asa y subir/bajar. La fila activa
   debe verse elevada y las demás desplazarse con animación **antes** de soltar.
3. **Soltar**: el orden queda donde se veía y aparece el indicador de guardado; al
   refrescar/volver, el nuevo orden persiste.
4. **Menú accesible (US3)**: abrir "..." de una fila → "Mover arriba"/"Mover abajo".
   Verificar que en la primera/última posición la opción queda deshabilitada.
5. **Lista larga (US2)**: con muchas favoritas, arrastrar hasta el borde superior/inferior
   y comprobar que la lista se auto-desplaza, más rápido cuanto más cerca del borde.
6. **Offline**: activar modo avión (o apuntar a un servidor inaccesible) y entrar en
   Favoritos; debe avisar "Mostrando caché sin conexión." y no permitir reordenar.
7. **Reproducción**: con una emisora sonando, reordenar y confirmar que el audio no se
   detiene ni se reinicia.
8. **Reducir movimiento**: activar la preferencia del sistema (Ajustes → Accesibilidad →
   Quitar animaciones) y comprobar que el reorden sigue siendo comprensible sin animación.

## Criterios de aceptación cubiertos

- FR-001…FR-018 y SC-001…SC-006 según `spec.md`.
- Regresión: abrir ficha, reproducir y quitar/deshacer siguen funcionando (FR-017).

## Referencias

- [spec.md](./spec.md) — requisitos y criterios de éxito.
- [data-model.md](./data-model.md) — entidades y estado de UI.
- [contracts/favorites-reorder-ui.md](./contracts/favorites-reorder-ui.md) — contrato de UI.
