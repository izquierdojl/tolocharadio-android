# Bug Assessment: Un corte de red en un episodio deja la reproducción en error terminal

- **Slug**: 0057-izquierdojl-20261122-podcast-error-sin-recuperacion
- **Created**: 2026-11-22
- **Source**: reporte del usuario en sesión (texto pegado)
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> Esta tarde, iba caminando y he entrado en una zona sin conexión, la
> reproducción se ha cortado, no sé si porque no podía reproducir o porque no
> veía el servidor y le bloqueaba algún timeout o similar.

Reportado sobre un episodio de podcast en streaming (no descargado). No hay URL
ni traza; el reporte es texto libre.

## Symptom

Al perder la cobertura durante la reproducción de un episodio, el audio se
detiene y la app queda en estado de error («No se pudo reproducir el
episodio»). Al recuperar la cobertura la reproducción no continúa: hay que
volver a pulsar play. Lo esperado es pausar/reintentar y reanudar en la misma
posición, como ya hace la radio.

## Reproduction

1. Reproducir un episodio **en streaming** (sin descarga local) con el perfil de
   buffer por defecto (Equilibrado, 20–50 s).
2. Entrar en una zona sin cobertura (o activar modo avión) y permanecer así más
   tiempo que el buffer disponible.
3. **Observado**: el episodio se detiene con «No se pudo reproducir el
   episodio» y no vuelve al recuperar la señal.
4. **Esperado**: la reproducción se pausa o reintenta y continúa en la misma
   posición al volver la red.
5. Variante a confirmar: si antes se estaba oyendo la radio, al recuperar la
   cobertura puede empezar a sonar la emisora en lugar del episodio
   [NEEDS CLARIFICATION: reproducir con una emisora previa en la misma sesión].

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt:103` — `onPlayerError` pasa a `State.Error` y para el ticker; no hay reintento ni estado de espera de red.
- `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt:280` — `onEnded` es el único final previsto; el error no conserva posición para reanudar.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt:87` — el listener de errores se engancha al `ExoPlayer` compartido y **no** comprueba si hay un episodio activo.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt:206` — al volver la conectividad, `WaitingForNetwork` relanza `startRecovery()` con la emisora guardada.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt:384` — `startRecovery()` usa `activeStationHolder.station` como único destino.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt:218` — un `networkChanges` con red aún «online» puede disparar una reconexión de radio mientras suena un episodio.
- `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt:34` — el datasource de episodios usa `readTimeout = 20 s`: convierte el silencio de red en `PlaybackException`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt:113` — `State.Error` solo muestra el mensaje; no hay acción de reintento ni reanudación.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt:515` — `activeStationHolder` solo se limpia al parar/cancelar la radio, así que puede quedar una emisora «fantasma».

## Root Cause Hypothesis

Confianza **alta** en el fallo principal y **media** en la variante de la radio.

1. **El VOD no tiene política de recuperación**. `EpisodePlaybackController` trata
   cualquier `PlaybackException` como terminal. El `readTimeout` de 20 s del
   datasource plano convierte un atasco de red en error, y éste no se reintenta,
   no espera a la red y no reanuda.
2. **La recuperación de radio no está aislada del VOD**. `PlaybackRecoveryManager`
   escucha el mismo `ExoPlayer` que usa el episodio y no filtra por
   `EpisodePlaybackController.isEpisodeActive()`. Si `activeStationHolder`
   conserva una emisora, el manager puede reiniciarla al recuperar cobertura (o
   dejar el estado `WaitingForNetwork` de la radio visible durante el episodio).

El corte de audio en sí es esperado al agotar el buffer; lo que es un defecto es
que el fallo sea terminal y que otro subsistema pueda tomar el reproductor.

## Proposed Remediation

**Preferred**: separar responsabilidades en dos pasos.

1. **Aislar la recuperación de radio** (fix mínimo y primero):
   - Inyectar en `PlaybackRecoveryManager` una comprobación de episodio activo
     (o el propio `EpisodePlaybackController`, rompiendo el ciclo con una
     interfaz pequeña) y salir temprano en `onPlayerError`, watchdog,
     `networkChanges` y el colector de conectividad cuando haya un episodio.
   - Limpiar/neutralizar el `activeStationHolder` al arrancar un episodio solo si
     eso no rompe la reanudación de radio posterior (ver riesgos).

2. **Dar al VOD su propia recuperación**:
   - Añadir estados equivalentes a `WaitingForNetwork` / `Reconnecting` al
     controlador de episodios, con backoff acotado y cancelación al pausar o
     parar el usuario.
   - Guardar la última posición conocida antes de entrar en error y reanudar en
     ella, sin reportar posiciones falsas (coordinado con el fix 0054).
   - Mantener un botón de reintento manual como salida cuando se agote el
     presupuesto.

**Alternativas**:
- Un `ExoPlayer` dedicado a VOD: elimina de raíz el listener compartido, pero
  obliga a tocar `MediaSession`, Cast y volumen. Mayor coste y riesgo.
- Solo aislar la radio (fix mínimo): evita el efecto colateral, pero el episodio
  sigue fallando de forma terminal.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt` (si se añade `LoadErrorHandlingPolicy`)

**Tests to add or update**:
- `PlaybackRecoveryManagerTest`: error con episodio activo + emisora en el holder
  → el manager no arranca la radio y no emite `WaitingForNetwork` de radio.
- `PlaybackRecoveryManagerTest`: sin episodio activo, el comportamiento actual de
  radio no cambia (regresión).
- `EpisodePlaybackControllerTest`: error de red → estado de espera, no `Error`;
  al volver la red reanuda en la posición previa; pausa del usuario cancela.
- `EpisodePlaybackControllerTest`: presupuesto agotado → `Error` con acción de
  reintento y posición conservada.
- `EpisodePlayerViewModelTest`: la UI muestra «Sin conexión, reintentando» y
  luego vuelve a `Active` sin perder la posición.

## Risks & Considerations

- **Regresión de radio**: el gate por episodio debe cubrir todos los puntos de
  entrada del manager, no solo `onPlayerError`.
- **Bug 0054**: la posición del episodio ya tiene un problema conocido de
  reporte; la recuperación no debe enviar 0 ni duplicar keepalives.
- **Bucles de batería**: reintentar indefinidamente mientras no hay red consume;
  conviene esperar al evento de conectividad y un backoff con tope.
- **Logs**: mantener tag + causa + `code`, sin PII ni URLs firmadas.
- **Sin cambios de API ni de servidor**: la reproducción sigue usando la URL
  firmada y el proxy.

## Open Questions

- [NEEDS CLARIFICATION: ¿la recuperación del VOD debe esperar a la red de forma indefinida mientras la app esté en primer plano, o agotar un presupuesto y mostrar reintento manual?]
- [NEEDS CLARIFICATION: ¿se confirma la variante de que la radio toma el reproductor tras un error de episodio, o solo ocurre al cambiar de contenido manualmente?]
- [NEEDS CLARIFICATION: ¿el reintento debe ser silencioso o mostrar un aviso persistente en la tarjeta del episodio?]
