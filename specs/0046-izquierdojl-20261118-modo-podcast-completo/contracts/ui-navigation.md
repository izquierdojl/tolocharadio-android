# Contract: Navegación y UI del modo Podcast

## Selector de modo
- Cabecera: segmentado `Radio | Podcasts`, visible solo con `features.podcasts=true`. Conmutar NO detiene la reproducción en curso (FR-002).
- Sin `podcasts`: sin selector, rutas ni opciones de podcast; Radio intacto.

## Barras inferiores (mismo shell, destinos por modo)
- Radio (actual, sin cambios): Explorar, Favoritos, Historial, Mis emisoras + Configuración.
- Podcasts (decisión clarify 2026-11-18): **Inicio, Descubrir, Suscripciones, Cola, Favoritos** + Configuración/Servidores con el mismo patrón (top-bar + destino).
- Estadísticas de podcast: accesible desde Inicio (no ocupa hueco inferior, espejo de Radio).

## Rutas nuevas (`Routes`)
- `podcast-home` (Inicio: continuar, novedades, programas, resumen cola + accesos a Descubrir/Cola)
- `podcast-discover` (búsqueda directorio + alta RSS + importar OPML + exportar)
- `podcast-subscriptions` (lista + baja)
- `podcast-show/{podcastId}` (ficha + episodios filtrables/ordenables/paginados)
- `podcast-queue` (pendientes con reorden por arrastre, quitar, vaciar con `keepEpisodeId`)
- `podcast-favorites` (episodios + corazón)
- `podcast-stats` (resumen, evolución, hábitos, categorías, recientes)
- Ficha de episodio: **hoja inferior modal** desde cualquier lista y desde el reproductor; acciones: reproducir/pausar, reintentar, a la cola (a continuación/al final), favorito, descargar, descartar (en continuar). Cierre: X, gesto atrás o toque fuera → devuelve el foco.

## Reproductor (persistente, compartido)
- Radio y podcast comparten mini-player + hoja completa; en episodio VOD añade: velocidad (1/1.25/1.5/2), saltos ±15/30 s, búsqueda en pista, insignia de origen (online/local) y acceso a la ficha.
- Cola: el episodio en curso se oculta de pendientes; pausar/detener/cambiar lo devuelve a su posición (lógica `mergeQueueOrder`).

## Configuración
- Nueva preferencia: sección inicial `Radio | Podcasts` (arranque con sesión). Se mantiene `startScreen` para la pantalla de Radio.
- Descargas: fila/ficha muestran estado (descargar → descargando % → descargado/local); con `podcastDownloads=false` la opción no existe.

## Estados transversales (español, accionables)
- Vacío (sin suscripciones/cola/favoritos/resultados) con acción a Descubrir; carga con esqueleto; error con motivo + Reintentar; 409 de cola con aviso + recarga; credenciales caducadas → editar servidor activo (flujo existente, sin pantallas de acceso).
