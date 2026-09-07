# Quickstart: Bottom Nav Icons Only + Section Titles

**Feature**: 012-bottom-nav-icons-only | **Date**: 2026-09-07

## Prerrequisitos

- Android Studio con emulador o dispositivo físico con API 26+
- Rama `012-bottom-nav-icons-only` activa
- App compilada e instalada

## Validación

### Escenario 1: Barra de navegación solo con iconos

1. Abrir la app y autenticarse.
2. Observar la barra de navegación inferior.
3. **Expected**: 5 iconos (🔍, ❤️, 🕘, 📻, ⚙️) sin texto debajo.
4. Pulsar cada icono y verificar que navega a la sección correcta.
5. Mantener pulsado cada icono y verificar que aparece un tooltip con el nombre.

### Escenario 2: Títulos de sección consistentes

1. Navegar a **Explorar** → verificar título "Explorar" visible debajo de la TopAppBar.
2. Navegar a **Favoritos** → verificar título "Tus favoritos" visible.
3. Navegar a **Historial** → verificar título "Tu historial" con subtítulo "Lo último que has escuchado."
4. Navegar a **Mis emisoras** → verificar título "Mis emisoras" con subtítulo.
5. Navegar a **Configuración** → verificar título "Configuración" (sin subtítulo).
6. **Expected**: Todos los títulos usan el mismo estilo (`headlineSmall`) y el mismo padding.

### Escenario 3: Padding consistente

1. Navegar a cada sección y observar el espacio entre la TopAppBar y el título.
2. **Expected**: El espacio es visualmente idéntico en las 5 secciones (~8dp vertical).

### Escenario 4: Sin regresiones

1. Verificar que el mini-player sigue apareciendo sobre la barra de navegación.
2. Verificar que el estado seleccionado (icono activo) funciona correctamente.
3. Verificar que TalkBack lee el nombre de cada sección al navegar.

## Comandos de verificación

```bash
# Compilación
./gradlew assembleDebug

# Tests unitarios
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug
```
