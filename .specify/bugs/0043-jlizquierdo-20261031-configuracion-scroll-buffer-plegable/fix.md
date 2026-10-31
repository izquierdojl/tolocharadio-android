# Bug Fix: Configuración sin scroll y opciones de buffer siempre desplegadas

- **Slug**: 0043-jlizquierdo-20261031-configuracion-scroll-buffer-plegable
- **Fixed**: 2026-10-31
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Se añadió desplazamiento vertical al contenido de Configuración y se dejó el
`SectionHeader` fijo, de modo que "Gráficas" y "Acerca de" vuelven a ser
alcanzables aunque la sección de reproducción ocupe más alto del disponible. Las
tres opciones de buffer ahora están plegadas por defecto tras una cabecera
"Buffer de audio" que resume el perfil activo; el interruptor de reanudación
sigue siempre visible.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt` | modified | Contenido extraído a `SettingsContent` con `verticalScroll`; `SettingsActions` agrupa callbacks; cabecera de buffer plegable con `rememberSaveable` y resumen del perfil |
| `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/PlaybackSettingsTest.kt` | modified | Los tests despliegan la sección antes de interactuar; nuevos tests de estado plegado por defecto y de alternar expandido/plegado |
| `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/SettingsContentTest.kt` | added test | Compone `SettingsContent` con 320 dp de alto y verifica que se puede hacer scroll y pulsar "Gráficas" y "Acerca de" |
| `.specify/bugs/0043-jlizquierdo-20261031-configuracion-scroll-buffer-plegable/assessment.md` | added | Assessment de prerrequisito (no existía para esta regresión) |

## Diff Highlights (optional)

```kotlin
// SettingsScreen.kt
Column(Modifier.fillMaxSize()) {
    SectionHeader(title = "Configuración")
    SettingsContent(
        ui = ui,
        actions = SettingsActions(...),
        modifier = Modifier.weight(1f),
    )
}

// SettingsContent: el contenido inferior es el que se desplaza
Column(
    modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp),
) { /* tema, arranque, reproducción, gráficas, acerca de */ }
```

```kotlin
// PlaybackSettingsSection: plegado por defecto
var bufferExpanded by rememberSaveable { mutableStateOf(false) }
Row(
    Modifier
        .fillMaxWidth()
        .clickable { bufferExpanded = !bufferExpanded }
        .semantics { stateDescription = if (bufferExpanded) "Expandido" else "Plegado" }
        .padding(vertical = 8.dp),
) {
    Column(Modifier.weight(1f)) {
        Text("Buffer de audio")
        Text("Perfil actual: ${bufferProfile.label()}")
    }
    Icon(if (bufferExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
}
if (bufferExpanded) {
    BufferProfile.entries.forEach { /* radios existentes */ }
}
```

## Tests Added or Updated

- `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/PlaybackSettingsTest.kt::opcionesDeBufferPlegadasPorDefecto`
  — fija que los tres perfiles no existen en el árbol semántico al abrir la
  sección, que se muestra el resumen "Perfil actual: Equilibrado" y que el
  interruptor de reanudación sigue visible.
- `...::pulsarBufferDeAudioDespliegaYOcultaOpciones` — fija que la cabecera
  alterna entre mostrar y ocultar las opciones.
- `...::muestraLosTresPerfilesYElSwitchAlDesplegar` — conserva la cobertura
  original de los tres perfiles y el switch, ahora tras desplegar.
- `...::seleccionarMaximaEstabilidadInvocaCallback` — actualizado para desplegar
  antes de pulsar; sigue comprobando el callback de selección.
- `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/SettingsContentTest.kt::conPocoAltoSePuedeLlegarAGraficasYAcercaDe`
  — regresión del scroll: en un contenedor de 320 dp, `performScrollTo` alcanza
  ambas filas y sus clics disparan los callbacks.

## Local Verification

- Commands run:
  - `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` → **BUILD SUCCESSFUL**
  - `./gradlew testDebugUnitTest` → **414 tests, 0 failures, 0 errors**
  - `./gradlew detekt` → **BUILD SUCCESSFUL** (tras agrupar callbacks en `SettingsActions` para resolver `LongParameterList`)
  - `./gradlew ktlintCheck` → **BUILD SUCCESSFUL**
  - `./gradlew lintDebug` → **BUILD SUCCESSFUL**
  - `./gradlew assembleDebug assembleDebugAndroidTest` → **BUILD SUCCESSFUL**
- Manual checks: no ejecutados; `adb devices` no lista ningún emulador ni
  dispositivo conectado, por lo que los tests Compose instrumentados se
  compilaron pero no se ejecutaron.

## Deviations from Assessment

- Se extrajo el contenido a `SettingsContent` + `SettingsActions` para poder
  testear el scroll sin Hilt. El assessment proponía el `verticalScroll` y el
  plegado, pero no imponía esta extracción; no cambia comportamiento y sigue el
  patrón `*ScreenContent` ya usado en Favoritos y Estadísticas.
- No existía `assessment.md` para esta regresión; se creó uno nuevo con slug
  auto-generado antes de aplicar el fix para cumplir el prerrequisito del comando.

## Follow-ups

- Ejecutar `/speckit.bug.test slug=0043-jlizquierdo-20261031-configuracion-scroll-buffer-plegable`
  en emulador/dispositivo: `connectedDebugAndroidTest` y verificación manual de
  que se llega a "Gráficas" y "Acerca de" y de que el buffer aparece plegado.
- Actualizar el diagrama de `specs/0042-izquierdojl-20261031-audio-playback-recovery/contracts/playback-settings-ui.md`
  para reflejar que las opciones de buffer se muestran plegadas por defecto
  (siguen cumpliendo el límite de ≤3 toques).
- Sin cambios de dependencias, persistencia, red ni migraciones.
