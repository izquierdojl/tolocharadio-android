# Bug Assessment: Cast sigue sin descubrir dispositivos — falta ACCESS_LOCAL_NETWORK (Android 17 / targetSdk 37)

- **Slug**: 0023-jlizquierdo-20260912-chromecast-local-network-permission
- **Created**: 2026-09-12
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> Seguimos teniendo problemas con el envio a dispositivos chromecast o similares, el sistema no encuentra nunca dispositivos en la red cuando existen. Revisa lo motivos y porque puede ser. No se si se puede probar en el emulador para ver los problemas por temas de red, pero deberiamos dejar esto resuelto.

Recurrencia del bug ya tratado en `.specify/bugs/chromecast-no-devices-found/` (fix del 2026-09-08: permisos Android 12/13 + solicitud en runtime). El usuario indica que el problema **persiste**: el selector de Cast nunca lista dispositivos aunque existan en la red, y pregunta si es posible reproducirlo en el emulador.

## Symptom

Al pulsar el botón Cast de la TopAppBar no aparece ningún dispositivo en el selector, aunque haya receptores Google Cast en la misma red Wi‑Fi. Se espera que el picker liste los dispositivos `_googlecast._tcp` disponibles. Ocurre de forma sistemática ("nunca" los encuentra), no intermitente.

## Reproduction

1. Compilar e instalar la app en un dispositivo (o emulador) que targetea API 37 (Android 17).
2. Conectar el dispositivo a la misma Wi‑Fi que un receptor Cast (Chromecast / Nest / TV).
3. Abrir Tolocha Radio y pulsar el botón Cast de la barra superior.
4. Observar que el selector aparece vacío ("No se encontraron dispositivos").
5. Abrir otra app Cast (p. ej. YouTube) y comprobar que sí descubre el mismo receptor.

`[NEEDS CLARIFICATION: ¿el usuario prueba en dispositivo físico o en emulador? La respuesta condiciona si el fallo es la app o el entorno de red.]`

## Suspected Code Paths

- `app/build.gradle.kts:31` — `targetSdk = 37`. Es la condición que activa la *enforcement* de Android 17: el acceso a la red local se bloquea por defecto para apps que targetean API 37+.
- `app/src/main/AndroidManifest.xml:5-14` — Declara `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `ACCESS_COARSE/FINE_LOCATION`, `NEARBY_WIFI_DEVICES`… pero **falta `ACCESS_LOCAL_NETWORK`**. Sin esa declaración el proceso no puede hacer mDNS/SSDP en la LAN aunque los otros permisos estén concedidos.
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt:318-331` — `requiredCastPermissions()` solo devuelve `NEARBY_WIFI_DEVICES` (33+) o `ACCESS_FINE_LOCATION` (31-32). Nunca pide `ACCESS_LOCAL_NETWORK`, y `areCastPermissionsGranted()` tampoco lo comprueba. El flujo de runtime (`:146-159`) no cubre el permiso exigido en Android 17.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:85-96` — `initCastContext()` envuelve la inicialización en `runCatching`, de modo que un fallo de acceso a red local no se manifiesta en logs más que como un `Log.e` genérico.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastOptionsProvider.kt:10-24` — Configuración correcta de Default Media Receiver, pero no hay `MediaTransferReceiver` en el manifest (vía alternativa del "output switcher", ver remediación).

## Root Cause Hypothesis

**Confidence: high**

Android 17 (API 37) introduce el permiso runtime `ACCESS_LOCAL_NETWORK` y, para apps que targetean API 37+, **bloquea por defecto todo acceso a la red local** (mDNS, sockets crudos, descubrimiento de dispositivos) hasta que el permiso se declare y se conceda. El descubrimiento de Google Cast se hace por mDNS (`_googlecast._tcp` en `224.0.0.251:5353`), es decir, es exactamente una operación de red local. Como el `AndroidManifest.xml` no declara `ACCESS_LOCAL_NETWORK` (y el flujo runtime no lo solicita), el tráfico multicast queda bloqueado y el picker aparece siempre vacío.

