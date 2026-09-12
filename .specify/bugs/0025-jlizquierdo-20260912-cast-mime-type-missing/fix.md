# Bug Fix: mimeType obligatorio en el MediaItem para Cast

- **Slug**: 0025-jlizquierdo-20260912-cast-mime-type-missing
- **Fixed**: 2026-09-12
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

`StationMediaItemFactory` ahora asigna **siempre** un `mimeType` al `MediaItem`: HLS → `application/x-mpegURL`; progresivo → según la extensión de la emisora (`aac`, `m4a`/`mp4`, `ogg`/`oga`, `opus`, `flac`) con `audio/mpeg` por defecto. Con esto `CastPlayer.setMediaItem` deja de lanzar `IllegalArgumentException: The item must specify its mimeType`.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactory.kt` | modified | `mimeTypeFor(station, source): String` no nulo + `audioMimeTypeFor`; `create` asigna siempre el mime |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactoryTest.kt` | updated test | Cubre mp3, sin extensión, aac y HLS |

## Diff Highlights

```kotlin
fun mimeTypeFor(
    station: StationDto,
    source: PlaybackSource,
): String = if (source.hls) MimeTypes.APPLICATION_M3U8 else audioMimeTypeFor(station.url)

private fun audioMimeTypeFor(url: String): String {
    val extension = runCatching { URI(url).path }
        .getOrNull()?.substringAfterLast('/')?.substringAfterLast('.', "")?.lowercase().orEmpty()
    return when (extension) {
        "aac" -> MimeTypes.AUDIO_AAC
        "m4a", "mp4" -> MimeTypes.AUDIO_MP4
        "ogg", "oga" -> MimeTypes.AUDIO_OGG
        "opus" -> MimeTypes.AUDIO_OPUS
        "flac" -> MimeTypes.AUDIO_FLAC
        else -> MimeTypes.AUDIO_MPEG
    }
}
```

```kotlin
val builder =
    MediaItem.Builder()
        .setUri(uriFor(source, baseUrl))
        .setMediaMetadata(metadata)
        .setMimeType(mimeTypeFor(station, source))
```

## Tests Added or Updated

- `StationMediaItemFactoryTest` — `mimeTypeFor` nunca es nulo: mp3 → `audio/mpeg`, sin extensión → `audio/mpeg`, aac → `audio/aac`, HLS → `application/x-mpegURL`.

## Local Verification

- `gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` → **BUILD SUCCESSFUL**.

## Deviations from Assessment

- Se descartó un test de `create()` en JVM porque `MediaItem.Builder.setUri` llama a `android.net.Uri.parse` (no mockeado sin Robolectric). La garantía de no-nulo la da el tipo de retorno `String` + los tests de `mimeTypeFor`.

## Follow-ups

- Verificar en dispositivo físico que el receptor reproduce tras el fix.
- Si el receptor recibe 401 del proxy (auth por servidor), hace falta token en la URL de Cast (query firmada) o un endpoint de reproducción sin Bearer para el receptor.
