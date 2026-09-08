# Research: Temporizador de apagado (Sleep Timer)

**Date**: 2026-09-08
**Feature**: 014-sleep-timer

## Decisiones de diseño

### 1. Implementación del temporizador con corrutinas

**Decision**: Usar `kotlinx.coroutines.delay()` dentro de un `UseCase` para el countdown.

**Rationale**:
- El proyecto ya usa corrutinas + Flow en toda la capa de dominio (constitución II)
- `delay()` es la forma estándar de temporizar en Kotlin sin bloquear el hilo
- No requiere dependencias adicionales (`AlarmManager`, `WorkManager`, etc.)
- El temporizador es transitorio (no persiste entre sesiones), por lo que no necesita supervivencia a cierre de app

**Alternatives considered**:
- `AlarmManager`: innecesario para temporizadores de <2h, más complejo, requiere permisos
- `Handler.postDelayed()`: API legacy, no suspendable, difícil de cancelar
- `WorkManager`: diseñado para trabajo diferido persistente, excesivo para un timer local

### 2. Arquitectura: UseCase dedicado vs. integrar en PlayerViewModel

**Decision**: Crear `SleepTimerUseCase` en `domain/` y `SleepTimerViewModel` en `feature/player/`.

**Rationale**:
- Separa responsabilidades: el timer es lógica de negocio independiente del player
- Permite testear el UseCase sin dependencias de Android (constitución III)
- El UseCase expone un `Flow<SleepTimerState>` que el ViewModel observa
- El ViewModel orquesta: cuando el timer expira, llama a `PlayerViewModel.stop()`

**Alternatives considered**:
- Integrar directamente en `PlayerViewModel`: violaría separación de capas y haría el VM más complejo
- Singleton con `StateFlow`: evitaría el UseCase pero dificultaría el testing

### 3. Ubicación del botón en la UI

**Decision**: Añadir un `SleepTimerButton` composable en las `actions` del `TopAppBar` de `TolochaNavGraph.kt`.

**Rationale**:
- El usuario especificó "botón típico en la barra superior"
- El TopAppBar ya tiene `actions` con Cast, ViewModeToggle y Servers
- El botón debe ser visible en todas las pantallas con chrome (igual que Cast/Servers)

**Alternatives considered**:
- Dentro del mini-player: menos accesible, no visible en todas las pantallas
- En el menú de ajustes: demasiado oculto para un uso frecuente
- Floating Action Button: no es el patrón estándar para sleep timer

### 4. Estado del temporizador: ViewModel dedicado vs. compartido

**Decision**: `SleepTimerViewModel` a ámbito de Activity (igual que `PlayerViewModel`).

**Rationale**:
- El timer debe persistir al navegar entre secciones (spec US-4)
- Comparte el mismo patrón que `PlayerViewModel` y `ViewModeViewModel`
- `hiltViewModel(LocalContext.current as ComponentActivity)` reutiliza la misma instancia

**Alternatives considered**:
- Scope de NavBackStackEntry: se destruiría al navegar
- Singleton en Hilt: dificultaría el testing y el ciclo de vida

### 5. Indicador visual del temporizador activo

**Decision**: Badge con tiempo restante sobre el icono del reloj.

**Rationale**:
- Material3 soporta `BadgedBox` nativamente
- Muestra información útil (tiempo restante) sin abrir el menú
- Patrón estándar en apps de radio (Spotify, Pocket Casts)

**Alternatives considered**:
- Cambio de icono (clock → clock con check): menos informativo
- Animación de pulso: distractiva para una función de sueño
