# Research: Continuidad de reproducción y buffer ajustable

**Feature**: `0042-izquierdojl-20261031-audio-playback-recovery`
**Date**: 2026-10-31

Este documento resuelve las decisiones técnicas de la fase 0. No quedan
`NEEDS CLARIFICATION`.

## R1. Perfiles de buffer y valores de `DefaultLoadControl`

**Decision**: usar tres perfiles con estos valores (milisegundos):

| Perfil | `minBufferMs` | `maxBufferMs` | `bufferForPlaybackMs` | `bufferForPlaybackAfterRebufferMs` |
|---|---:|---:|---:|---:|
| Ahorro de datos | 10 000 | 30 000 | 1 000 | 2 000 |
| Equilibrado | 20 000 | 50 000 | 2 000 | 4 000 |
| Máxima estabilidad | 40 000 | 60 000 | 6 000 | 7 000 |

**Rationale**: Media3 1.11 usa por defecto, para reproducción local, un mínimo
de 1 s, un máximo de 50 s, 1 s para empezar y 1 s tras un rebuffer. El problema
reportado no se explica solo por el máximo: el arranque y la reanudación tras un
corte son demasiado sensibles. "Equilibrado" conserva el máximo por defecto
(50 s) y sube el mínimo y el umbral de rebuffer; "Máxima estabilidad" prioriza
absorber cortes con 40 s de mínimo y 60 s de máximo; "Ahorro de datos" reduce el
máximo a 30 s y el mínimo a 10 s para disminuir memoria y datos iniciales.
"Máxima estabilidad" añade 4 s al arranque frente a "Equilibrado" (6 s vs 2 s),
dentro del límite de 5 s acordado, y su `afterRebuffer` de 7 s permite cumplir
la reanudación en menos de 10 s.

**Alternatives considered**:
- Subir solo `maxBufferMs`: no mejora el arranque ni la reanudación inmediata.
- Permitir valores numéricos al usuario: más superficie de error y peor UX.
- Un único perfil global: no responde a la petición de ajuste por el usuario.

**Note**: en un directo, un buffer mayor no multiplica el consumo de datos en
régimen estable; influye sobre todo en memoria, ráfaga inicial y tolerancia a
cortes. Los textos de UI evitarán prometer un ahorro de datos que no sea el
inicial.

## R2. Aplicación del perfil en caliente

**Decision**: recrear el `ExoPlayer` con un nuevo `DefaultLoadControl` y
sustituirlo en la `MediaSession` con `MediaSession.setPlayer(newPlayer)`.

**Rationale**: `ExoPlayer` 1.11 no expone `setLoadControl` después de `build()`.
Un wrapper de `LoadControl` que cambie de delegado en caliente obliga a compartir
el `DefaultAllocator` y a gestionar estados internos de carga, lo que es frágil
ante actualizaciones de Media3. `MediaSession.setPlayer` existe en 1.11 y
permite mantener la sesión y la notificación mientras se cambia el player. El
usuario eligió aplicación inmediata con una reconexión y aviso de corte breve.
`PlaybackEngine.ensureProfile` cubre el caso de un cambio de perfil mientras el
servicio no está activo: al arrancar, recrea el player antes de crear la sesión.

**Alternatives considered**:
- Reiniciar la app o pedir al usuario que lo haga: incumple la decisión de
  clarify y la experiencia esperada.
- Wrapper de `LoadControl`: riesgo de allocator y estados inconsistentes.
- Refactor completo a `MediaController` en la UI: mayor alcance y regresiones.

**Secuencia de swap**: detener y limpiar el player anterior antes de
`session.setPlayer(newPlayer)` evita que el audio antiguo siga sonando fuera de
la sesión; después se prepara la emisora en el player nuevo y se libera el
anterior. El corte breve es el aceptado en clarify.

