# Bug Assessment: La app se cierra al enviar a Cast una emisora no HLS

- **Slug**: 0025-jlizquierdo-20260912-cast-mime-type-missing
- **Created**: 2026-09-12
- **Source**: logcat del dispositivo (adb, Android 17 / Redmi Note 10)
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> Ya aparecen los dispositivos y se conecta como renderer, pero al reproducir, sale de la aplicación.

Stacktrace (buffer `crash`):

```
java.lang.IllegalArgumentException: The item must specify its mimeType
	at androidx.media3.cast.DefaultMediaItemConverter.toMediaQueueItem(DefaultMediaItemConverter.java:95)
	at androidx.media3.cast.CastPlayer.toMediaQueueItems(CastPlayer.java:1387)
	at androidx.media3.cast.CastPlayer.setMediaItemsInternal(CastPlayer.java:1144)
	at androidx.media3.cast.CastPlayer.setMediaItems(CastPlayer.java:324)
	at androidx.media3.common.BasePlayer.setMediaItem(BasePlayer.java:41)
	at com.izquierdojl.tolocharadio.cast.CastPlayerManager.connectToStation(CastPlayerManager.kt:285)
	at com.izquierdojl.tolocharadio.feature.player.PlayerViewModel.startSource(PlayerViewModel.kt:213)
```

## Symptom

Tras conectar a un dispositivo Cast, al pulsar play la app se cierra. Ocurre con **emisoras no HLS** (progresivas). Esperado: la emisora se reproduce en el receptor.

## Reproduction

1. Abrir la app en dispositivo Android 17 con receptor Cast en la misma Wi‑Fi.
2. Pulsar el botón Cast y conectar.
3. Reproducir una emisora que **no** sea `.m3u8` (p. ej. un stream MP3/AAC).
4. La app se cierra con `IllegalArgumentException: The item must specify its mimeType`.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactory.kt:59` — `mimeTypeFor(source)` devolvía `null` para emisoras progresivas y solo se asignaba el mime cuando no era nulo.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:285` — `castPlayer.setMediaItem(item)`; el `MediaItem` sin mime revienta en el conversor de Cast.

## Root Cause Hypothesis

**Confidence: high**

`androidx.media3.cast.DefaultMediaItemConverter.toMediaQueueItem` exige que todo `MediaItem` enviado a `CastPlayer` especifique `mimeType` (necesita el content type para el receptor). `StationMediaItemFactory` solo lo asignaba para HLS (`application/x-mpegURL`); en progresivo quedaba `null`, por lo que `CastPlayer.setMediaItem` lanzaba `IllegalArgumentException` en el hilo principal y cerraba la app. La ruta local (ExoPlayer) no valida el mimeType, de ahí que solo fallara al castear.

## Proposed Remediation

**Preferred**: que `StationMediaItemFactory` asigne siempre un `mimeType`: HLS → `application/x-mpegURL`; progresivo → derivado de la extensión de `station.url` (aac/m4a/mp4/ogg/oga/opus/flac) y `audio/mpeg` por defecto. Cambiar la firma de `mimeTypeFor(source)` a `mimeTypeFor(station, source): String` (no nulo).

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactory.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactoryTest.kt`

**Tests to add or update**:
- `mimeTypeFor` no nulo para progresivo (con extensión y sin ella) y HLS.

## Risks & Considerations

- Si el `mimeType` no coincide con el stream real, el receptor podría rechazarlo; el receptor suele seguir el `Content-Type` HTTP del proxy, pero conviene verificar en dispositivo.
- Queda por confirmar que el receptor pueda descargar el proxy si este exige auth (el player local añade `Authorization: Bearer`; el receptor no puede). Si tras este fix el receptor no reproduce, sería un segundo problema (auth del proxy en Cast).

## Open Questions

- [NEEDS CLARIFICATION: ¿el proxy `/api/v1/playback/{id}` requiere `Authorization`? Si sí, hace falta una URL firmada o token en query para el receptor.]
