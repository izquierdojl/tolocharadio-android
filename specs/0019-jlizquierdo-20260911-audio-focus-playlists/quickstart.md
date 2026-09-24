# Quickstart: Reproducción exclusiva y soporte de listas m3u/m3u8/pls

**Feature**: `0019-jlizquierdo-20260911-audio-focus-playlists` | **Date**: 2026-09-11

Guía de validación end-to-end. No contiene implementación; referencia `spec.md`, `data-model.md` y `contracts/`.

## Prerrequisitos

- JDK 17 (`JAVA_HOME` apuntando al JBR de Android Studio o Temurin 17).
- Android SDK con `compileSdk 37`; dispositivo o emulador con Android 8+ (API 26).
- `local.properties` con `tolocha.baseUrl` apuntando a una instancia TolochaRadio accesible.
- Cuenta de la app (los streams normales van por proxy autenticado).
- Para foco: una segunda app de audio (VLC, Pocket Casts o Foobar) en el mismo dispositivo.
- Emisoras de prueba: al menos una `.m3u8`, una `.m3u`, una `.pls` HTTPS y una con lista inválida (se pueden crear como "Mis emisoras").

## Comandos

```bash
# Compilación + tests unitarios + calidad estática (mismos gates que CI)
./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug

# Solo los tests de esta feature
./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.domain.playback.*" \
  --tests "com.izquierdojl.tolocharadio.feature.player.PlaylistPlaybackQueueTest" \
  --tests "com.izquierdojl.tolocharadio.feature.player.PlayerAudioConfigTest" \
  --tests "com.izquierdojl.tolocharadio.feature.player.PlayerViewModelTest"

# Instalar en dispositivo conectado
./gradlew installDebug
```

## Escenario 1 — Foco de audio (US1, FR-001..005, SC-002..004, SC-007)

1. Reproducir una emisora en TolochaRadio.
2. Abrir VLC/Pocket Casts y empezar a reproducir.
   **Esperado**: TolochaRadio pasa a `Paused` en ≤ 2 s; no se oye mezcla (SC-002).
3. Con VLC sonando, pulsar play en TolochaRadio.
   **Esperado**: VLC se detiene/pausa; solo suena la radio (SC-003).
4. Reproducir TolochaRadio, provocar una interrupción transitoria (llamada simulada o asistente) y dejar que termine.
   **Esperado**: la radio se pausa y reanuda sola (FR-003). Repetir hasta **20 interrupciones** y anotar el resultado: ≥ 19/20 reanudan (SC-004).
5. Repetir el paso 4 pausando manualmente durante la interrupción.
   **Esperado**: al terminar NO reanuda (FR-003, SC-004).
6. Repetir 1-3 con la app en background y con botones de auriculares/Bluetooth.
   **Esperado**: mismo comportamiento (FR-005).
7. Desconectar auriculares con la radio sonando.
   **Esperado**: pausa.
8. Activar modo silencio / No molestar y reproducir.
   **Esperado**: la app no modifica la configuración de sonido del sistema; la reproducción se rige por el stream de medios y el volumen del dispositivo.
9. Encadenar ≥ 10 ciclos de pérdida/recuperación de foco en una misma sesión de escucha.
   **Esperado**: sin cierres, bloqueos ni comportamiento errático (SC-007).

## Escenario 2 — Emisora `.m3u8` (US2, FR-006, SC-001)

1. Añadir/abrir una emisora cuyo enlace termina en `.m3u8` (HTTPS).
2. Pulsar play.
   **Esperado**: comienza a sonar de forma continua; no aparece error; controles y notificación funcionan.

## Escenario 3 — Emisora `.m3u` y `.pls` (US2, FR-007/FR-008, SC-001)

1. Reproducir una emisora `.m3u` con una entrada HTTPS.
   **Esperado**: suena la entrada indicada.
2. Repetir con una `.pls` con `File1`/`File2`.
   **Esperado**: suena `File1`; si `File1` está caído, se intenta `File2` (máx. 3 intentos) y, si ninguna suena, error con reintento.
3. Repetir con una emisora personalizada de "Mis emisoras" con enlace de lista.
   **Esperado**: mismo comportamiento que catálogo (FR-008).

## Escenario 4 — Errores de lista (FR-009/FR-013, SC-006)

| Caso | Preparación | Esperado |
|------|-------------|----------|
| Lista vacía / sin entradas | `.m3u` solo con comentarios | Mensaje "no contiene ninguna emisión reproducible" + reintento |
| Solo HTTP | `.m3u` con `http://...` | Mensaje de conexión no segura bloqueada |
| Texto inválido | `.txt` con basura renombrado a `.m3u` | Mensaje de formato no válido |
| Sin red | Activar modo avión y reproducir lista | Mensaje de red + reintento |

Ninguno debe provocar cierre inesperado (SC-006).

## Escenario 5 — No regresión (FR-011, SC-005)

1. Reproducir varias emisoras directas (MP3/AAC) del catálogo.
   **Esperado**: suenan igual que antes, vía proxy autenticado; el historial se registra.
2. Pausar/reanudar, silenciar, temporizador de apagado y Cast siguen funcionando.
3. Con Cast conectado, reproducir una emisora HLS y una de lista.
   **Esperado**: el receptor reproduce la URL resuelta; al desconectar, la reproducción local se reanuda con la misma fuente.

## Criterios de "hecho"

- [X] `testDebugUnitTest`, `detekt`, `ktlintCheck` y `lintDebug` en verde.
- [X] Escenarios 1-5 verificados en dispositivo/emulador.
- [X] Sin regresiones en emisoras directas (SC-005).
- [X] Deuda registrada: historial server-side para emisoras de lista (Complexity Tracking del plan). **Saldada por la spec 0021** (las listas vuelven a reproducirse por el proxy autenticado, restituyendo el historial server-side).
