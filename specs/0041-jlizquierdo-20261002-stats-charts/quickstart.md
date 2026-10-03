# Quickstart: validación de las gráficas de escucha

**Feature**: `0041-jlizquierdo-20261002-stats-charts` | **Date**: 2026-10-02

Guía de validación end-to-end. Los detalles de datos y contrato viven en `data-model.md` y `contracts/stats-api.md`.

## Prerrequisitos

- Entorno de desarrollo del repo (Android SDK, JDK 17; ver `README.md`).
- Un servidor TolochaRadio compatible (`/api/v1`) configurado en la app **con credenciales** (email/contraseña en el formulario de servidor) y con historial de escucha previo (reproducir alguna emisora vía proxy lo genera).
- Dispositivo o emulador con la app instalada (`.\gradlew.bat installDebug`).

## Comandos de verificación

```powershell
.\gradlew.bat assembleDebug            # compila
.\gradlew.bat testDebugUnitTest        # tests unitarios (domain/data/ViewModel/Format)
.\gradlew.bat detekt ktlintCheck lintDebug   # gates de calidad (CI)
.\gradlew.bat connectedDebugAndroidTest      # flujo de UI (requiere dispositivo)
```

Resultado esperado: todos en verde; `StatsRepoTest`, `ComputeStatsSummaryUseCaseTest`, `StatsFormatTest`, `StatsViewModelTest` y `StatsEntryFlowTest` presentes.

## Escenarios de validación manual

1. **Entrada y carga (FR-001/FR-002, SC-001/SC-002)** — Abrir Configuración y tocar la fila "Gráficas", junto a "Acerca de". Se abre la vista con periodo "30 días" y los 7 bloques con datos del usuario del servidor activo en menos de 2 s.
2. **Cambio de periodo (FR-003/FR-004, SC-004)** — Cambiar a "7 días": todos los bloques se recalculan y ningún bloque conserva datos del periodo anterior. Con "Todo", la evolución temporal agrupa por semana o mes (según amplitud).
3. **Paridad de datos (SC-003)** — Con el mismo usuario y periodo, comparar los valores (tiempo total, emisora destacada, día con más escucha, top, géneros, países, hábitos, recientes) con la página `/estadisticas` de la web sobre el mismo servidor: coinciden.
4. **Estado vacío (US1-3, SC-006)** — Con un periodo sin escuchas (p. ej. un servidor nuevo o un rango sin actividad), la vista muestra estado vacío que invita a reproducir una emisora; sin ceros engañosos ni errores.
5. **Estado de error y reintento (FR-007)** — Poner el dispositivo en modo avión y abrir/recargar la vista: mensaje de error en español con "Reintentar". Restaurar la red y reintentar carga los datos.
6. **Error de credenciales (principio IV)** — Con credenciales inválidas en el servidor activo (o sesión no recuperable), la vista muestra el error y la acción conduce a la edición del servidor activo.
7. **Cambio de servidor (FR-005)** — Cambiar de servidor activo y volver a la vista: se muestran los datos del nuevo servidor.
8. **Legibilidad (FR-008/FR-009)** — En un móvil real, todos los bloques se leen sin desplazamiento horizontal; tiempos en formato "N min / H h / H h M min".

## Cobertura automatizada mínima

| Test | Cubre |
|---|---|
| `StatsRepoTest` | mapeo de respuestas y de errores del contrato (401/400/red) |
| `ComputeStatsSummaryUseCaseTest` | resumen derivado: total, emisora destacada, día pico, caso sin datos |
| `StatsFormatTest` | formatos de tiempo y etiquetas de bucket en español |
| `StatsViewModelTest` | transiciones `Loading/Empty/Content/Error`, cambio de periodo, reintento, `isAuthError` |
| `StatsEntryFlowTest` | fila "Gráficas" en Configuración abre la vista; estado de error con reintento |
