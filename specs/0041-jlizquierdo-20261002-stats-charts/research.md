# Research: Gráficas de escucha (estadísticas) en la app Android

**Feature**: `0041-jlizquierdo-20261002-stats-charts` | **Date**: 2026-10-02

## R1. Renderizado de gráficas sin dependencias nuevas

- **Decision**: dibujar los bloques de gráfica con `Canvas` de Compose (barras, línea, matriz de intensidad y sectores) y estética del Tema Tolocha (verde-bosque/ocre-montaña), siguiendo la paleta de `apps/web/src/lib/stats.ts` (`CHART_COLORS`).
- **Rationale**: la constitución (V) exige justificar toda dependencia nueva; los 7 bloques son visualmente simples y ya existen patrones de composables en el proyecto; control total de dark mode, accesibilidad y tamaño de APK.
- **Alternatives considered**:
  - Vico (gráficas nativas Compose, mantenimiento activo) — descartada: dependencia nueva sin necesidad imperante; su curva de API y personalización costaría más que el Canvas propio para este alcance.
  - MPAndroidChart / librerías View-based — descartadas: el principio II prohíbe pantallas nuevas con Views/XML.
  - Listas de texto con barras Unicode — descartadas: no cumplen FR-002 ("gráficas") ni la paridad visual con la web.
- **Limits**: interacción avanzada (zoom, tooltips, selección de punto) queda fuera de alcance v1; solo lectura táctil de valores.

## R2. Sin caché local de estadísticas

- **Decision**: los datos de gráficas se consultan siempre en red; ante fallo se muestra estado de error con reintento. No se crea esquema Room ni snapshot.
- **Rationale**: son agregados que cambian con cada reproducción; el valor de lectura offline es bajo y FR-007 ya exige estado de error accionable. Evita migraciones y mappers de caché (YAGNI).
- **Alternatives considered**: caché Room tipo favoritos/historial (patrón existente) — descartada para v1; reintroducirla sería trivial si se echa en falta.
- **Limits**: sin conexión no hay gráficas (se muestra error con reintento, nunca pantalla en blanco — SC-006).

## R3. Carga del conjunto de bloques y estados de la vista

- **Decision**: un único caso de uso `LoadStatsUseCase` que consulta los 6 endpoints del periodo en paralelo (`coroutineScope` + `async`) y devuelve el conjunto; el `StatsViewModel` expone un `StatsUiState` sellado `Loading | Empty | Content | Error` y **recarga todo el conjunto** al cambiar de periodo (FR-003/SC-004). El resumen se considera parte de `Content`.
- **Rationale**: garantiza consistencia entre bloques (SC-004: ningún bloque conserva datos del periodo anterior) y un solo punto de reintento (FR-007); es el patrón `HistoryUiState` ya usado en la app.
- **Alternatives considered**:
  - Estado e independientes por bloque (como la web) — descartado para v1: más superficie de UI y de tests sin valor accionable añadido (YAGNI).
  - Carga secuencial de los 6 endpoints — descartada: latencia acumulada innecesaria frente a la paralela.
- **Limits**: si un único endpoint falla, la vista entera pasa a `Error` con reintento global.

## R4. Resumen derivado en dominio (paridad con la web)

- **Decision**: `ComputeStatsSummaryUseCase` puro (sin Android ni red) que calcula, igual que `StatsSummary.tsx`: tiempo total = suma de `totalMs` de la serie temporal; emisora destacada = primer elemento del top; día con más escucha = bucket de mayor `totalMs`. Si el total es 0 → la vista muestra estado vacío.
- **Rationale**: es regla de negocio presentable testeable aislada (principio III); replicar la web garantiza SC-003.
- **Alternatives considered**: calcular en el ViewModel — descartado: impide test unitario puro y aleja la regla de `domain`.
- **Limits**: "día con más escucha" se etiqueta según la granularidad usada (si es por semana/mes, el bucket se formatea con la etiqueta correspondiente, igual que `formatBucketLabel` web).

## R5. Granularidad temporal automática

- **Decision**: la serie temporal usa granularidad automática según la amplitud del periodo, replicando `autoGranularity` de la web: ≤ 62 días → día; ≤ 370 días → semana; si no → mes. El usuario no la configura (FR-004).
- **Rationale**: paridad de comportamiento con la web (SC-003) y simplicidad de UI (FR-004).
- **Alternatives considered**: selector manual de granularidad (la web lo permite) — descartado en v1 (fuera de alcance asumido en la spec); se añadiría como mejora si se echa en falta.

## R6. Punto de entrada y navegación

- **Decision**: fila accionable "Gráficas" en `SettingsScreen`, junto a "Acerca de" (mismo patrón `Text + clickable`), con `semantics { contentDescription = ... }`; la pantalla recibe un callback `onStats` que `TolochaNavGraph` conecta a `navController.navigate(Routes.STATS)`. Nueva ruta `Routes.STATS = "stats"` con `composable` dedicado y `hiltViewModel()` para `StatsViewModel`.
- **Rationale**: opción A elegida por el usuario (decisión registrada en la spec); sigue el patrón de navegación existente (`Routes.kt` + `TolochaNavGraph.kt`) sin inventar mecanismos.
- **Alternatives considered**: apartado de primer nivel en la bottom bar (opción B) — descartado por decisión del usuario; diálogo embebido en Configuración — descartado: los 7 bloques necesitan pantalla completa con scroll (FR-009).

## R7. Contrato de datos: DTOs espejo del OpenAPI del backend

- **Decision**: `StatsDtos.kt` con `@Serializable` replicando los nombres de campo del backend (`totalMs`, `bucket`, `weekday`, `hour`, `startedAt`, `durationMs`, `countryCode`…), tal como hace el resto del proyecto con `StationDtos`/`HistoryEntryDto`. El contrato completo queda en `contracts/stats-api.md`. Se reutiliza `StationDto` dentro de `StatsTopEntry`/`StatsRecentEntry`.
- **Rationale**: la constitución declara el OpenAPI (`/api/v1/openapi.json`) fuente de verdad de DTOs; tests de serialización contra ejemplos del contrato (principio III).
- **Alternatives considered**: modelos de dominio propios con mappers — descartado: el proyecto usa DTOs hasta la UI salvo caché Room (convención existente); crear una capa de mappers nueva rompería la consistencia sin aportar aislamiento (no hay segunda fuente de datos).

## R8. Formato de tiempos y textos

- **Decision**: `StatsFormat.kt` (puro, testeable) con `formatDurationMs` (0 min / N min / H h / H h M min) y `formatBucketLabel`/`formatFullDay` en español, espejo de `apps/web/src/lib/stats.ts`; textos de UI hardcodeados en español como el resto de composables.
- **Rationale**: SC-003 exige coincidencia con la web; el patrón `AppInfoFormat.kt`/`SettingsLabels.kt` ya existe en `feature/settings`.
- **Alternatives considered**: recursos `strings.xml` — descartado: inconsistente con la convención actual de la app (solo sleep-timer los usa); se unificaría en una refactorización global aparte.
