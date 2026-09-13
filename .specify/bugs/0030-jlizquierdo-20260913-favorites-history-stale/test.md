# Bug Verification: Favoritos e Historial no se refrescan al momento tras cambios

- **Slug**: 0030-jlizquierdo-20260913-favorites-history-stale
- **Tested**: 2026-09-13
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

Verificado en dispositivo físico (Redmi Note 10, Android 17/API 37) con el APK release local `1.7.7-fix0030` (versionCode 15, firmado con `release.keystore`, datos preservados sobre `1.7.6-fix0029`): marcar una emisora como favorita desde Explorar (búsqueda) la muestra en Favoritos al instante y reproducirla la muestra en Historial al instante, sin reiniciar ni salir de la app. Sin crashes; el Deshacer de Favoritos sigue funcionando.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Dispositivo | `adb devices -l` / `getprop` | pass | Redmi Note 10 (M2101K7AG / mojito), Android 17 (API 37) |
| Instalación | `gradlew assembleRelease` (RELEASE_VERSION=1.7.7-fix0030, VERSION_CODE=15) + `adb install -r` | pass | `Success`; upgrade sobre `1.7.6-fix0029` conservando datos y sesión |
| Reproducción favoritos (post-fix) | En Explorar, buscar "anon", corazón en Anon.FM → pestaña Favoritos | pass | Anon.FM aparece la primera en "Tus favoritos" sin reiniciar (verificado dos veces) |
| Reproducción historial (post-fix) | Reproducir Anon.FM desde Favoritos → pestaña Historial | pass | Anon.FM primera con "ahora mismo"; el mini-player muestra "Sonando: Anon.FM" |
| Persistencia (verdad del servidor) | HOME + reabrir app → Historial | pass | Anon.FM sigue primera con `hace 2 min` (reconciliado con el servidor, sin entrada fantasma) |
| Regresión Deshacer | Quitar Anon.FM con el corazón → "Deshacer" en la snackbar (mismo comando, <10 s) | pass | La snackbar "Favorita eliminada" aparece y "Deshacer" restaura la favorita en su sitio |
| Estado final / limpieza | Quitar Anon.FM (sin deshacer) + `cmd media_session dispatch stop` | pass | Favoritos vuelve a las 6 originales; sin "Sonando" ni reproducción activa |
| Tests del fix | `gradlew testDebugUnitTest --tests "…FavoritesRepoTest" --tests "…HistoryRepoTest" --tests "…FavoritesViewModelTest" --tests "…HistoryViewModelTest" --tests "…PlayerViewModelTest"` | pass | 53 tests, 0 fallos |
| Suite + gates completos | `gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` (fase fix, mismo código) | pass | BUILD SUCCESSFUL; 315 tests, 0 fallos; detekt/ktlint/lint verdes |
| Crashes | `adb logcat -b crash -d -t 100` | pass | Buffer vacío; sin `FATAL EXCEPTION` |
| Reordenar (arrastre) | No automatizado por `adb input` | not-run | Mantener como riesgo residual; lógica de `savingOrder` no modificada y cubierta por construcción |

## Output Excerpts

Favoritos tras marcar Anon.FM desde Explorar (sin reiniciar la app):

```
[TextView] text='Tus favoritos'
[ImageView] desc='Anon.FM'          bounds=[132,508][264,640]
[TextView] text='Anon.FM'           bounds=[286,473][468,526]
[ImageView] desc='Onda Cero Zaragoza'
[ImageView] desc='SER - Radio Zaragoza'
[ImageView] desc='Cope Zaragoza'
[ImageView] desc='RNE - Radio Nacional de España'
[ImageView] desc='RNE 1 - Zaragoza'
[ImageView] desc='Radio Marca'
```

Historial tras iniciar la reproducción (sin reiniciar la app):

```
[TextView] text='Tu historial'
[ImageView] desc='Anon.FM'          bounds=[22,551][154,683]
[TextView] text='Anon.FM'           bounds=[176,517][358,570]
[TextView] text='ahora mismo'       bounds=[176,679][386,716]
[ImageView] desc='Onda Cero Zaragoza'  (hace 49 min)
```

Deshacer:

```
[TextView] text='Favorita eliminada'  bounds=[77,1784][455,1849]
[TextView] text='Deshacer'            bounds=[800,1784][992,1849]
tap 896 1816  →  [ImageView] desc='Anon.FM' vuelve a la lista
```

Tests del fix:

```
TOTAL tests=53 failures=0
BUILD SUCCESSFUL in 11s
```

Versión instalada:

```
versionCode=15 minSdk=26 targetSdk=37
versionName=1.7.7-fix0030
```

## Residual Risks

- **Reordenar por arrastre** no se ejercitó en dispositivo (automatización compleja con `long-press`); el cambio no toca `moveItem`/`commitOrder` y el estado `savingOrder` se conserva en la reconciliación.
- La reproducción de prueba (Anon.FM) queda en el historial real del usuario en el servidor y en la caché local; eliminable con el botón de papelera de Historial. La favorita de prueba se eliminó y las 6 originales quedan intactas.
- **Cast** sigue sin registrarse en Historial (0026: el receptor usa la URL pública y el servidor no lo ve); comportamiento decidido en el fix, revisar como seguimiento.
- El APK de prueba es un release local `1.7.7-fix0030` (versionCode 15); los builds oficiales con versionCode superior lo actualizarán sin problemas.
- No se repitieron las condiciones de reposo/sesión del bug 0029; su verificación en dispositivo con `1.7.6-fix0029` sigue vigente y este fix no retira `refresh()`/`onForeground`.

## Recommendation

Cerrar el bug como verificado: los dos síntomas originales reproducidos en dispositivo real ya no ocurren (Favoritos y Historial se actualizan al momento sin reiniciar), con tests y gates en verde y sin regresiones en Deshacer. Mantener como seguimiento el riesgo de reordenación no automatizada y el caso Cast/historial.
