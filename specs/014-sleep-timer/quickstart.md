# Quickstart: Temporizador de apagado (Sleep Timer)

**Date**: 2026-09-08
**Feature**: 014-sleep-timer

## Prerrequisitos

- Android Studio instalado
- Dispositivo Android o emulador con minSdk 26
- Instancia de TolochaRadio accesible (configurada en la app)

## Validación manual

### Escenario 1: Activar temporizador

1. Abre la app y reproduce una emisora
2. Pulsa el icono del reloj en la barra superior
3. Se despliega un menú con opciones: 15, 30, 45, 60, 90 minutos
4. Selecciona "15 minutos"
5. **Resultado esperado**: El icono cambia a reloj activo con badge "15 min"
6. Espera 15 minutos (o usa un timer de debug más corto)
7. **Resultado esperado**: La reproducción se detiene silenciosamente

### Escenario 2: Ver tiempo restante

1. Con el temporizador activo, pulsa el botón del reloj
2. **Resultado esperado**: Se muestra "Quedan 15 minutos" (solo minutos, sin segundos) y opción "Cancelar temporizador"

### Escenario 3: Cancelar temporizador

1. Con el temporizador activo, pulsa el botón del reloj
2. Selecciona "Cancelar temporizador"
3. **Resultado esperado**: El icono vuelve al estado normal (sin badge), la reproducción continúa

### Escenario 4: Reemplazar temporizador

1. Activa un temporizador de 30 minutos
2. Pulsa el botón del reloj nuevamente
3. Selecciona "15 minutos"
4. **Resultado esperado**: El badge cambia a "15 min", el timer anterior se cancela

### Escenario 5: Navegar con temporizador activo

1. Activa un temporizador desde Explorar
2. Navega a Favoritos, Historial, Mis emisoras
3. **Resultado esperado**: El botón del reloj sigue mostrando el badge activo en todas las pantallas

## Validación de edge cases

### Reproducción pausada al expirar

1. Activa un temporizador corto (debug)
2. Pausa la reproducción
3. Espera a que expire
4. **Resultado esperado**: El temporizador se desactiva, al reanudar no hay timer activo

### Cambio de emisora

1. Activa un temporizador
2. Cambia a otra emisora
3. **Resultado esperado**: El temporizador sigue activo con la nueva emisora

### Cierre de app

1. Activa un temporizador
2. Cierra la app completamente (swipe en recents)
3. Reabre la app
4. **Resultado esperado**: No hay temporizador activo

## Tests unitarios

Ejecutar los tests del dominio:

```bash
./gradlew testDebugUnitTest --tests "*SleepTimer*"
```

Tests esperados:
- `SleepTimerUseCaseTest`: inicio, cancelación, expiración, reemplazo
- `SleepTimerViewModelTest`: integración con UseCase y PlayerViewModel
