# Bug Assessment: Un episodio completado vuelve a aparecer en la cola

- **Slug**: 0050-izquierdojl-20261120-podcast-queue-completed-returns
- **Created**: 2026-11-20
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: medium

## Report (verbatim or summarized)

> Hay un comportammiento extraño en la cola que he visto hace un rato. Si
> reproduzco un episodio que está en la cola, cuando termina ha vuelto a la cola
> y ha vuelto a aparecer. Revisa este comportamiento, si un episodio se
> reproduce completamente o casi completamente (digamos a un 98% por ejemplo) ya
> no debería volver a la cola y debería quedarse como reproducido completamente
> para evitar estos problema. Revisa bien el funcionamiento porque creo que hay
> un bug.

No se aportó URL; el reporte es texto pegado.

## Symptom

Al reproducir un episodio que está en la cola y llegar al final (o pararlo cerca
del final, ~98 %), el episodio se marca como completado pero **sigue en la
cola** y vuelve a aparecer entre los pendientes. Lo esperado es que desaparezca
de la cola al completarse y que no se reproduzca de nuevo automáticamente.

## Reproduction

1. Tener una cola con al menos dos episodios, por ejemplo A y B.
2. Reproducir A desde la cola y dejarlo terminar, o pararlo al ~98 %.
3. Comprobar que A queda marcado como completado.
4. Abrir la cola: A sigue listado entre los pendientes (y puede volver a sonar
   en el siguiente auto-avance).

No se reprodujo en el dispositivo para no alterar la cola y el progreso reales
de la cuenta; el fallo se deduce del flujo de código y de la diferencia con la
implementación web, que sí retira el episodio terminado.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt:229-245`
  — `onEnded()` marca completado y busca el siguiente episodio, pero nunca
  retira el episodio terminado de la cola.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt:259-298`
  — `report()` y `complete()` envían el progreso, pero descartan la respuesta
  del servidor; si el servidor auto-completa al 95 %/98 %, la app no reacciona.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt:173-183`
  — `stop()` reporta la posición final de forma asíncrona y limpia la UI, pero
  no comprueba si ese reporte ha completado el episodio.
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/QueueRepo.kt:75-82`
  — `remove()` existe y actualiza la cola, pero no se usa en el flujo de
  finalización.
- `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/QueueOrder.kt:34-37`
  — `nextQueueEpisodeId()` se limita a elegir el primer id distinto del
  terminado; no retira ni considera el estado completado.
- Web de referencia:
  `~/soft/tolocharadio/apps/web/src/components/podcasts/PodcastAutoAdvance.tsx:22-33`
  — al terminar, hace `removeEpisodeFromQueue(episode.id)` antes de reproducir el
  siguiente. Esa paridad falta en Android.
- Backend:
  `~/soft/tolocharadio/apps/api/src/services/podcasts/progress.ts:155-166`
  — auto-completa con posición ≥ 95 % y escucha acumulada ≥ 80 %;
  `apps/api/src/services/podcasts/queue.ts` no retira el episodio de la cola al
  completarse, por lo que la limpieza corresponde al cliente.
- `specs/0046-izquierdojl-20261118-modo-podcast-completo/spec.md:190` (FR-016)
  — exige marcar completado, retirarlo de pendientes/continuar y auto-avanzar.

## Root Cause Hypothesis

Confianza: **high**.

El auto-avance de Android está incompleto respecto a la web: marca el episodio
como completado y salta al siguiente, pero no ejecuta
`QueueRepo.remove(episodeId)`. El servidor tampoco elimina el elemento de la
cola al completar el progreso, así que el episodio permanece en
`podcast_queue_items` y reaparece en la vista de pendientes. En el caso de una
parada al 98 %, el backend ya lo auto-completa (95 % de posición + 80 % de
escucha), pero `EpisodePlayerViewModel.report()` ignora el `completed` devuelto
y no retira el elemento, por lo que el mismo síntoma se produce sin llegar al
evento `Ended`.

