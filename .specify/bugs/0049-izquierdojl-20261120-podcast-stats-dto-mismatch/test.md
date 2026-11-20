# Bug Verification: Gráficas de podcast sin error de deserialización

- **Slug**: 0049-izquierdojl-20261120-podcast-stats-dto-mismatch
- **Tested**: 2026-11-20
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

El fallo queda resuelto y verificado de punta a punta: con el APK corregido, el
emulador mostró las gráficas de podcast contra el servidor real configurado, con
resumen, evolución, rankings, hábitos, categorías y recientes, sin banner rojo.
La suite completa y los checks estáticos también pasan sin regresiones.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Emulador `Medium_Phone_API_37.0` + APK debug corregido; Podcasts → menú → Configuración → Gráficas | pass | Servidor configurado en la app (`radio.jlizquierdo.com`, backend 0.40.1 con `podcastStats: true`). Sin banner de error; siete bloques con datos reales. |
| Logcat post-fix | `adb logcat -d \| grep -E "StatsLoad\|MissingField\|SerializationException"` | pass | Sin entradas: desaparece el `DomainError.Unknown` por `MissingFieldException`. |
| Equivalent automated reproduction | `./gradlew cleanTestDebugUnitTest testDebugUnitTest` | pass | Los tests de `PodcastDtosTest` deserializan payloads reales del backend. |
| New / updated tests | `./gradlew cleanTestDebugUnitTest testDebugUnitTest` | pass | 624 tests, 0 fallos. `PodcastDtosTest`: 11/11; `PodcastStatsMappersTest`: 3/3; `PodcastStatsViewModelTest`: 5/5; `PodcastStatsRepoTest`: 1/1. |
| Regression suite | `./gradlew cleanTestDebugUnitTest testDebugUnitTest` | pass | Suite unitaria completa en verde. |
| Lint / type-check | `./gradlew compileDebugAndroidTestKotlin detekt ktlintCheck lintDebug` | pass | `BUILD SUCCESSFUL`; `lintDebug`: 0 errores, 93 warnings y 1 hint (pre-existentes). |
| Connected UI tests | `connectedDebugAndroidTest` | skipped | No se ejecutó la suite instrumentada; el smoke test manual cubre el flujo crítico. |
| APK build/install | `./gradlew assembleDebug` + `adb install -r app/build/outputs/apk/debug/app-debug.apk` | pass | Instalación correcta conservando servidor y sesión. |

## Output Excerpts

```text
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 13s
```

```text
suites=121 tests=624 failures=0 errors=0 skipped=0
```

```text
com.izquierdojl.tolocharadio.data.remote.dto.PodcastDtosTest tests=11 failures=0 errors=0
  - PodcastStatsSummary lee los agregados reales del backend
  - PodcastTopPodcasts lee el ranking plano del backend
  - PodcastTopEpisodes lee el ranking plano del backend
  - PodcastRecents lee episodeTitle y listenedMs
  - timeline habits y categories del backend se deserializan
```

```text
> Task :app:lintDebug
BUILD SUCCESSFUL in 1m 17s
0 errors, 93 warnings, 1 hint
```

UI observada en el emulador (modo Podcasts → Configuración → Gráficas):

```text
Tus estadísticas de podcast
Tiempo total: 29 h 31 min
42 empezados · 38 completados — Tasa de finalización: 90 %
Programas: 28 — Media por episodio: 42 min
Evolución de la escucha
Programas más escuchados (10)
Episodios más escuchados (10)
Tus hábitos de escucha
Categorías: Technology, Sports, News, Soccer, History…
Actividad reciente con hora y duración
```

Sin texto de error ni botón "Reintentar" en ninguno de los estados recorridos.

## Residual Risks

- El fix asume servidores con el contrato plano actual (≥ 0.40.x). Una
  instancia antigua con la forma anidada del documento de análisis volvería a
  fallar; no hay compatibilidad dual.
- El smoke test se hizo con build debug; el código de DTO y mapeo es el mismo en
  release, pero no se repitió con el APK firmado de release.
- La visualización depende de que el servidor tenga `podcastStats` habilitado y
  de que la cuenta tenga sesiones de escucha (verificado en este entorno).

## Recommendation

Cerrar el bug: verificado end-to-end en emulador contra un servidor real, con la
suite unitaria y los checks estáticos en verde. No es necesario repetir
`/speckit.bug.assess`.
