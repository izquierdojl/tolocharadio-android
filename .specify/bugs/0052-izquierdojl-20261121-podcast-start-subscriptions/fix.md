# Bug Fix: Sección inicial de Podcasts y pantalla de arranque de podcast

- **Slug**: 0052-izquierdojl-20261121-podcast-start-subscriptions
- **Fixed**: 2026-11-21
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

El modo de arranque ahora se deriva de la **sección inicial** configurada
(Radio/Podcasts) en lugar del último modo persistido, y la pantalla de arranque
de podcast se aplica tanto al iniciar como al cambiar a Podcasts. El selector de
Configuración deja de ser un desplegable y usa botones segmentados en dos filas,
como el de Radio.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/ModeViewModel.kt` | modified | El modo inicial sale de `defaultSection` cuando las capacidades están resueltas; el `contentMode` persistido ya no pisa la decisión de arranque. |
| `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` | modified | El arranque espera a `modeUi.loading == false`, se aplica una sola vez y no puede rebotar a `EXPLORE`. Añadida la puerta `shouldApplyStartDestination`. |
| `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/Routes.kt` | modified | `routeAfterModeChange` acepta la ruta de arranque de podcast en vez de fijar `PODCAST_HOME`. |
| `app/src/main/java/com/izquierdojl/tolocharadio/MainActivity.kt` | modified | No compone el grafo hasta que `startScreen`, `podcastStartScreen` y `defaultSection` han emitido; evita resolver el arranque con valores por defecto. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt` | modified | `PodcastStartSettings` pasa de `DropdownMenu` a dos `SingleChoiceSegmentedButtonRow` (3 + 2). Texto de ayuda actualizado. |
| `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/SettingsPodcastStartTest.kt` | modified | Prueba el selector segmentado, el estado seleccionado y la llamada con `SUBSCRIPTIONS`. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/ModeViewModelTest.kt` | modified | Cubre la sección inicial como fuente del modo de arranque y el fallback sin podcasts. |
| `app/src/test/java/com/izquierdojl/tolocharadio/core/ui/navigation/StartDestinationTest.kt` | modified | Cubre el destino de podcast configurado al cambiar de modo. |
| `app/src/test/java/com/izquierdojl/tolocharadio/core/ui/navigation/StartDestinationGateTest.kt` | added | Fija que el arranque solo se aplica una vez, en `HOME` y con capacidades resueltas. |
| `specs/0048-izquierdojl-20261119-podcast-ux-refinements/contracts/ui-contracts.md` | modified | El contrato ya no describe un desplegable: documenta los segmentados 3 + 2 y la semántica de selección. |

## Diff Highlights

Modo de arranque derivado de la sección inicial, en una sola actualización
atómica junto a `loading`:

```kotlin
val section = prefs.defaultSection.first()
val enabled = podcastCapabilitiesEnabled()
_ui.value =
    _ui.value.copy(
        mode = if (enabled) section else ContentMode.RADIO,
        defaultSection = section,
        podcastsEnabled = enabled,
        loading = false,
    )
```

El cambio de modo usa la pantalla configurada:

```kotlin
podcastMode && currentRoute in RADIO_CONTENT_ROUTES -> podcastStartRoute
```

El selector de Configuración, con el mismo componente que Radio:

```kotlin
val rows = listOf(
    PodcastStartScreen.entries.take(PODCAST_START_FIRST_ROW),
    PodcastStartScreen.entries.drop(PODCAST_START_FIRST_ROW),
)
rows.forEach { row ->
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        row.forEachIndexed { index, screen ->
            SegmentedButton(
                selected = current == screen,
                onClick = { onChange(screen) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = row.size),
                label = { Text(screen.label()) },
            )
        }
    }
}
```

## Tests Added or Updated

- `ModeViewModelTest::la seccion inicial decide el modo de arranque` — `defaultSection = PODCAST` gana a `contentMode = RADIO`.
- `ModeViewModelTest::el modo persistido no pisa la seccion inicial` — `contentMode = PODCAST` no gana a `defaultSection = RADIO`.
- `ModeViewModelTest::sin podcasts la seccion inicial podcast cae a radio` — el fallback se mantiene.
- `ModeViewModelTest::cambiar la seccion inicial no cambia el modo en caliente` — el cambio se aplica en el siguiente arranque.
- `StartDestinationTest::routeAfterModeChange usa la pantalla de arranque de podcast` — `EXPLORE` + modo podcast → `PODCAST_SUBSCRIPTIONS`; sin argumento → `PODCAST_HOME`.
- `StartDestinationGateTest::aplica el arranque solo en la ruta inicial y sin capacidades pendientes` — no navega en `EXPLORE`, con `modeLoading`, con otra puerta o si ya se aplicó.
- `SettingsPodcastStartTest::elSelectorDePodcastMuestraLasCincoOpcionesYSeleccionaSuscripciones` — cinco etiquetas visibles, "Inicio" seleccionado y clic en "Suscripciones" entrega `SUBSCRIPTIONS`.

## Local Verification

- Commands run: `./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.feature.podcasts.ModeViewModelTest" --tests "com.izquierdojl.tolocharadio.core.ui.navigation.StartDestinationTest" --tests "com.izquierdojl.tolocharadio.feature.settings.SettingsLabelsTest"` → `BUILD SUCCESSFUL`.
- Commands run: `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug compileDebugAndroidTestKotlin` → `BUILD SUCCESSFUL` (gates de CI en verde).
- Commands run: `./gradlew compileDebugAndroidTestKotlin` → compila el test instrumentado; no se ejecutó.
- Manual checks: ninguna en dispositivo. `adb devices` no lista emulador ni teléfono conectado, así que `connectedDebugAndroidTest` queda pendiente.
- Nota: detekt marcó inicialmente `MagicNumber` por `3` (resuelto con `PODCAST_START_FIRST_ROW`) y `ComplexCondition` en `MainActivity` (resuelto separando las guardas); ambas incidencias están corregidas.

## Deviations from Assessment

- **Archivo adicional `MainActivity.kt`**: el assessment no lo listaba. Fue necesario para que el grafo no se componga antes de la primera emisión de las tres preferencias; sin ello persistía una carrera entre DataStore y `/config` que podía volver a abrir Inicio o Radio.
- **Helper `shouldApplyStartDestination` + `StartDestinationGateTest`**: el assessment pedía un test del arranque; se extrajo la condición pura para poder cubrirla sin emulador.
- **Alcance funcional**: además del arranque, el cambio Radio → Podcasts también usa la pantalla configurada (propuesta 3 del assessment). El texto de ayuda y el contrato de UI se ajustaron a ese comportamiento.
- No se encontró ninguna evidencia de que el diagnóstico del assessment fuera incorrecto.

## Follow-ups

- Ejecutar `./gradlew connectedDebugAndroidTest` con un emulador o dispositivo para validar `SettingsPodcastStartTest` (el test instrumentado compila, pero no se ha ejecutado).
- Revisar si `contentMode` sigue siendo necesario: ahora el arranque no lo lee y solo se usa para cambios en caliente; conviene documentarlo o retirarlo en una limpieza posterior.
- Verificar con TalkBack y fuente al 200 % las dos filas de botones segmentados.
- Añadir, si se quiere cobertura de navegación real, un test instrumentado que monte `TolochaNavGraph` con un `NavController` de prueba y compruebe que no hay rebote a `EXPLORE`.
