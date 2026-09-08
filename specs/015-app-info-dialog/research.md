# Research: Diálogo de información de la aplicación

**Date**: 2026-09-08
**Feature**: 015-app-info-dialog

## Decisión 1: Tipo de componente para el diálogo

**Decisión**: `AlertDialog` de Material3

**Razón**: Es el componente estándar de Material3 para diámodales informativos simples. El proyecto ya usa Material3 en toda la app. No requiere dependencias adicionales.

**Alternativas consideradas**:
- `BottomSheet`: Más adecuado para contenido extenso o interacciones; excesivo para información estática.
- `Dialog` personalizado: Más control visual pero innecesario; AlertDialog ya soporta título, contenido y botones.
- Nueva pantalla/fragment: Sobredimensionado para contenido estático breve.

## Decisión 2: Fuente de datos de la app

**Decisión**: `BuildConfig` + constantes en el ViewModel

**Razón**: El proyecto ya usa `BuildConfig` para `TOLOCHA_BASE_URL` y `DEBUG`. Los campos `versionName` y `versionCode` están configurados en `build.gradle.kts`. El nombre de la app se obtiene de `context.getString(R.string.app_name)` o se hardcodea. La URL del repositorio y datos del desarrollador son constantes.

**Alternativas consideradas**:
- Archivo de configuración JSON: Innecesario para datos estáticos que no cambian por instancia.
- Obtener del backend: La spec indica que son datos estáticos sin comunicación backend.
- `PackageInfo` de Android: Accede a `versionName`/`versionCode` del manifest; equivalente a BuildConfig pero con más código.

## Decisión 3: Disparador en la pantalla de Configuración

**Decisión**: Fila con icono y texto "Acerca de" al final de la sección de Configuración

**Razón**: Patrón estándar en apps Android. El SettingsScreen actual tiene tema, pantalla de arranque y logout. "Acerca de" se coloca al final como convención UX.

**Alternativas consideradas**:
- En el menú de overflow de la barra superior: Menos discoverable; los usuarios buscan info en Configuración.
- Sección expandible: Sobrado para un simple disparador de diálogo.
- Footer con versión pequeña: Común pero no resuelve el requisito de mostrar info completa en diálogo.

## Decisión 4: Datos a mostrar en el diálogo

**Decisión**: Nombre de app, versión, URL del repositorio (clicable), desarrollador, licencia

**Razón**: Alineado con FR-002 a FR-007 de la spec. La URL del repositorio es `https://github.com/izquierdojl/tolocharadio-android` (confirmado en clarifications).

**Alternativas consideradas**:
- Añadir logo de la app: Posible pero no solicitado; se puede añadir como mejora futura.
- Añadir changelog: Fuera de alcance; requeriría lógica adicional.
- Añadir feedback/soporte: No solicitado en la spec.
