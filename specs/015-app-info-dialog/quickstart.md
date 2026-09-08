# Quickstart: Diálogo de información de la aplicación

**Date**: 2026-09-08
**Feature**: 015-app-info-dialog

## Prerrequisitos

- Android Studio instalado
- Dispositivo Android o emulador con minSdk 26
- App TolochaRadio compilada y ejecutable

## Validación manual

### Escenario 1: Abrir el diálogo de información

1. Abre la app y navega a Configuración
2. Desplázate hasta el final de la pantalla
3. Pulsa "Acerca de"
4. **Resultado esperado**: Se abre un AlertDialog mostrando:
   - Nombre de la app (Tolocha Radio)
   - Número de versión (ej: "1.0")
   - Desarrollador
   - Licencia
   - Enlace "Ver repositorio"

### Escenario 2: Acceder al repositorio

1. Con el diálogo abierto, pulsa "Ver repositorio"
2. **Resultado esperado**: Se abre el navegador con `https://github.com/izquierdojl/tolocharadio-android`
3. Vuelve a la app (el diálogo debe seguir abierto)

### Escenario 3: Cerrar el diálogo

1. Con el diálogo abierto, pulsa "Cerrar"
2. **Resultado esperado**: El diálogo se cierra, vuelve a Configuración
3. Pulsa "Acerca de" nuevamente
4. Pulsa fuera del diálogo
5. **Resultado esperado**: El diálogo se cierra

### Escenario 4: Rotación de dispositivo

1. Abre el diálogo de información
2. Rota el dispositivo 90 grados
3. **Resultado esperado**: El diálogo permanece abierto con la misma información

## Validación de edge cases

### Versión no disponible

1. Si `versionName` no está configurado en el build
2. **Resultado esperado**: Se muestra "1.0" como fallback

### Sin navegador instalado

1. En un dispositivo sin navegador (raro, pero posible en emuladores minimalistas)
2. Pulsa "Ver repositorio"
3. **Resultado esperado**: No crashea; el enlace permanece visible sin acción

## Tests unitarios

Ejecutar los tests de settings:

```bash
./gradlew testDebugUnitTest --tests "*Settings*"
```

Tests esperados:
- `SettingsViewModelTest`: showAppInfoDialog, dismissAppInfoDialog, datos correctos
