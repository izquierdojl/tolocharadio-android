# Bug Assessment: Configuración sin desplazamiento tras añadir el buffer

- **Slug**: 0043-jlizquierdo-20261031-configuracion-scroll-buffer-plegable
- **Created**: 2026-10-31
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> Al añadir las opciones del buffer en la pantalla de configuración, se ha bloqueado el desplazamiento vertical y no se puede acceder ni a gráficas ni a acerca de. Soluciona el problema y de paso que las opciones de buffer aparezcan plegadas por defecto para que no ocupen tanto espacio.

## Symptom

En Configuración, la nueva sección "Reproducción" (tres perfiles de buffer con descripción y el interruptor de reanudación) alarga el contenido más allá del alto disponible. La pantalla usa un `Column` sin `verticalScroll`, por lo que el contenido inferior ("Gráficas" y "Acerca de") queda recortado y es inaccesible. Se espera poder desplazarse por toda la pantalla y que las opciones de buffer estén plegadas inicialmente para reducir la altura.

## Reproduction

1. Abrir la app con un servidor configurado.
2. Ir a la pestaña "Configuración".
3. Observar que la sección "Reproducción" muestra los tres perfiles de buffer desplegados.
4. Intentar desplazarse hacia abajo: la pantalla no responde al scroll.
5. Comprobar que "Gráficas" y "Acerca de" quedan fuera del área visible y no se puede llegar a ellas.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt:55` — `Column(Modifier.fillMaxSize())` sin modificador de scroll; el contenido que excede el alto se recorta.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt:57` — segundo `Column` con padding horizontal que aloja todo el contenido; tampoco tiene `verticalScroll`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt:114-120` — inserción de `PlaybackSettingsSection`, que añade ~200 dp de altura y provoca el desbordamiento.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt:127-137` — filas "Gráficas" y "Acerca de" al final del contenido; son las primeras víctimas del recorte.
- `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/PlaybackSettingsTest.kt` — test existente que asume las opciones siempre visibles; debe adaptarse al estado plegado.

## Root Cause Hypothesis

Confianza: **high**.

`SettingsScreen` organiza todo el contenido en un `Column` con `fillMaxSize` y sin `verticalScroll`. Antes de la spec 0042 el contenido cabía en pantalla; la nueva sección de reproducción (tres filas de radio con etiqueta y descripción, más el interruptor) incrementa la altura lo suficiente para desbordar en alturas habituales. Al no existir un contenedor desplazable, Compose mide el `Column` con el alto máximo disponible y recorta el excedente, dejando "Gráficas" y "Acerca de" fuera de alcance. No es un fallo de la sección de reproducción en sí, sino de la falta de scroll de la pantalla.

## Proposed Remediation

**Preferred**: mantener el `SectionHeader` fijo y envolver el contenido inferior en un `Column` con `Modifier.weight(1f).verticalScroll(rememberScrollState())` (o aplicar `verticalScroll` al `Column` de contenido), de modo que toda la pantalla pueda desplazarse y las acciones finales sean alcanzables. Además, convertir el bloque de perfiles de buffer en una sección plegable: una fila cabecera "Buffer de audio" clicable, con el perfil actual como resumen y un icono de expansión, y las tres opciones dentro de un `if (expanded)`/`AnimatedVisibility` controlado por `rememberSaveable { mutableStateOf(false) }`. El interruptor "Reanudar al recuperar conexión" permanece visible. Así el estado inicial es compacto y el usuario sigue llegando a las opciones en dos toques (desplegar + elegir), cumpliendo el contrato de la spec 0042 (≤3 toques).

**Alternatives** (optional):
- Plegar toda la sección "Reproducción" (incluido el interruptor). Ahorra más altura, pero oculta un ajuste que antes estaba visible y complica el acceso; el reporte pide plegar las opciones de buffer, no el interruptor.
- Reducir tipografías/descripciones o eliminar el interruptor. Cambia el contrato visual de la spec 0042 y no resuelve el scroll en pantallas pequeñas.
- Usar `LazyColumn`. Más coste de migración y riesgo de perder el `SectionHeader` fijo; `verticalScroll` es suficiente para un contenido estático.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt`
- `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/PlaybackSettingsTest.kt`

**Tests to add or update**:
- Test Compose: las opciones de buffer no están visibles por defecto y aparecen tras pulsar la cabecera "Buffer de audio".
- Test Compose: el resumen de la cabecera refleja el perfil seleccionado.
- Actualizar los tests existentes de selección y switch para desplegar primero la sección.
- Verificación manual/instrumentada: en una pantalla con altura limitada, hacer scroll hasta "Gráficas" y "Acerca de" y comprobar que ambas filas responden.
- Gates obligatorios: `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug` (y `assembleDebugAndroidTest` para compilar los tests de UI).

## Risks & Considerations

- El scroll debe convivir con el `Scaffold` externo y la barra inferior; verificar que el último elemento no queda tapado por la `NavigationBar`/mini-player.
- El estado plegado con `rememberSaveable` sobrevive a rotaciones; al salir y volver a Configuración debe reiniciarse a plegado.
- La cabecera plegable necesita semántica accesible (estado expandido/plegado y etiqueta clara) para no degradar TalkBack.
- Cambio puramente de UI: sin migraciones, red, credenciales ni persistencia nueva.

## Open Questions

- Ninguna bloqueante. El reporte no indica dispositivo concreto; el fallo se reproduce por altura de contenido y se corrige con scroll independientemente de la pantalla.
