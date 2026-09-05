# Quickstart: validar el look-and-feel web

**Feature**: `002-web-look-and-feel`

## Prerrequisitos

- Rama `002-web-look-and-feel`, Android Studio + SDK 37, dispositivo/emulador API 26+.
- Referencia web: `https://github.com/izquierdojl/tolocharadio/tree/main/apps/web` (o checkout local para lado a lado).

## Pasos

1. **Compilar y gates**
   ```powershell
   .\gradlew :app:assembleDebug
   .\gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest
   .\gradlew detekt ktlintCheck lintDebug
   ```
   Esperado: todo verde; el test de contraste WCAG AA pasa en dark/light.

2. **Marca P1 (spec US1)**
   - Abrir la app junto a la web en oscuro y claro.
   - Esperado: mismo emblema Sierra en cabecera/launcher/splash, fondo `pine-950`/`#eef3ef`, marca ocre `400/600`.

3. **Componentes P2 (spec US2)**
   - Recorrer Home → Explorar → Favoritos → Historial → Mis emisoras → Perfil + mini-player.
   - Esperado: tarjetas 16:9 con degradado, play circular (ocre al reproducir), ≤3 chips uppercase + bitrate, franja `MountainWall`, `EmptyState` con tono muted. Sin paleta anterior residual.

4. **Iconos + assets P3 (spec US3)**
   - Cambiar de sección por la bottom bar; crear/ver emisora personalizada sin favicon.
   - Esperado: iconos Material con igual significado que Lucide; `res/drawable/sierra_emblem*.xml + mountain_wall.xml + ic_github.xml` existen con cabecera de origen; placeholder = emblema.

5. **Tema 3 estados (FR-010, SC-005)**
   - En Perfil/Ajustes: Sistema → Claro → Oscuro; rotar pantalla y mandar a background/foreground.
   - Esperado: cada cambio <1s, sin mezcla de temas, persiste tras reinicio.

## Salida

Si falla 2–5, adjuntar captura lado a lado (web vs app) y par problemático (p. ej. `ochre-600/white`) al reportar.