**Lifecycle**: el player del engine es process-scoped. `RadioPlaybackService`
no debe llamar `release()` en `onDestroy` porque la instancia se reutiliza al
recrear el servicio; debe llamar `stop()`/`clearMediaItems()`, que liberan
codecs y buffers de reproducción. La instancia se libera con el proceso. Esto
preserva el objetivo de rendimiento de la constitución sin invalidar el player
compartido.

## R3. Dónde vive la recuperación

**Decision**: `PlaybackRecoveryManager` singleton con un scope de aplicación en
el hilo principal (`SupervisorJob() + Dispatchers.Main.immediate`), instanciado
por `RadioPlaybackService` y observado por `PlayerViewModel`.

**Rationale**: un `ViewModel` se destruye al cerrar la Activity; la reproducción
sigue en el servicio. Si la recuperación viviera en el ViewModel, no funcionaría
con la pantalla apagada o la app en segundo plano, incumpliendo FR-018. El
manager mantiene el watchdog y los reintentos; el ViewModel solo refleja el
estado y ofrece acciones. ExoPlayer debe tocarse en su looper principal, de ahí
que el scope sea Main y no el `ApplicationScope` de trabajo en background.

**Alternatives considered**:
- Watchdog en `PlayerViewModel`: no sobrevive al background.
- Watchdog en el servicio con lógica de UI: acopla servicio y presentación.
- WorkManager: innecesario para un proceso de reproducción activo.

## R4. Detección de silencio/stall

**Decision**: watchdog de posición cada segundo, con dos umbrales:

- **Tras la primera reproducción**: 5 s sin que avance `currentPosition`
  mientras `playWhenReady` es true y no hay Cast.
- **Carga inicial**: 10 s sin alcanzar `STATE_READY`/`isPlaying` desde que se
  pidió la reproducción.

Se ignoran `STATE_IDLE`, `STATE_ENDED`, pausa manual, pérdida de foco de audio,
`becoming noisy` y Cast. `onPlayerError` dispara la recuperación de inmediato,
sin esperar al watchdog.

**Rationale**: `Player.Listener` no emite un evento de "sigue en READY pero no
suena"; el avance de posición es la señal más fiable y barata. El umbral
separado de carga inicial evita reintentar una emisión que simplemente tarda en
arrancar en una red lenta.

**Alternatives considered**:
- Solo `onIsPlayingChanged`: no detecta un stream congelado que sigue READY.
- Métricas de `AudioTrack`: baja nivel, complejas y menos portables.
- Umbral único de 5 s para todo: falsos positivos en el primer arranque.

## R5. Conectividad

**Decision**: `ConnectivityMonitor` con `registerDefaultNetworkCallback` y
`StateFlow<Boolean>`, usando `NET_CAPABILITY_INTERNET` y
`NET_CAPABILITY_VALIDATED`. Sin red: estado `WaitingForNetwork` y cero
reintentos; al volver: reintento inmediato si el ajuste de reanudación está
activo.

**Rationale**: el sistema ya concede `ACCESS_NETWORK_STATE`; el callback evita
polling y permite reaccionar en el mismo instante en que la red vuelve. Esto
cumple FR-014/FR-015 y SC-007.

**Alternatives considered**:
- Polling periódico: coste de batería y latencia.
- Detectar la red solo por errores del player: tarde y sin distinguir "sin red"
  de "emisora caída".

## R6. Errores de credenciales

**Decision**: extraer el código HTTP de la causa de `PlaybackException`; ante
401/403 llamar a `AuthenticateServerUseCase` (refresh y, si falla, re-login) y
reintentar una vez. Si la autenticación falla, estado `Error` con acción
`EDIT_SERVER`.

**Rationale**: `PlayerDataSourceFactory` usa un `OkHttpClient()` sin
autenticador; el player no renueva tokens por sí solo. La app ya tiene un caso de
uso que asegura la sesión del servidor de arranque. Sin esto, un token caducado
se convertiría en un bucle de reintentos inútil.

