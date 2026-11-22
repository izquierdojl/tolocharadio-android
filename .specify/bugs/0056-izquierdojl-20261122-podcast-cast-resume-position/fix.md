# Bug Fix: Reanudar en Cast la posición guardada del episodio

- **Slug**: 0056-izquierdojl-20261122-podcast-cast-resume-position
- **Fixed**: 2026-11-22
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

La rama Cast de `EpisodePlaybackController.play()` ya propaga `startMs` y
`CastPlayerManager.connectToEpisode()` lo entrega al reproductor remoto con
`setMediaItem(item, startMs)`, la sobrecarga que Media3 reenvía al receptor como
posición inicial. Un episodio a medias deja de empezar en 0:00 al emitirlo.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` | modified | `connectToEpisode()` acepta `startMs` y usa `player.setMediaItem(item, startMs.coerceAtLeast(0))`. |
| `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt` | modified | La rama `casting` pasa `startMs` a `connectToEpisode()`. |
| `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackControllerTest.kt` | added test | Verifica la propagación de la posición en la rama Cast. |

## Diff Highlights

```kotlin
cast.connectToEpisode(source.uri, episode.title, podcast?.title, startMs)
```

```kotlin
fun connectToEpisode(
    uri: String,
    title: String,
    artist: String?,
    startMs: Long,
) {
    if (!isCastConnected) return
    val player = castPlayer ?: return

    castError = null
    player.setMediaItem(
        mediaItemFactory.createEpisodeCastItem(uri, title, artist),
        startMs.coerceAtLeast(0),
    )
    player.prepare()
    player.playWhenReady = true
}
```

## Tests Added or Updated

- `EpisodePlaybackControllerTest::play en Cast propaga la posicion de reanudacion` — con `CastPlayerManager` y `Player` simulados, comprueba que `play(startMs = 120_000)` llama a `connectToEpisode(..., 120_000L)` y que el estado activo conserva `positionMs = 120_000` y `casting = true`.

## Local Verification

- Commands run: `./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackControllerTest"` → `BUILD SUCCESSFUL`. Antes del cambio el test no compilaba (`Too many arguments for 'fun connectToEpisode'`), que era el rojo de partida.
- Commands run: `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug compileDebugAndroidTestKotlin` → `BUILD SUCCESSFUL`, 133 suites / 721 tests, 0 fallos.
- Manual checks: **pendiente con un receptor Cast real**. El emulador no permite validar el comportamiento del receptor; queda para `/speckit.bug.test` o para una prueba con Chromecast.

## Deviations from Assessment

- Ninguna. Se aplicó la remediación preferida (parámetro `startMs` + `setMediaItem(item, startMs)`); solo se añadió `coerceAtLeast(0)` como defensa para no enviar una posición negativa al receptor.

## Follow-ups

- Verificar en un Chromecast físico que el receptor reanuda en la posición guardada y que, al desconectar, `resumeLocally()` conserva la posición.
- Si el receptor ignora la posición inicial en algún modelo, evaluar `prepare()` + `seekTo(startMs)` tras la confirmación de la sesión.
- Detectar el caso de conectar Cast **durante** la reproducción local: hoy no se transfiere el episodio en curso (comportamiento independiente, no cubierto por este bug).