Esto explica la recurrencia: el fix anterior (2026-09-08) añadió los permisos de Android 12/13 (`ACCESS_FINE_LOCATION`/`NEARBY_WIFI_DEVICES`), que son necesarios pero **ya no suficientes** en Android 17. El propio sample oficial de Google (googlecast/CastVideos-android, issue #143) reporta el mismo síntoma al subir a targetSdk 37: "casting no longer finds devices to cast to".

Evidencia directa:
- Android Developers — *Behavior changes: Apps targeting Android 17 or higher*: "Local network permission required for apps targeting Android 17".
- Android Developers — *Local network permission*: "New Apps / Updated Apps, Target SDK >= 37, Blocked By Default → Declare and request `ACCESS_LOCAL_NETWORK` runtime permission".
- `ACCESS_LOCAL_NETWORK` pertenece al grupo `NEARBY_DEVICES`; declararlo es obligatorio aunque ya se conceda otro permiso del grupo.

**Blast radius adicional**: la app permite configurar servidores en LAN (ver `NormalizeBaseUrlUseCaseTest.kt:22`, caso `http://192.168.1.10:3000`). Con targetSdk 37, `ACCESS_LOCAL_NETWORK` también es necesario para conectar con un servidor Tolocha autoalojado en la red local, no solo para Cast.

## Proposed Remediation

**Preferred** (ruta B de la guía oficial: permiso explícito, mantiene el `MediaRouteButton` actual):

1. Añadir al manifest:
   ```xml
   <uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />
   ```
   (y opcionalmente `android:usesPermissionFlags="neverForLocation"` en `NEARBY_WIFI_DEVICES`).
2. Extender `requiredCastPermissions()` para incluir `Manifest.permission.ACCESS_LOCAL_NETWORK` en API 37+ y extraer la función a un helper puro testeable (p. ej. `core/ui/util/CastPermissions.kt`).
3. Verificar/re-solicitar el permiso en `onResume` (no solo en `LaunchedEffect(chromeVisible)`), ya que el usuario puede volver de Ajustes y `castPermissionsGranted` es `remember` de una sola vez. Mostrar un dialog de racional ("necesario para descubrir dispositivos en tu red local; no se usa para localizarte").
4. Mejorar la observabilidad en `CastPlayerManager.initCastContext()`: registrar el estado de concesión de `ACCESS_LOCAL_NETWORK` y no tragar el fallo de discovery.

**Alternativas**:
- **Ruta A — Output Switcher / `MediaTransferReceiver`**: recomendada por Google para casting; el sistema gestiona el descubrimiento sin pedir `ACCESS_LOCAL_NETWORK`. Trade-off: hay que declarar `MediaTransferReceiver` en el manifest y adoptar el output switcher del sistema; se pierde el `MediaRouteButton` "clásico" y parte del control de UI. Es la opción más privacy‑friendly y a prueba de futuros cambios.
- **Bajar `targetSdk` por debajo de 37**: **no recomendado** (se incumple la política de Play y solo es una prórroga temporal).

**Files likely to change**:
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`
- (nuevo) `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/util/CastPermissions.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`

**Tests to add or update**:
- Unit test de la selección de permisos por API level (`requiredCastPermissions(33/36/37)` debe incluir `ACCESS_LOCAL_NETWORK` en 37+).
- Unit test de `areCastPermissionsGranted()` con/sin `ACCESS_LOCAL_NETWORK`.
- Instrumented test en emulador API 37: verificar que se lanza la solicitud y que el permiso queda concedido (la discovery real no es verificable en emulador).
- Test manual en **dispositivo físico** con un receptor Cast real (obligatorio).

## Risks & Considerations

- **Emulador no sirve para validar la discovery real**: el emulador está detrás de NAT y el multicast mDNS no llega a la LAN del host. El nuevo stack de red del emulador (36.5) solo permite NSD entre AVDs del mismo host, **no** hacia un Chromecast físico. La afirmación "todo verificado en emulador" de `specs/011-chromecast-integration/spec.md:7` no pudo cubrir el descubrimiento; solo la UI. Validar siempre en dispositivo físico.
- **Permiso a nivel de dispositivo**: aunque la app declare/conceda el permiso, Google Play Services debe tener "Dispositivos cercanos" concedido (hilos de GrapheneOS/Android 17). No es controlable desde la app; conviene un hint de troubleshooting si no aparece nada tras conceder.
- **Migración/compatibilidad**: `ACCESS_LOCAL_NETWORK` solo existe en API 37; gatearlo por `Build.VERSION.SDK_INT >= 37` para no romper API 26-36.
- **Play Store**: permisos del grupo `NEARBY_DEVICES` pueden requerir justificación en la ficha; documentar que se usan para descubrir dispositivos Cast / servidor local, no para ubicación.
- **Regresión**: el cambio es aditivo a nivel de permisos y no afecta la lógica de sesión/switch de `CastPlayerManager`, ya probada.

## Open Questions

- [NEEDS CLARIFICATION: ¿se opta por la ruta B (declarar `ACCESS_LOCAL_NETWORK`) o por la ruta A (output switcher / `MediaTransferReceiver`) recomendada por Google?]
- [NEEDS CLARIFICATION: ¿el servidor Tolocha del usuario puede estar en la LAN? Si sí, el permiso también es requisito para la conectividad base, no solo para Cast.]
- [NEEDS CLARIFICATION: ¿en qué API level/versión de Android reproduce el usuario el fallo? Confirmar que es Android 17 (API 37) para cerrar el diagnóstico.]
