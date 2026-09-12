# Bug Assessment: Controles de Cast (pausa/play) y flash del formulario al arrancar

- **Slug**: 0027-jlizquierdo-20260912-cast-controls-startup-flash
- **Created**: 2026-09-12
- **Source**: reporte del usuario (dispositivo físico)
- **Verdict**: valid
- **Severity**: medium

## Report (verbatim or summarized)

> cuando reproduce [en Cast], no aparece nunca el botón pause para pausarla, simplemente sale el circulo girando. Estaría bien que se pudiera controlar el pause y el play desde el móvil.
>
> cuando inicia la aplicación, se ve la pantalla de nuevo servidor brevemente durante menos de un segundo cuando ya existe uno configurado.

## Symptom

1. Con Cast reproduciendo, el mini-player muestra el spinner de buffering indefinidamente y no aparece el botón de pausa; no se puede pausar/reanudar desde el móvil.
2. Al abrir la app con un servidor ya configurado, se ve el formulario de servidor durante <1 s antes de la pantalla real.

## Reproduction

1. Conectar a Cast, reproducir una emisora: el panel muestra spinner y nunca Pause.
2. Cerrar y abrir la app con un servidor guardado: parpadea el formulario de servidor.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` — `updateCastState()` derivaba el estado del `activeStationHolder` (que se queda en BUFFERING durante Cast) en vez del `CastPlayer`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` — `toggle()`/`cancelLoad()` usaban `_state` (local, congelado en Buffering) y el listener de ExoPlayer podía pisar el estado durante Cast.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` — el botón principal usaba `viewModel.state`, no el estado de Cast.
- `app/src/main/java/com/izquierdojl/tolocharadio/MainActivity.kt` — `collectAsState(initial = emptyList())` de Room hacía que el gate fuese `NoServers` en la primera composición.

## Root Cause Hypothesis

**Confidence: high**

1. El estado de reproducción de Cast nunca se leía del `CastPlayer`; `_state`/holder se quedaban en `Buffering`, así que la UI mostraba el spinner y el toggle no tenía efecto.
2. La lista de servidores de Room emite de forma asíncrona; con `initial = emptyList()` el `StartupGate` arrancaba en `NoServers` y el `NavHost` iniciaba en el formulario antes de la primera emisión.

## Proposed Remediation

**Preferred**:
1. `CastPlayerManager.updateCastState()` deriva del `CastPlayer` (READY + `isPlaying` → Playing/Paused). `PlayerViewModel` usa un "estado efectivo" (Cast si hay sesión) en `toggle()`/`cancelLoad()`, ignora eventos de ExoPlayer durante Cast y la UI usa el estado de Cast. El slider de volumen controla el player activo.
2. `MainActivity` no renderiza el grafo hasta tener los servidores (`initial = null` + fondo neutro).

**Files likely to change**:
- `CastPlayerManager.kt`, `PlayerViewModel.kt`, `PlayerUi.kt`, `MainActivity.kt`
- `PlayerViewModelTest.kt`

**Tests to add or update**:
- `toggle` pausa/reanuda el `CastPlayer` con estado efectivo Playing/Paused.

## Risks & Considerations

- El modo de reproducción local no debe verse afectado (se mantiene el flujo por ExoPlayer).
- Errores de Cast siguen sin mostrarse en UI (`onPlayerError` del CastPlayer no tratado).

## Open Questions

- [NEEDS CLARIFICATION: ¿mostrar también errores de Cast en el panel?]
