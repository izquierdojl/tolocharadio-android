# Bug Assessment: Perder la reproducción de un episodio de la cola borra su posición

- **Slug**: 0054-izquierdojl-20261122-podcast-resume-position-lost
- **Created**: 2026-11-22
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> Cuando pierdes reproducción sobre un episodio de la cola del que has escuchado
> una parte, no lo respeta y comienza desde el principio.

No se aportó URL; el reporte es texto pegado.

## Symptom

Se escucha una parte de un episodio que está en la cola. Cuando esa reproducción
se pierde (por ejemplo, al empezar la radio, al llegar el temporizador de
sueño o al salir de la app), la siguiente selección del mismo episodio empieza
en 0:00 en lugar de continuar por la posición guardada. Lo esperado es reanudar
en la última posición conocida (FR-001/FR-006 de spec 0047, SC-003 de spec 0046).

## Reproduction

1. Tener podcasts habilitados y un episodio con progreso parcial en la cola.
2. Reproducirlo unos minutos desde la cola.
3. Perder esa reproducción: cambiar a Radio (o dejar que expire el temporizador
   de sueño) y después pasar la app a segundo plano.
4. Volver a Podcasts y seleccionar el mismo episodio desde la cola.
5. **Observado**: empieza en 0:00. **Esperado**: continúa en la posición previa.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt:91` — en `State.Idle` conserva `_ui.episode`; la tarjeta del episodio sobrevive a una parada externa.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt:203` — `onAppBackground()` calcula la posición con `controller.positionMs()` y hace keepalive aunque el controlador ya no tenga el episodio.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt:178` — `stop()` hace lo mismo: reporta la posición devuelta por `controller.stop()`.
- `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt:205` — `positionMs()` devuelve `0` cuando `isEpisodeActive()` es falso.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt:359` — al arrancar la radio llama a `episodes.stop()` directamente, sin pasar por el ViewModel de episodios ni reportar progreso.
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt:130` — el temporizador de sueño llama a `episodeVm.stop()` incluso si el episodio ya no está activo.

## Root Cause Hypothesis

Confianza alta. El reproductor local es compartido entre radio y podcast. Cuando
la radio toma el reproductor (`PlayerViewModel.play()` → `episodes.stop()`), el
controlador pasa a `Idle`, pero `EpisodePlayerViewModel` mantiene `episode` en
su `_ui` y lo muestra como una tarjeta pausada «fantasma». A partir de ahí,
cualquier ruta que reporte progreso (`onAppBackground()`, `stop()` del
temporizador/miniplayer o el ticker si se pulsa play) lee
`controller.positionMs()`, que vale `0` porque `isEpisodeActive()` es falso, y
envía `positionSec = 0` al servidor. El backend sobrescribe `podcast_progress`
con 0 (no hay guarda para posiciones no nulas), así que la siguiente selección
del episodio carga 0 y empieza desde el principio.

El mismo mecanismo explica otros dos síntomas asociados: la tarjeta del podcast
tapa al miniplayer de la radio y pulsar play en ella reanuda el reproductor
equivocado.

## Proposed Remediation

**Preferred**: tratar `State.Idle` como «no hay episodio» en
`EpisodePlayerViewModel`: limpiar `_ui.episode` (conservando la velocidad)
cuando el controlador emita `Idle` con un episodio cargado. Así desaparece la
tarjeta fantasma y ninguna ruta de reporte puede enviar 0. Capturar el
`podcast` antes del `launch` asíncrono de `onEnded()` para que el auto-avance
siga conociendo el programa. Añadir tests que simulen la parada externa y
verifiquen que no se envía `positionSec = 0`.

**Alternativas**:
- Guardar en `onAppBackground()`/`stop()` con `if (!controller.isEpisodeActive()) return`: evita el 0 pero deja la tarjeta fantasma y el play sobre la radio.
- Persistir la posición localmente y usarla cuando el controlador no esté activo: más robusto para procesos muertos, pero exige almacenamiento nuevo y no es necesario para este síntoma.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModelTest.kt`

**Tests to add or update**:
- Parada externa del controlador → la tarjeta se limpia y `onAppBackground()` no envía keepalive con posición 0.
- `stop()` después de una parada externa no reporta 0.
- El auto-avance conserva el `PodcastSummary` aunque el controlador haya pasado por `Idle`.

## Risks & Considerations

- Limpiar en `Idle` debe ejecutarse después de que `onEnded()` capture lo que necesita; de lo contrario el siguiente episodio perdería portada/programa. Se mitiga capturando antes del `launch`.
- No hay cambios de contrato ni de servidor: el backend ya guarda lo que recibe.
- Los reportes periódicos siguen teniendo una ventana de hasta 30 s; este fix evita la pérdida total, no reduce esa ventana.
- Un `stop()` legítimo en 0:00 sigue reportando 0, que es correcto.

## Open Questions

- Ninguna bloqueante. La reproducción exacta («radio», «temporizador» o «salir de la app») se confirmará con la verificación posterior; el fallo de datos es el mismo en todas.
