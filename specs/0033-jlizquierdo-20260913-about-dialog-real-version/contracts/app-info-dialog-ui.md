# Contrato: UI del diálogo "Acerca de"

**Feature**: 0033-jlizquierdo-20260913-about-dialog-real-version
**Tipo**: Contrato de UI Compose (Android). Define la superficie observable por el usuario y por los tests de UI.

## Componente

```kotlin
@Composable
fun AppInfoDialog(
    info: AppInfo,
    onDismiss: () -> Unit,
    onCopyResult: (Boolean) -> Unit,
    snackbar: SnackbarHostState,
)
```

- Se muestra solo cuando `AppInfoUiState` es `Showing`.
- Implementación: ventana `Dialog` a pantalla completa (`usePlatformDefaultWidth = false`) con scrim, tarjeta Material 3 y `SnackbarHost` anclado abajo.
- Sin `Context` de Activity en el ViewModel; el portapapeles se maneja en el composable.

## Contenido mostrado

| Sección | Dato | Origen |
|---------|------|--------|
| Cabecera | Nombre de la app | `AppInfo.appName` |
| Compilación | Versión | `AppInfo.version` (real) |
| Compilación | Número de compilación | `AppInfo.versionCode` |
| Compilación | Identificador de la aplicación | `AppInfo.applicationId` |
| Compilación | Tipo de build | `AppInfo.buildType` |
| Configuración | Tema | `AppInfo.theme` |
| Configuración | Pantalla de arranque | `AppInfo.startScreen` |
| Configuración | Servidor activo | `AppInfo.activeServerAlias` (si no es null) |
| Proyecto | Desarrollador | `AppInfo.developer` |
| Proyecto | Licencia | `AppInfo.license` |
| Proyecto | Repositorio (enlace) | `AppInfo.repositoryUrl` |

## Acciones del usuario

| Acción | Comportamiento |
|--------|----------------|
| Pulsar "Copiar información" | Copia `AppInfo.toClipboardText()` al portapapeles; llama a `onCopyResult(true)`; aparece snackbar "Información copiada" |
| Fallo al copiar | `runCatching { setText }`; si lanza, llama a `onCopyResult(false)` y muestra "No se pudo copiar la información" (ruta defensiva: en la práctica `setText` no suele lanzar) |
| Pulsar "Cerrar" | `onDismiss()` |
| Pulsar el scrim / atrás | `onDismiss()` |
| Pulsar "Ver repositorio" | Intenta abrir `repositoryUrl`; si falla, silencio (sin crash) |

## Semántica y accesibilidad

| Elemento | `contentDescription` |
|----------|----------------------|
| Acción copiar | "Copiar información de la aplicación" |
| Enlace repositorio | "Enlace al repositorio del código fuente" |
| Botón cerrar | "Cerrar diálogo de información" |

- El snackbar no roba el foco ni cierra el diálogo (SC-007).
- Todos los textos visibles en español.
- Contenido desplazable si excede la pantalla (Edge Case de la spec).

## Estados

```text
Hidden  ──► (no se compone el diálogo)
Showing ──► diálogo visible con info; snackbar disponible para el resultado de copia
```

## No objetivos

- No edita configuración (solo lectura).
- No consulta red ni backend.
- No muestra credenciales, tokens ni datos personales.
