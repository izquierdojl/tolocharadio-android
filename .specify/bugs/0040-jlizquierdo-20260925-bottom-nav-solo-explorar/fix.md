# Bug Fix: La barra de navegación inferior solo muestra "Explorar"

- **Slug**: 0040-jlizquierdo-20260925-bottom-nav-solo-explorar
- **Fixed**: 2026-09-25
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Se restauró el `weight` de los 5 destinos de la `NavigationBar` inferior. El
`TooltipBox` que envolvía cada `NavigationBarItem` (commit `82b3995`) anulaba el
`Modifier.weight(1f)` del item, de modo que el primer destino ocupaba todo el
ancho y los otros cuatro se medían a 0. Ahora cada `TooltipBox` vive dentro de un
`Box(Modifier.weight(1f))`, que sí es hijo directo del `Row` de la barra. Se
extrajo además la barra a `TolochaNavigationBar` (mismo fichero) para poder
fijarla con un test de UI sin Hilt.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` | modified | Import de `Box`; `Box(Modifier.weight(1f))` envolviendo cada `TooltipBox`; barra extraída a `TolochaNavigationBar(currentRoute, onNavigate)` `internal` |
| `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavigationBarTest.kt` | added test | Compone la barra real y exige 5 destinos visibles, con ancho > 0 y repartidos |

## Diff Highlights (optional)

```kotlin
NavigationBar {
    val rowScope = this
    BOTTOM_DESTS.forEach { dest ->
        val tooltipState = rememberTooltipState()
        Box(Modifier.weight(1f)) {          // <- restaura el weight como hijo directo
            TooltipBox(...) {
                with(rowScope) {
                    NavigationBarItem(
                        selected = ...,
                        onClick = { onNavigate(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                    )
                }
            }
        }
    }
}
```

El `TooltipBox` (Material3 1.4.0, `BasicTooltipBox`) aplica su `modifier` a un
`Box` interno, no a su nodo raíz, por lo que pasar `Modifier.weight(1f)` al
`TooltipBox` no habría bastado: hacía falta un `Box` con `weight` como hijo
directo del `Row`.

## Tests Added or Updated

- `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavigationBarTest.kt::todosLosDestinosSonVisiblesYRepartenElAncho`
  — fija que los 5 `contentDescription` ("Explorar", "Favoritos", "Historial",
  "Mis emisoras", "Configuración") estén visibles, tengan ancho > 0 y sus centros
  estén ordenados de izquierda a derecha ocupando > 50% del ancho de la barra.
  En la versión con el bug, Favoritos/Historial/… quedan a ancho 0 y el test falla.

## Local Verification

- Comandos run (Windows, `.\gradlew.bat ...`):
  - `assembleDebug` → **BUILD SUCCESSFUL**
  - `assembleDebugAndroidTest` → **BUILD SUCCESSFUL** (el test de UI compila)
  - `testDebugUnitTest detekt ktlintCheck lintDebug` → **BUILD SUCCESSFUL** (gates
    obligatorios en verde)
- Manual checks: **no ejecutados en dispositivo** — `adb devices` no lista ningún
  emulador/teléfono conectado, así que `TolochaNavigationBarTest` (instrumentado)
  no se pudo ejecutar aquí, solo compilar. Verificación visual pendiente.

## Deviations from Assessment

- **Extracción de `TolochaNavigationBar`** (assessment la listaba como alternativa
  y como vía para el test preferido). Se hizo dentro del único fichero
  `TolochaNavGraph.kt` para poder testear la **producción** en lugar de replicar el
  patrón; no cambia el comportamiento, solo la estructura.
- **Receptor explícito `with(rowScope)`**: al envolver el item en un `Box`
  (BoxScope), el compilador ya no resolvía implícitamente la extensión
  `RowScope.NavigationBarItem`; se captura el `RowScope` de `NavigationBar` y se
  invoca con receptor explícito.
- El resto de la remediación preferida se aplicó tal cual (se conserva el tooltip
  de accesibilidad de spec 012; no se revirtió T016).

## Follow-ups

- Ejecutar `/speckit.bug.test slug=0040-jlizquierdo-20260925-bottom-nav-solo-explorar`
  en emulador/dispositivo: abrir la app y confirmar visualmente los 5 iconos y que
  cada uno navega; opcionalmente `connectedDebugAndroidTest`.
- Revisar si otros `TooltipBox { NavigationBarItem }` aparecen en el futuro: es un
  patrón que pierde el `weight` (documentado en el KDoc de `TolochaNavigationBar`).
- Sin cambios de dependencias ni de gates.
