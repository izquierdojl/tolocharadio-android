# Bug Assessment: Las estadísticas de podcast cuentan la posición, no el tiempo real de escucha

- **Slug**: 0051-izquierdojl-20261120-podcast-listening-time
- **Created**: 2026-11-20
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: medium

## Report (verbatim or summarized)

> Hay otro problema con las estadísticas del apartado de podcast. No sé si es
> problema de la aplicación o del servicio del API, me lo dices si tengo que
> revisar el servicio. Cuando se contabilizan el tiempo, no se tiene en cuenta el
> tiempo real de escucha, sino el total del episodio, por ejemplo, puedo escuchar
> un episodio que dura 2 horas en total, pero si sólo escucho realmente 30
> minutos que me interesan, debería contabilizar sólo 30 minutos, no el total.
> Revisa si es problema de la app o del API, y si es de la app ajústalo.

No se aportó URL; el reporte es texto pegado.

## Symptom

El tiempo acumulado en **Configuración → Gráficas → Podcasts** (y las filas de
"actividad reciente") crece como si se hubiera escuchado todo el tramo recorrido
del episodio. Si el usuario avanza hasta el minuto 90 de un episodio de 2 h y
escucha sólo 5 minutos, la app informa `listenedMs = 95 min`; si termina el
episodio tras un salto, informa la duración completa. Lo esperado es sumar sólo
el tiempo de reproducción continua real, descontando saltos y pausas.

## Reproduction

1. Tener un episodio de podcast de duración conocida (p. ej. 2 h).
2. Reproducirlo y dejarlo avanzar unos segundos.
3. Arrastrar la barra de progreso hasta el minuto 90 y escuchar 5 minutos.
4. Pausar o detener.
5. Abrir **Configuración → Gráficas** en modo Podcasts: el tiempo total y el de
   la sesión reciente reflejan ~95 min, no los ~5 min escuchados.
6. Alternativa más corta: escuchar 30 min desde el inicio, saltar al final y
   dejarlo terminar; el evento `Ended` informa la duración completa.

No se reprodujo en dispositivo para no contaminar las estadísticas reales de la
cuenta; el fallo se deduce del flujo de código y se confirma contra el contrato
del backend y la implementación web.

## Evidence

El contrato del backend está bien definido: `listenedMs` es el tiempo escuchado
**de la sesión**, y el cliente lo envía en cada reporte de progreso.

`~/soft/tolocharadio/apps/api/src/services/podcasts/progress.ts:95-101,145-153`

```ts
const maxListenedMs = effectiveDurationSec
  ? Math.min(MAX_SESSION_LISTENED_MS, effectiveDurationSec * 1000 * 3)
  : MAX_SESSION_LISTENED_MS;
const clampedListenedMs = Math.max(0, Math.min(Math.round(input.listenedMs), maxListenedMs));
// …
const prevListenedMs = existingSession?.listenedMs ?? 0;
const newSessionListenedMs = Math.max(prevListenedMs, clampedListenedMs);
const deltaMs = Math.max(0, newSessionListenedMs - prevListenedMs);
```

El servidor confía en el cliente para `listenedMs`, lo acumula como marca de
agua por sesión y sólo añade el delta a `user_podcast_stats_hourly`. La web hace
lo correcto: acumula el avance continuo de `currentTime` y descarta saltos.

`~/soft/tolocharadio/apps/web/src/lib/playbackEngine.ts:406-418`

```ts
if (this.status === "playing" && this.lastAudioTime !== null) {
  const deltaSec = audioTime - this.lastAudioTime;
  // Solo acumulamos avance continuo de reproduccion (descartamos saltos seek hacia delante)
  if (deltaSec > 0 && deltaSec <= 5) {
    this.listenedMs += Math.round(deltaSec * 1000);
  }
}
```

`~/soft/tolocharadio/apps/web/src/stores/player.ts:126-147` envía
`listenedMs: Math.round(state.listenedMs)`.

En Android, en cambio, **todos** los reportes derivan `listenedMs` de la
posición absoluta:

`app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt:277-301`

```kotlin
val positionSec = (if (completed) durationMs else positionMs) / MS_PER_SEC
// …
listenedMs = positionSec * MS_PER_SEC,
```

Y el keepalive de segundo plano repite el mismo cálculo:

`app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt:198-212`

```kotlin
listenedMs = positionSec * MS_PER_SEC,
```

El repositorio refuerza el error con valores por defecto que igualan
`listenedMs` a la posición:

`app/src/main/java/com/izquierdojl/tolocharadio/data/repo/ProgressRepo.kt:52-60,78-86`

```kotlin
listenedMs: Long = positionSec * MS_PER_SEC,
```

Además, el keepalive de segundo plano pasa `sequence + 1` sin incrementar el
contador, por lo que dos keepalives seguidos repiten secuencia y el backend
descarta el segundo (`progress.ts:133-143`).

Hay un tercer camino que inyecta tiempo no escuchado: marcar manualmente un
episodio como reproducido envía la duración completa como `listenedMs`
(`EpisodeActionsViewModel.kt:188-202`).

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt:277-301`
  — `reportProgress()` calcula `listenedMs` desde `positionSec`; afecta a
  reportes periódicos, pausa, búsqueda, parada y finalización.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt:198-212`
  — el keepalive de segundo plano repite el cálculo y no incrementa `sequence`.
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/ProgressRepo.kt:52-99`
  — los valores por defecto de `report()` y `keepalive()` permiten seguir
  enviando la posición como tiempo escuchado sin que el compilador lo detecte.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodeActionsViewModel.kt:188-202`
  — marcar como reproducido envía la duración completa como escucha.
