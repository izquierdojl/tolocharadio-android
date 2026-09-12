# Quickstart — Validación: Credenciales por servidor y auto-login

**Feature**: `0024-jlizquierdo-20260912-per-server-credentials` | **Date**: 2026-09-12

Guía de validación end-to-end. Contratos: [contracts/server-auth.md](./contracts/server-auth.md) y [data-model.md](./data-model.md).

## Prerrequisitos

- Instancia TolochaRadio con **login email/contraseña y JWT + refresh** (catálogo público; favoritos/historial/proxy autenticados).
- Build de debug: `./gradlew assembleDebug` (o `installDebug`).
- Para la actualización: dispositivo/emulador con una build de la **spec 0022** y un servidor guardado sin credenciales.

## Escenario 0 — Gates automáticos

```bash
./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug
./gradlew connectedDebugAndroidTest   # requiere dispositivo/emulador
```

Esperado: todo en verde, incluidos los tests restaurados de sesión/auth y los nuevos de la pantalla unificada.

## Escenario 1 — Primer arranque: formulario completo (FR-011, US1)

1. Instala limpia (o borra datos de la app).
2. Abre la app.

Esperado: bienvenida con el formulario unificado pidiendo **URL, alias, email y contraseña** (no solo URL+alias). No hay pantalla de login/registro.

## Escenario 2 — Alta de servidor con credenciales (FR-001, FR-003, FR-004, SC-001)

1. Introduce URL, alias, email y contraseña válidos y guarda.

Esperado: se valida la instancia, se hace login, se guarda el servidor (activo y por defecto) y se accede al contenido. Una URL o credenciales inválidas no persisten y muestran error accionable.

## Escenario 3 — Uso diario sin pedir credenciales (FR-004, FR-005, SC-002, SC-003)

1. Abre Favoritos, Historial y Mis emisoras.
2. Reproduce una emisora (progresiva y `.m3u8`).
3. Deja caducar el token o revócalo en la instancia y repite.

Esperado: todo carga/reproduce sin pedir nada; ante token caducado, refresh transparente y reintento; no aparece ninguna pantalla de login. Verifica que las peticiones (incluidos segmentos HLS) llevan `Bearer` y que el token no viaja en la URL.

## Escenario 4 — Credenciales incorrectas → editar servidor activo (FR-006, US3, SC-004)

1. Edita el servidor activo y pon una contraseña incorrecta; guarda (si el login falla, no debe persistir).
2. Alternativa: cambia la contraseña en la instancia dejando la app con la antigua.
3. Abre Favoritos o Historial y pulsa la acción del error.

Esperado: el error de credenciales ofrece **Editar servidor** y abre la edición del **servidor activo**; al corregir la contraseña, la app re-loguea y los datos cargan. Con error de red, la acción es **Reintentar** y no abre la edición.

## Escenario 5 — Edición de servidor (FR-007)

1. Desde Servidores, pulsa el icono Editar de un servidor.

Esperado: formulario unificado precargado (email visible, contraseña enmascarada). Guardar sin tocar la contraseña la conserva; cambiarla revalida y re-loguea.

## Escenario 6 — Varios servidores y cambio (FR-008)

1. Añade un segundo servidor con credenciales (puede ser otro usuario).
2. Selecciónalo como activo.

Esperado: se limpia caché y sesión del anterior; login automático con las del nuevo; favoritos/historial reflejan al nuevo usuario. El por defecto no cambia.

## Escenario 7 — Actualización desde la 0022 (FR-010, SC-006)

1. Ten una build de la 0022 con un servidor guardado sin credenciales.
2. Instala encima esta build y abre la app.

Esperado: el servidor se conserva; el arranque abre el formulario de edición de forma **bloqueante** hasta completar credenciales; tras guardar, login y acceso. No se pierde ningún dato.

## Escenario 8 — Borrado de servidores (FR-010 de la 0022 se mantiene)

1. Elimina todos los servidores.

Esperado: al borrar el último, vuelve la bienvenida con el formulario unificado.

## Escenario 9 — Seguridad (SC-005)

1. Revisa logs y tráfico.
2. Revisa el dispositivo (`tolocha_tokens`).

Esperado: 0 credenciales en logs o URLs; contraseña y refresh solo en almacenamiento cifrado; el almacén de tokens excluido de backup.
