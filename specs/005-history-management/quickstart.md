# Quickstart: Historial — lista, reproducción y gestión

**Date**: 2026-09-06 | **Feature**: 005-history-management

## Prerequisites

- Instancia TolochaRadio accesible con al menos una cuenta registrada
- La cuenta debe tener historial de reproducción (reproducir al menos 2 emisoras desde Explorar o Favoritos)
- App compilada con la feature de historial implementada

## Validation Scenarios

### VS1: Ver historial con datos

1. Iniciar sesión con una cuenta que tiene historial
2. Pulsar "Historial" en la barra inferior
3. **Expected**: Lista de emisoras con imagen, nombre, país/idioma y hora relativa ("hace N minutos/horas/días"), ordenadas de más reciente a más antigua. Tiempo de carga < 2 s.

### VS2: Estado vacío

1. Iniciar sesión con una cuenta nueva (sin historial)
2. Pulsar "Historial" en la barra inferior
3. **Expected**: Estado vacío con icono, mensaje "Todavía no has escuchado nada" y botón a Explorar.

### VS3: Reproducir desde historial

1. Abrir Historial con datos
2. Tocar una emisora (botón de reproducir o la tarjeta)
3. **Expected**: La emisora suena, el mini-reproductor aparece, la emisora pasa a primera posición en la lista. La reproducción no se interrumpe al seguir navegando.

### VS4: Eliminar emisora individual

1. Abrir Historial con datos
2. Pulsar el botón de eliminar (papelera) en una emisora
3. **Expected**: La emisora desaparece de la lista al instante. Al recargar (pull-to-refresh o reabrir), sigue sin aparecer.

### VS5: Limpiar todo el historial

1. Abrir Historial con datos
2. Pulsar "Limpiar"
3. **Expected**: Aparece diálogo "¿Limpiar todo el historial?" con Cancelar/Limpiar. Al confirmar, la lista se vacía y muestra estado vacío.

### VS6: Error de red con caché

1. Abrir Historial con datos (con red)
2. Activar modo avión
3. Cerrar y reabrir Historial
4. **Expected**: Se muestra la última lista conocida con indicador de "Sin conexión" y botón de reintentar.

### VS7: Sin sesión

1. Cerrar sesión (o usar cuenta sin sesión)
2. Pulsar "Historial" en la barra inferior
3. **Expected**: Redirección a pantalla de Login.

### VS8: Error del servidor

1. Simular error 500 del servidor (o usar instancia caída)
2. Abrir Historial
3. **Expected**: Mensaje de error en español con botón de Reintentar. Nunca pantalla en blanco ni crash.

## Test Commands

```bash
# Unit tests (ViewModel, UseCase, Repo, DTO serialization)
./gradlew :app:testDebugUnitTest

# Compose UI tests (History flow)
./gradlew :app:connectedDebugAndroidTest

# Lint + static analysis
./gradlew :app:detekt :app:ktlintCheck :app:lintDebug
```

## Related Artifacts

- [Spec](spec.md) — Feature specification
- [Data Model](data-model.md) — Entities, fields, relationships
- [contracts/HistoryApi.kt](contracts/HistoryApi.kt) — Retrofit interface
- [research.md](research.md) — Technical decisions
