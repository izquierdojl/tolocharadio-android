# Quickstart: validación de buffer y caché de episodios

**Feature**: `0058-izquierdojl-20261122-podcast-buffer-cache`
**Date**: 2026-11-22

## Requisitos

- Dispositivo o emulador Android 26+ con la app instalada (`./gradlew installDebug`).
- Servidor de podcasts configurado y un episodio en streaming (no descargado).
- Capacidad de alternar Wi-Fi / datos móviles o modo avión.

## Comprobaciones automáticas

```bash
./gradlew testDebugUnitTest
./gradlew detekt ktlintCheck
./gradlew lintDebug
```

Resultado esperado: tres comandos en `BUILD SUCCESSFUL`, 0 tests fallidos.

Tests que deben existir y pasar:

- `EpisodeBufferSpecTest` — invariantes de `minBufferMs`, `maxBufferMs`,
  `bufferForPlaybackMs` y `bufferForPlaybackAfterRebufferMs`.
- `EpisodeCachePolicyTest` — `limitUnmeteredBytes >= limitMeteredBytes` y
  `prefetchAheadMeteredMs <= 300_000`.
- `QuotaCacheEvictorTest` — al bajar de no medida a medida, el tamaño se recorta
  al límite de 50 MB.
- `PlaybackEngineKindTest` — `ensureKind(VOD)` no altera el perfil de radio;
  `ensureKind(LIVE)` conserva `BufferProfile`.
- `EpisodePrefetchPolicyTest` — Wi-Fi = episodio completo; móvil = 5 min;
  cancelación en pausa/pérdida de red.
- `EpisodeMediaSourceFactoryTest` — clave `episode-<id>` y ausencia de caché para
  `content://`.

## Escenario 1 — Margen sin cobertura (SC-001)

1. Reproducir un episodio en streaming sobre Wi-Fi.
2. Esperar 2 minutos (el buffer debe llenarse hasta 4 min).
3. Activar modo avión durante 3 minutos.
4. **Esperado**: el audio no se corta; al desactivar el modo avión sigue en la
   misma posición, sin reiniciar.
5. **Contraprueba**: con el perfil anterior (50 s) se cortaba a los ~50 s.

## Escenario 2 — La radio no cambia (SC-002, SC-007)

1. Anotar la latencia del directo respecto a la emisión real (o medir con un
   evento conocido).
2. Reproducir un episodio y volver a la radio.
3. **Esperado**: la latencia del directo es la misma que antes de la feature
   (±2 s). El episodio no arrastra su buffer a la radio.

## Escenario 3 — Volver atrás sin red (SC-003)

1. Escuchar 5 minutos de un episodio en streaming.
2. Activar modo avión.
3. Retroceder 3 minutos.
4. **Esperado**: el audio se reproduce desde la caché.
5. Cerrar y reabrir la app sin red; el tramo debe seguir disponible.

## Escenario 4 — Precarga completa en Wi-Fi (SC-008)

1. Con Wi-Fi, empezar un episodio de ~60 min y dejarlo reproducir 1 minuto.
2. Esperar a que la precarga termine (indicador o logs
   `EpisodePrefetch: completed bytes=...`).
3. Activar modo avión y saltar al final.
4. **Esperado**: el episodio se reproduce de principio a fin sin red.

## Escenario 5 — Datos móviles acotados (SC-005, SC-009)

1. Desactivar Wi-Fi y usar datos móviles.
2. Reproducir un episodio nuevo.
3. **Esperado**: la precarga no supera 5 min por delante del punto de
   reproducción; la caché total no supera 50 MB.
4. Cambiar a Wi-Fi a mitad: la precarga completa se reanuda sin cortar el audio.
5. Volver a móvil: la caché se recorta a 50 MB por LRU.

## Escenario 6 — Ajustes y liberación (SC-005)

1. Abrir Ajustes → «Caché de episodios».
2. **Esperado**: muestra el espacio ocupado y los límites 500/50 MB según la red.
3. Pulsar «Liberar espacio».
4. **Esperado**: el tamaño vuelve a 0 y la reproducción en curso no se corta.

## Regresión

- La radio sigue arrancando y reconectando (spec 0042/0044).
- Las descargas existentes siguen funcionando y no se duplican en caché.
- Cast sigue emitiendo sin usar la caché local.
- Los ajustes de buffer de radio no cambian de valores.

## Observabilidad

- `EpisodePrefetch`: `start`, `progress`, `completed`, `cancelled`.
- `EpisodeCache`: `hit`, `miss`, `evicted`, `cleared` (sin URLs firmadas ni PII).
- `EpisodePlayback`: se mantiene el tag existente para errores.

## Resultado de la validación manual (2026-11-22)

Emulador `Medium_Phone` (API 37) contra una instancia local de TolochaRadio
(`adb reverse`), con Radio Ambulante suscrito desde la API.

| Escenario | Observado |
|-----------|-----------|
| Buffer VOD | `buffered position` de 258 s (~4,3 min) en el primer episodio. |
| Precarga Wi-Fi | Episodio de 49:36 completo en caché: 46,7 MB en ~20 s. |
| Modo avión | 40 s sin red; posición 74,2 s → 112,2 s y estado `PLAYING`. |
| Cambio a móvil | Caché 46,7 MB → 4,7 MB (LRU al límite de 50 MB); segundo episodio con 259 s de buffer. |
| Ajustes | «Ocupa 4,6 MB», límites 500/50 MB y «Liberar espacio» → 0 KB. |
| Defecto encontrado | Las stats mostraban 45,6 MB tras el desalojo; corregido con refresco cada 2 s mientras Ajustes está visible. |

Cast y latencia de radio confirmados por el usuario (2026-11-22). El resto de
escenarios del quickstart quedan cubiertos por tests unitarios y por esta pasada
manual.
