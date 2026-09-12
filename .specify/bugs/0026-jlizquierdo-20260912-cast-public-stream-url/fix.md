# Bug Fix: Cast reproduce con la URL pública de la emisora

- **Slug**: 0026-jlizquierdo-20260912-cast-public-stream-url
- **Fixed**: 2026-09-12
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

El `MediaItem` que se envía a Cast ya no usa el proxy autenticado (`/api/v1/playback/{id}`, que devuelve 401 al receptor) sino la **URL pública de la emisora** (`station.url`), con su `mimeType`. La reproducción local sigue usando el proxy con `Authorization: Bearer`.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactory.kt` | modified | `castUriFor` (URL pública, fallback al proxy) y `createForCast`; `metadataFor` extraído |
| `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` | modified | `connectToStation` usa `createForCast` |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactoryTest.kt` | updated test | URL pública de Cast y fallback al proxy |

## Diff Highlights

```kotlin
fun castUriFor(
    station: StationDto,
    fallbackBaseUrl: String,
): String = station.url.ifBlank { streamUrl(fallbackBaseUrl, station.id) }

fun createForCast(
    station: StationDto,
    source: PlaybackSource,
    fallbackBaseUrl: String,
): MediaItem =
    MediaItem.Builder()
        .setUri(castUriFor(station, fallbackBaseUrl))
        .setMediaMetadata(metadataFor(station))
        .setMimeType(mimeTypeFor(station, source))
        .build()
```

```kotlin
// CastPlayerManager.connectToStation
val item = mediaItemFactory.createForCast(station, source, baseUrl)
player.setMediaItem(item)
```

## Tests Added or Updated

- `StationMediaItemFactoryTest`: `cast usa la url publica de la emisora`, `cast cae al proxy si la emisora no tiene url`.

## Local Verification

- `gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` → **BUILD SUCCESSFUL**.

## Deviations from Assessment

- Ninguna reseñable. Se descartó la alternativa de URL firmada en servidor por decisión del usuario.

## Follow-ups

- Verificar en dispositivo con varias emisoras (progresivas y HLS).
- Valorar mostrar errores de Cast en UI (`CastPlayerManager.onPlayerError`) para no fallar en silencio.
