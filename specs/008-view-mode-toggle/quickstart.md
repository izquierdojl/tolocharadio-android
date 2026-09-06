# Quickstart: Validación del Alternador de Vista Lista/Tarjetas

Feature: `008-view-mode-toggle` | Date: 2026-09-06

## Prerrequisitos

- Instancia backend TolochaRadio accesible (o la de `BuildConfig.TOLOCHA_BASE_URL`).
- Cuenta con sesión iniciada (las 4 secciones afectadas requieren auth, salvo Explorar pública).
- Emulator/dispositivo con `minSdk 26+`.

```powershell
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## Escenarios de validación (mapean a la spec)

### E1. Default lista en instalación limpia (US-1.1, SC-003)

1. Borra datos de la app (desinstala o "Clear storage").
2. Inicia sesión y abre Explorar → Favoritos → Historial → Mis emisoras.
3. **Esperado**: las 4 secciones se ven en modo lista; el toggle muestra el icono de cuadrícula (modo destino).

### E2. Alternar y globalidad (US-1.2–1.4, FR-004, SC-002)

1. En Explorar pulsa el toggle de la barra superior.
2. **Esperado**: el contenido pasa a tarjetas (2 columnas), el icono pasa a lista, sin spinner ni recarga.
3. Navega a Favoritos → Historial → Mis emisoras.
4. **Esperado**: las 3 secciones ya están en tarjetas; un solo cambio de modo fue suficiente.

### E3. Persistencia entre sesiones (US-2, FR-006, SC-004)

1. Con modo tarjetas activo, cierra la app desde el launcher y vuelve a abrirla.
2. **Esperado**: cualquier sección arranca en tarjetas.
3. Vuelve a modo lista, reinicia: arranca en lista.

### E4. Estado conservado al alternar (US-3, FR-007, SC-005)

1. En Explorar, busca "rock" y pulsa "Cargar más" una o dos veces.
2. Alterna a tarjetas.
3. **Esperado**: la búsqueda y todos los resultados cargados se muestran ya en tarjetas, sin recarga ni reset de la paginación.

### E5. Estados degradados (edge cases)

- Sección vacía (favoritos sin emisoras): el toggle funciona y el `EmptyState` se mantiene.
- Explorar con error de red: el toggle funciona; el banner de error con reintento se re-presenta en el nuevo modo.
- Preferencia corrupta: escribe manualmente `view_mode=XXX` en el DataStore (oDevice File Explorer / adb) y abre la app → modo lista sin error.

### E6. Visibilidad del control (FR-009)

- **Esperado**: sin toggle en Login, Registro, Perfil, Servidores, detalle de emisora ni home de arranque.

## Tests automatizados

```powershell
./gradlew :app:testDebugUnitTest          # InstancePrefsTest, ViewModeViewModelTest
./gradlew :app:connectedDebugAndroidTest  # Compose test del alternador (Explorar)
./gradlew :app:lintDebug detekt ktlintCheck
```

Referencias: [contracts/ui-contract.md](./contracts/ui-contract.md) para el comportamiento observable esperado; [data-model.md](./data-model.md) para la clave y reglas de la preferencia.
