# Quickstart: Diálogo "Acerca de" con versión real y detalles ampliados

**Feature**: 0033-jlizquierdo-20260913-about-dialog-real-version
**Date**: 2026-09-13

Guía de validación end-to-end. No contiene código de implementación; el detalle de contratos y entidad está en [contracts/](./contracts/) y [data-model.md](./data-model.md).

## Prerrequisitos

- Windows + PowerShell 7 y JDK 17.
- Dispositivo/emulador Android (API 26+) para la validación manual.
- Repo en la rama `0033-jlizquierdo-20260913-about-dialog-real-version`.

## Compilar y ejecutar

```powershell
# Compilación estándar
.\gradlew.bat assembleDebug

# Compilación con versión conocida para validar la versión "real"
.\gradlew.bat installDebug -PversionName=2.3.1 -PversionCode=42
```

Instalar en el dispositivo/emulador y abrir la app.

## Gates obligatorios (constitución)

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat detekt ktlintCheck lintDebug
```

> Antes del fix, el test `showAppInfoDialog usa la versión del build` debe FALLAR (Red) porque `AppInfo.version` es `"1.0"` fijo; tras implementar debe PASAR (Green).

## Escenarios de validación manual

### VS-1 — Versión real (US1, FR-001)

1. Instalar con `-PversionName=2.3.1 -PversionCode=42`.
2. Ir a **Configuración → Acerca de**.
3. **Esperado**: se muestra `Versión: 2.3.1` y `Número de compilación: 42` (no `1.0`).

### VS-2 — Detalles ampliados y configuración activa (US2, FR-002)

1. En Configuración, elegir tema **Oscuro** y pantalla de arranque **Favoritos**.
2. Con al menos un servidor guardado y activo (alias "Mi servidor"), abrir **Acerca de**.
3. **Esperado**: el diálogo muestra versión, nº de compilación, identificador (`com.izquierdojl.tolocharadio`), tipo de build (**Depuración**/**Publicación**), tema, pantalla de arranque, servidor activo, desarrollador, licencia y enlace al repositorio.

### VS-3 — Copiar información (US3, FR-005/FR-006)

1. Abrir **Acerca de** y pulsar **Copiar información**.
2. **Esperado**: aparece un **snackbar** en la parte inferior ("Información copiada") y el diálogo **permanece abierto**; al pegar en otra app el texto contiene todos los datos etiquetados.

### VS-4 — Sin servidor activo (Edge Case)

1. Sin servidores configurados (o sin servidor activo), abrir **Acerca de**.
2. **Esperado**: no aparece la línea "Servidor activo" (o muestra "Sin servidor"); el diálogo no falla.

### VS-5 — Rotación (Edge Case, FR-008)

1. Abrir **Acerca de** y rotar el dispositivo.
2. **Esperado**: el diálogo sigue abierto con la misma información.

### VS-6 — Enlace al repositorio (FR-004)

1. Pulsar **Ver repositorio**.
2. **Esperado**: se abre el navegador con la URL del repositorio; si no hubiera navegador, no hay crash.

## Trazabilidad

| Requisito | Escenario |
|-----------|-----------|
| FR-001, SC-001, SC-002 | VS-1 |
| FR-002, FR-003, SC-004 | VS-2 |
| FR-005, FR-006, SC-003, SC-007 | VS-3 |
| FR-007 | VS-4 |
| FR-008, SC-005 | VS-5 |
| FR-004 | VS-6 |
| FR-009, SC-006 | Revisión de contenido: ningún dato sensible mostrado/copiado |