**Alternatives considered**:
- Confiar en el interceptor de Retrofit: el player no usa ese cliente.
- Reintentar 401/403 con backoff: desperdicia tiempo y datos.
- Pedir credenciales al usuario: prohibido por la constitución.

## R7. Persistencia

**Decision**: ampliar `InstancePrefs` con:

- `buffer_profile`: `DATA_SAVER` | `BALANCED` | `MAX_STABILITY`, default
  `BALANCED`, `valueOf` protegido con fallback.
- `resume_on_connectivity`: booleano, default `true`.

**Rationale**: DataStore ya es la fuente de ajustes locales y no requiere
migración Room. El patrón de fallback ante valores corruptos ya existe en
`viewMode` y `themeMode`.

**Alternatives considered**:
- Room: sobredimensionado para dos valores escalares.
- SharedPreferences: stack ya decidido a favor de DataStore.
- Sincronizar con el servidor: el ajuste es del dispositivo, no del usuario.

## R8. Estados y contratos de UI

**Decision**:

- `PlayerState.Reconnecting(station, attempt, maxAttempts, waitingForNetwork)`.
- `PlayerState.Error(station, message, action)` con
  `PlayerErrorAction.RETRY` o `EDIT_SERVER`.
- Mini-player: subtítulo "Reconectando (n/5)…" o "Sin conexión…", spinner y
  acción contextual.
- Reproductor completo: mensaje y botón "Reintentar" o "Editar servidor".
- Notificación: comando `CUSTOM_COMMAND_RETRY` y botón visible solo en error.

**Rationale**: la constitución exige estados sellados con mensaje accionable y
botón de reintento; el caso de credenciales requiere una acción distinta. La
notificación ya usa comandos personalizados para stop/mute, así que el patrón es
reutilizable.

**Alternatives considered**:
- Reutilizar `Buffering` con un texto distinto: impide distinguir "cargando" de
  "recuperando" y complica los tests.
- Un diálogo global de error: interrumpe la navegación y no funciona en
  segundo plano.

## R9. Observabilidad

**Decision**: logs con tag `PlaybackRecovery` y mensajes estructurados:
`event=recovery_attempt cause=... attempt=... outcome=... durationMs=...
station=<hash>`. Sin título de emisora, URL, token ni credenciales.

**Rationale**: cumple FR-021 y la constitución IV; permite diagnosticar sin
introducir telemetría invasiva ni PII. El `stationId` se transforma con un hash
estable no reversible.

**Alternatives considered**:
- Loggear la URL o el id completo: puede exponer datos de la emisora/servidor.
- Añadir analytics: fuera de alcance y de la política de privacidad.

## R10. Estrategia de pruebas

**Decision**:

- Dominio: tests puros de `BufferProfileSpec` y `RecoveryPolicy` (límites,
  backoff, timeout, clasificación, pausa manual).
- Datos: `InstancePrefsTest` con `FakeDataStore` para defaults, round-trip y
  corrupción.
- Motor: `PlaybackEngineTest` con una fábrica falsa y `ExoPlayer` mockeado para
  verificar `swap`, `playerFlow` y liberación del anterior.
- ViewModels: `SettingsViewModelTest` y `PlayerViewModelTest` con Turbine/MockK
  para persistencia, estados `Reconnecting`, error con acción y reintento.
- UI: Compose tests de la sección "Reproducción" y de los estados del panel.
- Manual: quickstart con cortes de red, modo avión, cambio de perfil, segundo
  plano y 401/403.

**Rationale**: ExoPlayer real no es fiable en JVM; la lógica crítica se extrae a
dominio y el resto se cubre con mocks y pruebas instrumentadas. Cumple la regla
Red-Green y los gates de la constitución.

**Alternatives considered**:
- Robolectric: no está configurado y añade dependencia.
- Solo pruebas manuales: incumple la constitución III.
- Tests instrumentados de red real: lentos y no deterministas.
