# Bug Verification: Favoritos no disponibles tras reposo prolongado

- **Slug**: 0029-jlizquierdo-20260913-favorites-stale-after-idle
- **Tested**: 2026-09-13
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

Verificado en dispositivo real (Redmi Note 10, Android 17/API 37) con el APK release `1.7.6-fix0029` (versionCode 14, firmado con `release.keystore`, datos preservados). Se forzaron las condiciones del fallo (muerte de proceso, modo avión en arranque en frío y retorno a primer plano) y **Favoritos siempre quedó disponible**: carga normal, restauración de sesión tras matar el proceso, caché offline con banner y refresco automático al recuperar red sin pulsar "Reintentar". Sin crashes. La expiración real de JWT tras reposo largo no se pudo acelerar (ver riesgos).

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproducción (baseline) | Abrir Favoritos con sesión normal | pass | 6 favoritas cargadas, sin banner offline |
| Reproducción (muerte de proceso) | `am force-stop` + `am start` + abrir Favoritos | pass | Auto-login restaura sesión; lista completa |
| Reproducción (sin red en arranque en frío) | Modo avión + `force-stop` + `am start` + Favoritos | pass | "Mostrando caché sin conexión." con las 6 favoritas (sin error duro) |
| Reproducción (vuelta a primer plano) | Restaurar red + HOME + `am start` (ON_RESUME) | pass | Refresco automático; banner desaparece sin "Reintentar" |
| Reproducción natural (reposo horas + expiración JWT) | Dejar el móvil en reposo | not-run | No acelerable; el trigger real (401 con token caducado) está cubierto por tests |
| Smoke de reproducción | Reproducir favorita en el proxy autenticado | pass | Mini-player "Onda Cero Zaragoza" en reproducción |
| New / updated tests | `.\gradlew.bat testDebugUnitTest --rerun` | pass | 304 tests, 0 fallos, 0 errores (7 nuevos) |
| Regression suite | `.\gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug` | pass | BUILD SUCCESSFUL |
| Crash / ANR | `adb logcat -b crash` + logcat | pass | Sin `FATAL EXCEPTION` ni ANR de la app |
| Instrumented (Compose UI) | `connectedDebugAndroidTest` | not-run | No era necesario para el síntoma; dispositivo ocupado con la reproducción manual |

## Output Excerpts

```
Physical size: 1080x2400 / density 440
versionCode=14 minSdk=26 targetSdk=37
versionName=1.7.6-fix0029
```

```
TOTAL tests=304 failures=0 errors=0
core.network.TokenAuthenticatorTest tests=6 failures=0
  - fallo de red en refresh y login no limpia la sesion (reintentable)
  - refresh 503 cae al re-login con credenciales
data.repo.FavoritesRepoTest tests=10 failures=0
  - list 401 con cache devuelve cache offline
domain.auth.AuthenticateServerUseCaseTest tests=2 failures=0
```

Capturas de pantalla (checklist visual, ya borradas del dispositivo):

1. Favoritos con 6 emisoras, sin banner.
2. Tras `force-stop` + relanzar: Favoritos completo (sesión restaurada).
3. Modo avión + arranque en frío: lista + "Mostrando caché sin conexión."
4. Red restaurada + ON_RESUME: lista sin banner (refresco automático).
5. Mini-player reproduciendo tras el ciclo anterior.

## Residual Risks

- **Expiración real de JWT tras reposo largo**: no se pudo forzar; se validaron las condiciones equivalentes (proceso muerto, red caída, retorno a primer plano) y la renovación 401→refresh→re-login está cubierta por tests unitarios. Si el backend responde 403 (no 401) con el access caducado, el `Authenticator` de OkHttp no se dispara: pendiente confirmar contrato.
- **Playback tras reposo**: `PlayerDataSourceFactory` captura el token al crear el data source con un `OkHttpClient` propio sin interceptor; el smoke test pasó tras el ciclo, pero un token caducado en el momento de pulsar play podría fallar (fuera del alcance de este fix).
- **UX de error de credenciales con caché**: con caché disponible se muestra la lista offline en vez de "Editar servidor"; revisar si se quiere un aviso más explícito.
- El APK instalado en el dispositivo es un release local `1.7.6-fix0029` (versionCode 14); los builds oficiales con versionCode superior lo actualizarán sin problemas.

## Recommendation

Cerrar el bug como verificado: en dispositivo real las condiciones que provocaban "Favoritos no disponibles al volver" ya no reproducen el fallo, con tests y gates en verde. Mantener como seguimiento los riesgos residuales (403 vs 401 y token del player); si volviese a ocurrir tras un reposo real de horas, reabrir y re-ejecutar `/speckit.bug.assess` con la evidencia de `logcat` y el estado exacto de la UI.
