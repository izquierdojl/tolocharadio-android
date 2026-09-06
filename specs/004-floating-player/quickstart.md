# Quickstart: Reproductor flotante inferior (004-floating-player)

**Spec**: [spec.md](spec.md) | **Contrato UI**: [contracts/mini-panel-ui.md](contracts/mini-panel-ui.md) | **Modelo**: [data-model.md](data-model.md)

Guía de validación manual + automática. Requisito: instancia configurada y una cuenta con favoritas (ver `specs/003-favorites-management/quickstart.md` si hace falta).

## 1. Tests automáticos

```powershell
.\gradlew :app:testDebugUnitTest --tests "com.example.tolocharadio.feature.player.*"
```

Esperado: en verde `PlayerViewModelTest` ampliado (mute on/off, reset de mute en `play()`/`stop()`, `cancelLoad()` → `Idle`) y tests de `panelSubtitle` / `resolveCopyLink` (subtítulo con datos parciales/ausentes; URL en blanco → `null`).

> Nota: los Compose Test siguen la limitación conocida del entorno (API 37 incompatible con `compose-ui-test`, igual que specs 002/003): el recorrido visual se verifica manualmente en emulador según §2.

## 2. Recorrido manual en emulador (15 min)

1. **Panel global**: reproducir desde Favoritos → navegar por Explorar → Historial → Mis emisoras → Perfil. Esperado: audio continuo, panel encima de la barra inferior con avatar, nombre y `país · idioma · codec bitrate` (SC-001/SC-004).
2. **Play/pausa + mute**: pausar y reanudar (icono cambia); silenciar (icono `VolumeOff`, silencio inmediato) y quitar silencio (sonido al instante, sin reconexión) (SC-002/SC-005).
3. **Copiar enlace**: pulsar copiar → snackbar `"Enlace copiado"` → pegar en otra app → es la URL original de la emisora (SC-003). Con emisora sin URL → `"enlace no disponible"` y no copia nada.
4. **Cancelar carga**: con red lenta, pulsar el botón principal durante la carga → el intento se cancela y el panel se oculta.
5. **Error**: forzar emisora no disponible → mensaje breve en ES + botones reintentar + copiar (sin mute).
6. **Degradación**: emisora sin imagen → avatar Sierra; sin técnicos → `"Emisora de radio"` (SC-006).
7. **Reset de mute**: silenciar → reproducir otra emisora → vuelve con sonido; reiniciar app → con sonido.
8. **Full-player**: tocar la zona izquierda → se abre el completo con la misma emisora y `Detener` (oculta el panel). Rotar: estado intacto.

## 3. Calidad estática (gate de merge)

```powershell
.\gradlew lintDebug ktlintCheck detekt
```

Esperado: sin errores (constitución III).
