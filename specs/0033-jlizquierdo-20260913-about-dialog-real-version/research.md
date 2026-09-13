# Research: Diálogo "Acerca de" con versión real y detalles ampliados

**Feature**: 0033-jlizquierdo-20260913-about-dialog-real-version
**Date**: 2026-09-13

Este documento resuelve las incógnitas técnicas del plan. No quedan `NEEDS CLARIFICATION`.

## R1 — Fuente de la versión real de la aplicación

- **Decision**: Leer `BuildConfig.VERSION_NAME` (nombre de versión) y `BuildConfig.VERSION_CODE` (nº de compilación). `app/build.gradle.kts` ya calcula `versionName`/`versionCode` desde `RELEASE_VERSION`/`VERSION_CODE`, `-PversionName`/`-PversionCode`, `GITHUB_REF_NAME` (tag `vX.Y.Z`) o el valor por defecto `1.0`, y `buildConfig = true` ya está activado. En `SettingsViewModel`, sustituir el valor fijo `AppInfo(version = "1.0")` por el valor derivado del build.
- **Rationale**: Es la única fuente fiable del metadato real en tiempo de ejecución; no requiere red ni persistencia. Corrige la causa raíz (constante `"1.0"` en `AppInfo`).
- **Alternatives considered**:
  - Consultar la versión al backend (`GET /config`): rechazado — el backend no expone la versión del cliente y viola el requisito "sin backend".
  - Leer el `PackageManager` con `Context`: rechazado — el ViewModel no debe referenciar `Context` de Activity (Principio I); `BuildConfig` es un dato estático sin `Context`.

## R2 — Metadatos del build inyectables (testabilidad Red-Green)

- **Decision**: Introducir un value object `AppBuildInfo` (`appName`, `versionName`, `versionCode`, `applicationId`, `buildType`, `repositoryUrl`, `developer`, `license`) y proveerlo con Hilt desde `BuildConfig` (nuevo `di/AppInfoModule.kt`). `SettingsViewModel` lo recibe por constructor. `buildType` se deriva de `BuildConfig.DEBUG` ("Depuración"/"Publicación").
- **Rationale**: Con `BuildConfig` leído directamente, en tests locales `VERSION_NAME` siempre vale `"1.0"`, por lo que un test Red-Green no podría distinguir el valor fijo del derivado. Inyectando `AppBuildInfo` se puede simular `versionName = "2.3.1"` y demostrar que el diálogo refleja el valor inyectado. Sin `Context`, sin dependencias nuevas y sin interfaz adicional (es un `data class` provisto por DI).
- **Alternatives considered**:
  - Leer `BuildConfig` directo en el ViewModel: rechazado — no permite un test Rojo significativo y acopla el ViewModel a campos generados.
  - Interfaz `AppInfoProvider` + implementación: rechazada por YAGNI (un solo consumidor real); el value object + `@Provides` cubre testabilidad con menos indirección.
  - `buildConfigField` adicionales: innecesario; `VERSION_NAME`, `VERSION_CODE`, `APPLICATION_ID` y `DEBUG` ya se generan.

## R3 — Configuración activa (tema, pantalla de arranque, alias del servidor)

- **Decision**: Reutilizar los flujos ya inyectados de `InstancePrefs` (`themeMode`, `startScreen`) y añadir `GetServersUseCase()` para derivar el `alias` del servidor con `isActive = true`. Se exponen en `SettingsUi` (p. ej. `themeMode`, `startScreen`, `activeServerAlias: String?`) y `showAppInfoDialog()` compone `AppInfo` con esos valores actuales.
- **Rationale**: Son los mismos datos que ya usa Configuración; `GetServersUseCase` es la vía de dominio existente (no se accede al `ServerDao` desde la UI). Evita duplicar lógica y respeta MVVM+Clean.
- **Alternatives considered**:
  - `ServerRepository.getActive()` suspend dentro de `showAppInfoDialog()`: viable, pero introduce asincronía en el clic y un estado intermedio; el `Flow` continuo es más simple y reactivo.
  - Guardar los datos en `AppInfo` de forma estática: rechazado — la configuración cambia en runtime y debe reflejar el valor vigente al abrir.

