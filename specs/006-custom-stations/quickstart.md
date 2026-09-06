# Quickstart: Emisoras personalizadas — Mis emisoras

**Date**: 2026-09-06 | **Feature**: 006-custom-stations

## Prerequisites

- Instancia TolochaRadio accesible con al menos una cuenta registrada
- Una URL de stream pública válida para pruebas (p. ej. cualquier stream `https://` funcional)
- App compilada con la feature de emisoras personalizadas implementada

## Validation Scenarios

### VS1: Ver lista con datos

1. Iniciar sesión con una cuenta que tiene emisoras personalizadas (creadas desde la web o la app)
2. Pulsar "Mis emisoras" en la barra inferior
3. **Expected**: Lista de emisoras con nombre y emblema de TolochaRadio, formulario Nombre + URL visible arriba. Tiempo de carga < 2 s.

### VS2: Estado vacío

1. Iniciar sesión con una cuenta nueva (sin personalizadas)
2. Pulsar "Mis emisoras" en la barra inferior
3. **Expected**: Estado vacío con mensaje "Aún no tienes emisoras personalizadas" y explicación de usar el formulario superior.

### VS3: Añadir emisora válida

1. Abrir Mis emisoras
2. Escribir nombre "Radio Sierra" y una URL de stream `https://` válida, pulsar Añadir
3. **Expected**: La emisora aparece en la lista al instante, el formulario se vacía y hay confirmación ("Emisora personalizada añadida"). Al recargar, sigue ahí.

### VS4: Validación cliente (sin red)

1. Abrir Mis emisoras
2. Pulsar Añadir con nombre vacío → **Expected**: aviso "Escribe un nombre para la emisora", sin llamada al servidor
3. Escribir nombre y URL "no-es-una-url" o "ftp://..." → **Expected**: aviso de URL no válida / debe empezar por http(s), sin llamada al servidor

### VS5: Reproducir personalizada

1. Abrir Mis emisoras con datos
2. Tocar una emisora para reproducir
3. **Expected**: Suena vía proxy, el mini-reproductor aparece con su nombre, y el audio continúa al navegar a Explorar/Historial.

### VS6: Eliminar emisora

1. Abrir Mis emisoras con datos
2. Pulsar el botón de eliminar (papelera) en una emisora
3. **Expected**: Desaparece al instante con confirmación ("Emisora eliminada"). Al recargar, sigue sin aparecer. Si era favorita, ya no aparece en Favoritos.

### VS7: Error de red con caché

1. Abrir Mis emisoras con datos (con red)
2. Activar modo avión
3. Cerrar y reabrir Mis emisoras
4. **Expected**: Se muestra la última lista conocida con indicador de "Sin conexión" y botón de reintentar. El formulario sigue visible.

### VS8: Sin sesión

1. Cerrar sesión (o usar cuenta sin sesión)
2. Pulsar "Mis emisoras" en la barra inferior
3. **Expected**: Redirección a pantalla de Login.

### VS9: Error del servidor al añadir

1. Provocar un 422 (p. ej. nombre de más de 256 caracteres si la instancia lo valida)
2. Pulsar Añadir
3. **Expected**: La lista no cambia y se muestra el motivo en español (detalle por campo).

## Test Commands

```bash
# Unit tests (ViewModel, UseCase de validación, Repo, DTO serialization)
./gradlew :app:testDebugUnitTest

# Compose UI tests (flujo Mis emisoras)
./gradlew :app:connectedDebugAndroidTest

# Lint + static analysis
./gradlew :app:detekt :app:ktlintCheck :app:lintDebug
```

## Related Artifacts

- [Spec](spec.md) — Feature specification
- [Data Model](data-model.md) — Entities, fields, relationships
- [contracts/CustomStationsApi.kt](contracts/CustomStationsApi.kt) — Retrofit interface
- [research.md](research.md) — Technical decisions