- `~/soft/tolocharadio/apps/api/src/services/podcasts/progress.ts:95-153`
  — acumulación correcta por delta; no requiere cambios.
- `~/soft/tolocharadio/apps/web/src/lib/playbackEngine.ts:406-418`
  — referencia de comportamiento correcto (avance continuo, sin saltos).

## Root Cause Hypothesis

Confianza: **high**.

El fallo es de la **app Android**, no del API. El contrato del servidor separa
posición (`positionSec`) y tiempo escuchado (`listenedMs`), y la web acumula este
último a partir de deltas continuos de reproducción. Android reutiliza la
posición como si fuera tiempo escuchado en cada reporte: al hacer un salto hacia
delante, el propio reporte del `seek` ya declara minutos no escuchados, y el
backend los acepta porque sólo puede confiar en el valor del cliente. El mismo
error se repite al pausar, al parar y al completar, donde se envía la duración
total. El resultado son estadísticas infladas de forma proporcional a lo lejos
que se haya avanzado en el episodio, no al tiempo real de escucha.

## Proposed Remediation

**Preferred**: acumular en el cliente el tiempo de reproducción continua y
enviarlo como `listenedMs`, con paridad respecto a la web:

1. Añadir un acumulador puro en `domain/podcasts` (p. ej.
   `ListeningTimeTracker`) que reciba `(positionMs, playing)` y sume sólo deltas
   positivos dentro de una ventana continua (5–10 s), descartando saltos hacia
   delante y hacia atrás. Se reinicia al empezar una sesión de episodio.
2. En `EpisodePlayerViewModel`, alimentar el acumulador con cada emisión de
   `EpisodePlaybackController.State.Active` y enviar `listenedMs = acumulado`
   en todos los reportes (periódico, pausa, búsqueda, parada, fin y keepalive).
   Hacer una sincronización final con la posición del reporte para no perder el
   último tramo, siempre sujeta al mismo límite anti-salto.
3. En `onAppBackground()`, incrementar `sequence` antes de enviar el keepalive
   para que el backend no lo descarte como duplicado.
4. Quitar los valores por defecto de `listenedMs` en `ProgressRepo.report()` y
   `keepalive()`, obligando a cada llamador a decidir explícitamente el tiempo
   escuchado.
5. En `EpisodeActionsViewModel.togglePlayed()`, no declarar la duración completa
   como tiempo escuchado. Se propone enviar `listenedMs = 0` al marcar como
   reproducido; el estado `completed` sigue siendo explícito y el episodio no
   suma minutos que nadie ha escuchado. Si se prefiere conservar el
   comportamiento actual para que cuente como "empezado", dejarlo documentado y
   fuera de este fix.

**Alternatives**:
- Corregir en el backend calculando el tiempo escuchado a partir de la posición:
  no es viable, porque el servidor no puede distinguir un salto de un avance
  normal ni conoce el tiempo real entre reportes; seguiría inflando.
- Acumular tiempo de reloj en el controlador (`elapsedRealtime`) en vez de
  deltas de posición: es más robusto ante bloqueos del proceso, pero difiere de
  la semántica web (a 2× contarían 30 min reales en vez de 60 min de contenido)
  y exigiría otro diseño de sincronización. Se descarta para mantener paridad.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/ListeningTimeTracker.kt`
  (nuevo)
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/ProgressRepo.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodeActionsViewModel.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/ListeningTimeTrackerTest.kt`
  (nuevo)
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModelTest.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/ProgressRepoTest.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodeActionsViewModelTest.kt`

**Tests to add or update**:
- El acumulador suma avances de 1 s, ignora saltos de 30 s hacia delante y
  hacia atrás, ignora posiciones mientras está en pausa y se reinicia.
- Un reporte periódico envía el tiempo acumulado, no la posición.
- Hacer `seek` hacia delante no infla `listenedMs`; el reporte del seek envía
  sólo lo escuchado hasta ese momento.
- Parar tras un salto envía el acumulado y no la posición final.
- El keepalive de segundo plano incrementa `sequence` y envía el acumulado.
- Marcar manualmente como reproducido no añade la duración completa.
- `ProgressRepo.report()` respeta el `listenedMs` recibido aunque difiera de la
  posición.

## Risks & Considerations

- El acumulador depende de que el `ViewModel` siga recolectando el estado del
  controlador; el `EpisodePlayerViewModel` es de ámbito de actividad y
  sobrevive a la navegación, así que el muestreo continúa mientras suena el
  episodio. Si el proceso se congela en segundo plano, se puede perder el tramo
  no muestreado; es el mismo compromiso que la web y el keepalive mitiga la
  pérdida.
- Un salto pequeño (< ventana continua) se contará como escuchado. Con una
  ventana de 10 s el error máximo es acotado y muy inferior al actual.
- Al dejar de inflar `listenedMs`, el auto-completado del backend (95 % de
  posición + 80 % de escucha) ya no se disparará tras un salto al final. Es el
  comportamiento correcto y coherente con el bug 0050.
- El cambio afecta sólo a estadísticas; no altera la reproducción ni el
  guardado de posición.
- No se modifica el API ni la base de datos. Las estadísticas ya infladas no se
  corrigen retroactivamente; sólo cambia lo que se registre a partir del fix.

## Open Questions

- [NEEDS CLARIFICATION: ¿marcar manualmente un episodio como reproducido debe
  sumar tiempo de escucha? La propuesta es que no (0 ms) y que sólo cambie el
  estado `completed`. Si se desea que cuente como "empezado" sin sumar tiempo,
  habría que ajustar el backend, porque hoy `episodesStarted` se calcula con la
  suma de `listenedMs`.]