## R4 — Etiquetas legibles de tema y pantalla de arranque

- **Decision**: Mapear `ThemeMode` → "Sistema"/"Claro"/"Oscuro" y `StartScreen` → "Explorar"/"Favoritos"/"Historial" mediante funciones puras de extensión en `feature/settings` (testables con JUnit). Reutilizan las etiquetas ya presentes en `SettingsScreen`.
- **Rationale**: La UI debe mostrar texto humano, no el nombre del enum; centralizar el mapeo evita duplicación y permite testearlo sin Compose.
- **Alternatives considered**:
  - `when` inline en el composable: rechazado — no testeable de forma pura y duplica las etiquetas ya existentes.
  - Añadir `label` a los enums de `core/ui`: rechazado — cambiaría enums compartidos por una necesidad local (YAGNI).

## R5 — Snackbar visible con un diálogo modal abierto

- **Decision**: Sustituir `AlertDialog` por una ventana `Dialog` a pantalla completa (`DialogProperties(usePlatformDefaultWidth = false)`) que contenga scrim + tarjeta Material 3 + un `SnackbarHost` anclado abajo. El `SnackbarHostState` lo posee `SettingsScreen` y se coloca dentro de la ventana del diálogo, de modo que el aviso aparece por encima del scrim y el diálogo permanece abierto.
- **Rationale**: `AlertDialog` vive en una ventana separada cuya capa de atenuación cubre la pantalla; un `SnackbarHost` del `Scaffold` subyacente quedaría oculto/atenuado por el scrim de la ventana del diálogo. Alojar el host dentro de la ventana modal garantiza que el snackbar se vea en la parte inferior, tal como pidió el usuario (Q3), sin cerrar el diálogo.
- **Alternatives considered**:
  - `AlertDialog` + snackbar del `TolochaNavGraph`: rechazado — el aviso queda tras el scrim de la ventana del diálogo (no visible).
  - Cerrar el diálogo al copiar y mostrar el snackbar debajo: rechazado — la clarificación exige no cerrar el diálogo.
  - `Toast` del sistema: rechazado explícitamente por el usuario en la clarificación.
  - Snackbar dentro del contenido de `AlertDialog`: rechazado — quedaría en el borde inferior de la tarjeta, no en la parte inferior de la pantalla, y `AlertDialog` no expone un slot para ello.

## R6 — Copiado al portapapeles y mensajes de resultado (one-shot)

- **Decision**: En Compose, `LocalClipboardManager.current.setText(AnnotatedString(text))`. El texto se genera con una función pura `AppInfo.toClipboardText()` (pares "Etiqueta: valor" en líneas, sin credenciales). Tras intentar la copia, el composable llama a `viewModel.onCopyResult(success)`; el ViewModel emite por un `MutableSharedFlow<String>` (`messages`, patrón ya usado en `FavoritesViewModel`/`HistoryViewModel`) el mensaje "Información copiada" o "No se pudo copiar la información". `SettingsScreen` recoge `messages` y llama a `snackbar.showSnackbar(...)`.
- **Rationale**: Mantiene la lógica de mensajes en el ViewModel (testeable) y la operación de sistema en la UI. El `SharedFlow` evita repetir el aviso en recomposiciones. No expone credenciales ni PII (FR-009/SC-006).
- **Alternatives considered**:
  - Copiar desde el ViewModel: rechazado — requeriría `Context` (prohibido por el Principio I).
  - Poner el texto en `SettingsUi` y observarlo como evento: más complejo que `SharedFlow`; se descarta por simplicidad.
  - `ClipboardManager` de Android directo: rechazado — `LocalClipboardManager` es la vía Compose idiomática ya usada en `PlayerUi`.
