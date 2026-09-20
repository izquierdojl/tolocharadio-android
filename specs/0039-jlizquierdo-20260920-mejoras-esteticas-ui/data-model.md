# Data Model: Mejoras estéticas UI

**Feature**: `0039-jlizquierdo-20260920-mejoras-esteticas-ui` | **Date**: 2026-09-20

Sin entidades nuevas ni migraciones. Esta mejora solo presenta datos ya existentes con otra disposición visual.

## Entidades reutilizadas (sin cambios)

- **Emisora** (ya existe en DTOs de catálogo/favoritas/historial): se lee `name` (cabeceras y ficha) y `url` (enlace real para compartir/copiar). Regla: `url.trim().isEmpty()` ⇒ enlace no disponible (ver `resolveCopyLink`).
- **Orden personalizado de favoritas**: permutación de ids ya gestionada por `moveItem`/`commitOrder`; el arrastre la conserva, el menú eliminado no la tocaba directamente.

## Parámetros de presentación (constantes de UI, en español)

| Superficie | Título | Subtítulo |
|------------|--------|-----------|
| Explorar | Explorar | Descubre emisoras de todo el mundo. |
| Favoritas | Tus favoritos | Tus emisoras guardadas, en tu orden. |
| Historial | Tu historial | Lo último que has escuchado. (vigente, referencia de estilo) |

## Transiciones de estado (sin cambios)

- Panel en reproducción normal: principal (play/pausa) + silenciar. En error: reintentar (sin silenciar). En carga: cancelar. La ficha no altera el estado de reproducción al abrirse, compartirse o cerrarse.
- Ficha de emisora: contenido estático + sección "Enlace" con dos acciones independientes (compartir del sistema / copiar con aviso); ambas deshabilitadas o con aviso cuando no hay enlace.
