# Contratos UI: Mejoras estéticas UI

**Feature**: `0039-jlizquierdo-20260920-mejoras-esteticas-ui` | **Date**: 2026-09-20

Contratos observables por estado (para guiar implementación y tests de UI). Sin APIs ni endpoints nuevos.

## C1. Lista de favoritas (modo lista)

- Cada fila muestra: asa de arrastre, artwork, nombre, país/idioma, etiquetas, botón reproducir, corazón (quitar).
- Ninguna fila muestra botón de más opciones ni "Mover arriba" ni "Mover abajo".
- Arrastrar el asa mueve la fila en vivo; al soltar se guarda el orden (comportamiento vigente).

## C2. Mini-player

- Reproducción normal (sonando/pausado): exactamente 2 acciones — principal (Pausar/Reanudar) + silenciar (Silenciar/Activar sonido). No existe "Copiar enlace".
- Error: acción de reintentar (sin silenciar, sin copiar). Carga: indicador con cancelar.

## C3. Ficha de emisora, sección "Enlace"

- Entrada: `station.url` resuelto con `resolveCopyLink` (recorte + vacío ⇒ nulo).
- Si hay enlace, la sección "Enlace" (encima de la homepage) ofrece:
  - **Compartir** (principal): abre el menú de compartir del sistema con el enlace como texto completo.
  - **Copiar** (secundaria): deja el enlace completo en el portapapeles y muestra "Enlace copiado".
  - El texto del enlace se muestra recortado a modo informativo (máximo 2 líneas con elipsis); Compartir y Copiar usan siempre el enlace completo.
- Si no hay enlace: cualquier intento muestra "Enlace no disponible" y no copia ni comparte nada.
- Abrir, compartir o cerrar la ficha no interrumpe la reproducción.

## C4. Encabezados de sección

- Explorar: título "Explorar" + subtítulo "Descubre emisoras de todo el mundo.".
- Favoritas: título "Tus favoritos" + subtítulo "Tus emisoras guardadas, en tu orden.".
- Mismo estilo tipográfico y color de subtítulo que el historial.
