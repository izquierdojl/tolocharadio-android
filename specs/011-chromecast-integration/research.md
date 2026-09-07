# Research: Chromecast Integration

**Feature**: 011-chromecast-integration
**Date**: 2026-09-07
**Status**: Complete

## Research Areas

### 1. Media3 Cast Module vs Cast SDK Directo

**Decision**: Usar `media3-cast` (módulo oficial de Media3)

**Rationale**:
- La app ya usa Media3 ExoPlayer + MediaSessionService
- `media3-cast` proporciona `CastPlayerImpl` que implementa la interfaz `Player` de Media3
- Permite usar el mismo patrón de delegación que ya existe en el PlayerViewModel
- El switching local↔remoto se maneja con `updateActivePlayer()` que transfiere estado automáticamente
- Evita duplicar lógica de control de reproducción

**Alternatives considered**:
- **Cast SDK directo (sin Media3)**: Requeriría crear un wrapper manual del `RemoteMediaClient` para adaptarlo a la interfaz `Player`. Más código, más mantenimiento, más propenso a bugs.
- **ExoPlayer Cast extension (deprecated)**: La extensión antigua de ExoPlayer para Cast está deprecated en favor de `media3-cast`.

**Key findings**:
- `CastPlayerImpl.updateActivePlayer()` gestiona el switching automáticamente cuando una sesión Cast se conecta/desconecta
- `TransferCallback.transferState()` copia el estado del player fuente al destino antes del swap
- El player anterior se detiene (`previousPlayer.stop()`) después de la transferencia

---

### 2. CastOptionsProvider Configuration

**Decision**: Implementar `OptionsProvider` con Default Media Receiver y media session desactivada

**Rationale**:
- Default Media Receiver (`CC1AD845`) no requiere registro en Google
- La media session del SDK Cast debe desactivarse para evitar duplicados con la MediaSession existente de la app
- Las notificaciones del SDK Cast también se desactivan (ya tenemos las nuestras via MediaSessionService)

**Configuration**:
```kotlin
CastOptions.Builder()
    .setReceiverApplicationId("CC1AD845")
    .setCastMediaOptions(
        CastMediaOptions.Builder()
            .setMediaSessionEnabled(false)
            .setNotificationOptions(null)
            .build())
    .build()
```

**Alternatives considered**:
- **Custom Receiver**: Requeriría registro en Google Cast Developer Console. No necesario para app personal/educativa.
- **Media session activa del SDK Cast**: Crearía notificaciones duplicadas con la MediaSession existente.

---

### 3. Player Switching Architecture

**Decision**: Crear un `CastPlayerManager` que encapsule la lógica de switching entre ExoPlayer local y CastPlayer remoto

**Rationale**:
- El PlayerViewModel actual usa directamente `ExoPlayer` (singleton inyectado)
- Con Cast, necesitamos un player delegado que pueda ser local o remoto
- `CastPlayerImpl` de Media3 ya hace esto internamente, pero necesitamos integrarlo con nuestro Hilt DI
- El MediaSession en `RadioPlaybackService` debe usar el mismo player delegado

**Architecture**:
```
PlayerViewModel
    └── CastPlayerManager (nuevo, @Singleton)
        ├── ExoPlayer (local, ya existente)
        ├── CastPlayer (remoto, creado al conectar)
        └── MediaSession.setPlayer(activePlayer) ← actualización dinámica
```

**Key flow**:
1. Usuario toca Cast → selecciona dispositivo
2. CastPlayerManager crea CastPlayer con la sesión Cast
3. Transfiere estado de ExoPlayer a CastPlayer
4. Actualiza MediaSession para usar CastPlayer
5. ExoPlayer se detiene

**Reverso (desconexión)**:
1. CastPlayerManager detecta desconexión
2. Transfiere estado de CastPlayer a ExoPlayer
3. Prepara ExoPlayer con la misma emisora
4. Actualiza MediaSession para usar ExoPlayer
5. CastPlayer se libera

---

### 4. MediaRouteButton en Compose

**Decision**: Usar `AndroidView` interop para integrar `MediaRouteButton` en el TopAppBar de Compose

**Rationale**:
- El SDK Cast proporciona `MediaRouteButton` como View nativa de Android
- No hay wrapper oficial de Compose para MediaRouteButton
- `AndroidView` es el patrón estándar para integrar Views legacy en Compose
- `CastButtonFactory.setUpMediaRouteButton()` configura el botón automáticamente

**Implementation pattern**:
```kotlin
AndroidView(factory = { ctx ->
    MediaRouteButton(ctx).apply {
        CastButtonFactory.setUpMediaRouteButton(ctx, this)
    }
})
```

**Alternatives considered**:
- **Custom Compose button**: Reimplementar toda la lógica de discovery y selección de dispositivos. Mucho trabajo, propenso a bugs.
- **Accompanist wrapper**: No existe wrapper oficial de Accompanist para Cast.

---

### 5. Authentication & Stream URL for Cast

**Decision**: El Chromecast recibe la URL del stream directamente, sin autenticación Bearer

**Rationale**:
- El precheck de `playable` se ejecuta en el dispositivo local antes de enviar al Cast
- La URL del stream (`/api/v1/stream/:id`) puede no requerir autenticación para reproducción
- Si la URL requiere auth, el CastPlayer de Media3 soporta custom DataSources

**Fallback**: Si la URL del stream requiere autenticación, se puede usar un `ResolvingDataSource` que añada el token Bearer. Esto se implementaría en el `AuthDataSourceFactory` existente.

---

### 6. Notification & MediaSession Integration

**Decision**: Desactivar media session y notificaciones del SDK Cast, mantener las existentes

**Rationale**:
- La app ya tiene `RadioPlaybackService` con MediaSession y notificaciones ricas
- El SDK Cast intenta crear su propia media session y notificaciones → duplicados
- Al desactivarlas en `CastOptionsProvider`, mantenemos control total
- La MediaSession existente funciona igual con CastPlayer que con ExoPlayer (misma interfaz `Player`)

**Key**: `MediaSession.setPlayer()` permite cambiar el player subyacente dinámicamente. Cuando el CastPlayer se activa, la MediaSession lo refleja automáticamente en las notificaciones.

---

### 7. Dependencies to Add

**Decision**: Añadir `media3-cast` y `play-services-cast-framework`

**Rationale**:
- `media3-cast:1.4.1` — misma versión que los otros módulos Media3 existentes
- `play-services-cast-framework` — SDK de Google Cast (discovery, conexión, UI)

**Version compatibility**:
- `media3-cast` debe coincidir con la versión de `media3-exoplayer` (1.4.1)
- `play-services-cast-framework` es independiente de la versión de Media3
