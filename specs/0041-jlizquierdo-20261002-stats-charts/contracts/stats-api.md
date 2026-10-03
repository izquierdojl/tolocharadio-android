# Contracts: API de estadísticas de escucha

**Feature**: `0041-jlizquierdo-20261002-stats-charts` | **Date**: 2026-10-02

Fuente de verdad: OpenAPI del backend (`GET /api/v1/openapi.json`) e implementación en `apps/api/src/routes/stats.ts` + `apps/api/src/services/stats.ts` del repo `izquierdojl/tolocharadio`. Todos los endpoints son **GET autenticados** (`Authorization: Bearer <access>` vía `AuthInterceptor`; refresco automático con `TokenAuthenticator`). Errores siempre `{error:{code,message,status,details?}}` (401 sesión caducada, 400 `INVALID_PARAMS`).

## Endpoints

| Método y ruta | Parámetros | Respuesta |
|---|---|---|
| `GET /api/v1/stats/me/top` | `from?`, `to?` (YYYY-MM-DD), `limit?` (1..50, def. 10) | `{items: StatsTopEntry[]}` |
| `GET /api/v1/stats/me/timeline` | `from?`, `to?`, `granularity?` (`day\|week\|month`, def. `day`) | `{granularity, items: StatsTimelineEntry[]}` |
| `GET /api/v1/stats/me/habits` | `from?`, `to?` | `{items: StatsHabitEntry[]}` |
| `GET /api/v1/stats/me/genres` | `from?`, `to?`, `limit?` (1..50, def. 10) | `{items: StatsGenreEntry[]}` |
| `GET /api/v1/stats/me/countries` | `from?`, `to?` | `{items: StatsCountryEntry[]}` |
| `GET /api/v1/stats/me/recent` | `limit?` (1..200, def. 50) | `{items: StatsRecentEntry[]}` |

Restricciones del servidor: `from <= to`; el rango de `timeline` no puede superar 1830 días (`MAX_TIMELINE_DAYS`). Formas de error: `INVALID_PARAMS` (400), `UNAUTHORIZED` (401).

## Interfaz cliente (sketch Kotlin, `data/remote/api/StatsApi.kt`)

```kotlin
interface StatsApi {
    @GET("stats/me/top")      suspend fun top(@Query("from") from: String?, @Query("to") to: String?, @Query("limit") limit: Int?): Response<StatsTopListDto>
    @GET("stats/me/timeline") suspend fun timeline(@Query("from") from: String?, @Query("to") to: String?, @Query("granularity") granularity: String?): Response<StatsTimelineDto>
    @GET("stats/me/habits")   suspend fun habits(@Query("from") from: String?, @Query("to") to: String?): Response<StatsHabitListDto>
    @GET("stats/me/genres")   suspend fun genres(@Query("from") from: String?, @Query("to") to: String?, @Query("limit") limit: Int?): Response<StatsGenreListDto>
    @GET("stats/me/countries") suspend fun countries(@Query("from") from: String?, @Query("to") to: String?): Response<StatsCountryListDto>
    @GET("stats/me/recent")   suspend fun recent(@Query("limit") limit: Int?): Response<StatsRecentListDto>
}
```

Estilo idéntico a `HistoryApi`/`CustomStationsApi`: `suspend` + `retrofit2.Response<T>`, path relativo a `{baseUrl}/api/v1/`, DTOs `@Serializable` en `StatsDtos.kt` (ver `data-model.md`).

## Contrato de UI (`feature/stats`)

| Estado | Contenido visible | Acciones |
|---|---|---|
| `Loading` | indicador de carga | — |
| `Empty` | mensaje "Todavía no hay escucha en este periodo…" + filtro de periodo | cambiar periodo |
| `Content` | filtro de periodo + 7 bloques (resumen, evolución temporal, top emisoras, hábitos, géneros, países, recientes) | cambiar periodo (recarga todo) |
| `Error` | mensaje en español (`DomainError.userMessage()`) | "Reintentar"; si `isAuthError` → editar servidor activo |

- Filtro de periodo: `7 días | 30 días | 90 días | Todo` (defecto `30 días`).
- Bloques equivalentes 1:1 a la página `/estadisticas` de la web (SC-003).
- Accesibilidad: cada bloque con etiqueta legible; sin scroll horizontal (FR-009); `contentDescription` en la fila "Gráficas" de Configuración.