## Proposed Remediation

**Preferred**: completar la paridad con la web en `EpisodePlayerViewModel`:

1. En `onEnded()`, convertir `complete()` en una operación esperada (suspend) y,
   tras reportar `completed = true`, llamar a `queue.remove(episodeId)` antes de
   calcular y reproducir el siguiente. Si la retirada falla, continuar con el
   auto-avance y dejar que el próximo refresco reconcilie.
2. En `stop()`, esperar la respuesta de `ProgressRepo.report()` y, si
   `PodcastEpisodeProgress.completed == true`, retirar el episodio de la cola.
   Esto cubre la parada al 98 % sin duplicar en el cliente el umbral del
   servidor.
3. No retirar en cada reporte periódico: un episodio ya completado que se vuelve
   a encolar y se reproduce devolvería `completed = true` en el primer reporte y
   se retiraría antes de que el usuario termine de reescucharlo.
4. Opcionalmente, aprovechar también la respuesta del keepalive de segundo plano
   si el servidor completa ahí; puede quedar como mejora posterior.

**Alternatives**:
- Hacer que el backend elimine el elemento de la cola cuando el progreso pasa a
  completado. Beneficia a todos los clientes y evita depender de que cada uno lo
  limpie, pero cambia el comportamiento del servidor y puede sorprender si el
  usuario vuelve a encolar un episodio completado; además la web ya asume la
  limpieza en cliente.
- Añadir un umbral local de 98 % en el cliente. Duplica una regla que el backend
  ya aplica (95 %/80 %) y puede marcar como completado un salto al final sin
  escucha real; se descarta.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/ProgressRepo.kt`
  (si se devuelve el progreso resultante del reporte)
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModelTest.kt`

**Tests to add or update**:
- Al emitir `EpisodePlayerEvent.Ended`, se marca completado, se llama a
  `queue.remove(episodeId)` y se reproduce el siguiente.
- Parar al 98 % con respuesta `completed = true` retira el episodio de la cola.
- Parar a mitad con respuesta `completed = false` no lo retira.
- Un fallo de `queue.remove` no impide el auto-avance ni deja la UI bloqueada.
- Un reporte periódico de un episodio ya completado y reencolado no lo retira
  antes de terminar (si se mantiene la restricción del punto 3).

## Risks & Considerations

- Retirar en cada reporte periódico podría quitar de la cola un episodio
  completado que el usuario está reescuchando; limitar la retirada a `Ended` y
  parada explícita.
- Hay una carrera potencial entre el reporte de finalización y el cálculo del
  siguiente episodio; conviene esperar el reporte o al menos la retirada antes
  de elegir el siguiente.
- Si la retirada falla por red, el episodio reaparecerá hasta el próximo refresco
  de la cola; valorar una actualización local optimista o un reintento silencioso.
- El auto-completado del backend exige también 80 % de escucha acumulada; un
  salto al 98 % sin escuchar no se marcará completado. Es coherente con
  "reproducir casi completamente", pero conviene confirmarlo con el usuario.
- `nextQueueEpisodeId()` elige el primer elemento distinto del terminado, no
  estrictamente el siguiente en orden; es un comportamiento preexistente
  compartido con la web y no forma parte de este bug, pero puede amplificar la
  sensación de "vuelve a la cola" si el episodio terminado estaba en medio.

## Open Questions

- [NEEDS CLARIFICATION: ¿el criterio de "casi completo" debe ser solo posición
  ≥ 98 %, o se mantiene la regla del servidor de 95 % de posición + 80 % de
  escucha acumulada? Se propone respetar la regla del servidor y usar su flag
  `completed`.]
- [NEEDS CLARIFICATION: ¿debe retirarse de la cola también cuando el episodio se
  completa desde otro dispositivo? Hoy el servidor no lo retira; la app podría
  reconciliarlo al refrescar la cola, pero eso amplía el alcance.]
