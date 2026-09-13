# Feature Specification: Diálogo "Acerca de" con versión real y detalles ampliados

**Feature Branch**: `0033-jlizquierdo-20260913-about-dialog-real-version`

**Created**: 2026-09-13

**Status**: Draft

**Input**: User description: "En Dialogo acerca de, que tome información de versión real, ahora siempre pone 1.0, e incluye algo más de información"

## Clarifications

### Session 2026-09-13

- Q: ¿El diálogo "Acerca de" debe incluir una acción para copiar toda la información al portapapeles para soporte, o basta con mostrarla en pantalla? → A: Sí, incluir "Copiar información" con confirmación.
- Q: Además del nombre y la versión reales, ¿qué información adicional debe mostrar el diálogo? → A: Metadatos de la compilación más la configuración activa (tema, pantalla de arranque y alias del servidor activo).
- Q: Cuando el usuario pulsa "Copiar información", ¿cómo debe confirmarse visualmente que la copia se ha realizado? → A: Aviso tipo snackbar en la parte inferior, sin cerrar el diálogo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver la versión real de la aplicación instalada (Priority: P1)

El usuario abre el diálogo "Acerca de" desde Configuración y ve el número de versión que realmente corresponde a la aplicación que tiene instalada. Hoy en día el diálogo muestra siempre "1.0", sin importar qué build se haya instalado, por lo que la información es engañosa para el usuario y para soporte.

**Why this priority**: Es la corrección central solicitada. Sin ella, el resto de la ampliación pierde sentido: la versión es el dato más consultado del diálogo (para reportes de error, soporte y verificación de actualizaciones).

**Independent Test**: Se puede probar instalando dos builds con versiones diferentes, abriendo el diálogo en cada uno y comprobando que la versión mostrada coincide con la del build instalado en ambos casos.

**Acceptance Scenarios**:

1. **Given** una compilación con versión `X.Y.Z`, **When** el usuario abre el diálogo "Acerca de", **Then** se muestra `X.Y.Z` (la versión del build instalado), no el valor fijo `1.0`.
2. **Given** una compilación publicada a partir de la etiqueta `v2.3.1`, **When** el usuario abre el diálogo, **Then** se muestra `2.3.1`.
3. **Given** una compilación local sin versión explícita, **When** el usuario abre el diálogo, **Then** se muestra la versión por defecto definida por el proyecto (actualmente `1.0`), que en ese caso sí es la versión real.
4. **Given** el diálogo abierto, **When** el usuario lo cierra y lo vuelve a abrir, **Then** la versión mostrada sigue siendo la del build y no cambia entre aperturas.

---

### User Story 2 - Ver detalles técnicos ampliados (Priority: P2)

El usuario (o quien le da soporte) necesita más contexto que el nombre y la versión. El diálogo amplía la información mostrada con el número de compilación, el identificador de la aplicación y el tipo de build, además de los datos ya existentes (desarrollador, licencia y enlace al repositorio). También incluye la configuración activa de la aplicación: tema, pantalla de arranque y alias del servidor activo.

**Why this priority**: Responde directamente a "incluye algo más de información". Estos datos permiten identificar sin ambigüedad la compilación concreta y el contexto de uso, y son los que habitualmente se piden en un reporte de error.

**Independent Test**: Se puede probar abriendo el diálogo en un build de depuración y en uno de publicación, con distinto tema y distinta pantalla de arranque, y comprobando que el identificador de la aplicación, el tipo de build y la configuración activa mostrados corresponden a cada caso.

**Acceptance Scenarios**:

1. **Given** el diálogo abierto, **When** el usuario observa el contenido, **Then** se muestran al menos: nombre de la app, versión, número de compilación, identificador de la aplicación, tipo de build, desarrollador, licencia, enlace al repositorio y configuración activa (tema, pantalla de arranque y alias del servidor activo).
2. **Given** una compilación de depuración, **When** el usuario abre el diálogo, **Then** el tipo de build mostrado indica "depuración" (o equivalente claro para el usuario).
3. **Given** una compilación de publicación, **When** el usuario abre el diálogo, **Then** el tipo de build mostrado indica "publicación" (o equivalente claro para el usuario).
4. **Given** un tema o una pantalla de arranque seleccionados, **When** el usuario abre el diálogo, **Then** la configuración activa mostrada coincide con la selección actual.
5. **Given** el diálogo abierto, **When** un dato opcional no está disponible, **Then** ese dato se omite o se sustituye por un valor por defecto definido, sin dejar la línea vacía ni provocar un fallo.

---

### User Story 3 - Copiar la información para soporte (Priority: P3)

El usuario quiere reportar un problema o pedir ayuda. Desde el diálogo pulsa una acción "Copiar información" y todos los datos mostrados (nombre, versión, número de compilación, identificador, tipo de build, configuración activa, etc.) se copian al portapapeles en un formato de texto legible, listo para pegar en un correo, un issue o un chat.

**Why this priority**: Es la ampliación con mayor valor práctico tras la versión real. Reduce errores de transcripción y acelera el soporte, pero el diálogo sigue siendo útil sin ella.

**Independent Test**: Se puede probar abriendo el diálogo, pulsando "Copiar información", pegando en una aplicación de texto y verificando que el contenido pegado incluye todos los datos mostrados con etiquetas comprensibles.

**Acceptance Scenarios**:

1. **Given** el diálogo abierto, **When** el usuario pulsa "Copiar información", **Then** el portapapeles contiene todo el bloque de información en texto legible con cada dato etiquetado.
2. **Given** el usuario pulsa "Copiar información", **When** la copia se completa, **Then** se muestra un aviso tipo snackbar en la parte inferior confirmando la copia, sin cerrar el diálogo.
3. **Given** el usuario pulsa "Copiar información", **When** el portapapeles no está disponible o el sistema deniega el acceso, **Then** la aplicación no falla y se muestra un aviso indicando que no se pudo copiar.

---

### Edge Cases

- **Versión no disponible**: si el build no expone un número de versión, el diálogo no debe quedar en blanco ni fallar; se muestra un valor por defecto definido por el proyecto (hoy `1.0`).
- **Versión muy larga**: versiones con sufijos (por ejemplo `2.3.1-rc.1+build.20260913`) o cadenas largas no deben romper el diseño del diálogo; deben seguir siendo legibles sin desbordar.
- **Rotación del dispositivo con el diálogo abierto**: el diálogo permanece abierto y muestra la misma información; su estado no se pierde ante cambios de configuración.
- **Sin navegador instalado**: el enlace al repositorio sigue fallando de forma silenciosa, sin bloquear el resto del diálogo.
- **Portapapeles no disponible**: la acción de copiar no debe provocar un cierre inesperado ni una excepción visible; se muestra el aviso de error sin cerrar el diálogo.
- **Sin servidor activo**: si no hay un servidor configurado, el alias del servidor activo no existe; se omite ese dato o se muestra un valor por defecto ("sin servidor"), sin romper el diálogo.
- **Configuración activa no legible**: si el tema o la pantalla de arranque no pueden resolverse, se muestra el valor por defecto correspondiente sin dejar la línea vacía.
- **Contenido muy extenso en pantallas pequeñas**: el diálogo debe permitir desplazamiento o ajuste para que todos los datos sigan siendo accesibles.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El diálogo "Acerca de" DEBE mostrar el número de versión real de la compilación instalada, obtenido del propio build (por ejemplo, el derivado de una etiqueta de release), en lugar de un valor fijo codificado en la interfaz.
- **FR-002**: El diálogo DEBE mostrar información ampliada además del nombre y la versión: número de compilación, identificador de la aplicación, tipo de build (depuración/publicación) y la configuración activa (tema, pantalla de arranque y alias del servidor activo).
- **FR-003**: El diálogo DEBE seguir mostrando el desarrollador, la licencia y el enlace interactivo al repositorio del código fuente.
- **FR-004**: El enlace al repositorio DEBE abrirse en el navegador del dispositivo al pulsarlo, y fallar de forma silenciosa si no hay navegador disponible.
- **FR-005**: El diálogo DEBE ofrecer una acción para copiar al portapapeles todo el bloque de información en texto legible con etiquetas.
- **FR-006**: Tras copiar, la aplicación DEBE mostrar un aviso tipo snackbar en la parte inferior confirmando la copia, sin cerrar el diálogo; si la copia no es posible, DEBE mostrar un aviso de error sin cerrar la aplicación.
- **FR-007**: Cuando un dato no esté disponible (por ejemplo, sin servidor activo), el diálogo DEBE mostrar un valor por defecto definido o omitir la línea, sin dejar celdas vacías ni provocar errores.
- **FR-008**: El diálogo DEBE poder cerrarse mediante el botón de cierre o pulsando fuera de él, y DEBE conservar su estado ante cambios de configuración del dispositivo (por ejemplo, rotación).
- **FR-009**: La información mostrada DEBE estar disponible sin conexión a red y DEBE excluir credenciales, tokens y datos personales; el alias del servidor activo es un nombre definido por el usuario y PUEDE mostrarse.

### Key Entities *(include if feature involves data)*

- **Información de la aplicación**: Conjunto de datos que identifican la compilación instalada y su contexto de uso. Atributos: nombre de la app, versión (nombre), número de compilación, identificador de la aplicación, tipo de build, desarrollador, licencia, URL del repositorio y configuración activa (tema, pantalla de arranque, alias del servidor activo). Los datos de compilación se derivan del build o de constantes del proyecto; la configuración activa procede de los ajustes locales de la app. No se consultan al backend ni se persisten como parte de la feature.
- **Configuración activa**: Preferencias locales vigentes en el momento de abrir el diálogo: tema (sistema/claro/oscuro), pantalla de arranque y alias del servidor activo (si lo hay). Es de solo lectura para el diálogo.
- **Estado del diálogo**: Indica si el diálogo está oculto o visible y qué información muestra. Persiste en memoria durante la sesión de la pantalla de Configuración.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100% de los builds con una versión definida (etiqueta de release o propiedad de build), el diálogo muestra exactamente esa versión, verificable instalando dos builds con versiones distintas.
- **SC-002**: El diálogo deja de mostrar el valor fijo `1.0` en cualquier build cuya versión configurada sea distinta de `1.0`.
- **SC-003**: El usuario puede copiar toda la información para soporte en una sola acción y pegarla en un editor de texto como bloque legible con todos los datos etiquetados.
- **SC-004**: El diálogo muestra al menos 9 datos distintos (nombre, versión, número de compilación, identificador de la app, tipo de build, desarrollador, licencia, tema y pantalla de arranque) más el alias del servidor activo (cuando exista) y el enlace al repositorio.
- **SC-005**: El diálogo se abre, se cierra y sobrevive a la rotación sin errores en el 100% de las interacciones probadas.
- **SC-006**: Ningún dato personal del usuario, credencial o token aparece en el contenido mostrado o copiado; solo puede aparecer el alias del servidor, definido por el propio usuario.
- **SC-007**: El 100% de las pulsaciones sobre "Copiar información" produce un aviso snackbar (de éxito o de error), sin cerrar el diálogo en ningún caso.

## Assumptions

- La versión real de la aplicación está disponible en el build como metadato estándar (nombre de versión y número de compilación) y el diálogo debe leerla de ahí; no se introduce ninguna fuente de datos nueva ni llamada al backend.
- "Algo más de información" se interpreta como metadatos técnicos de la compilación más la configuración activa, útiles para soporte: número de compilación, identificador de la aplicación, tipo de build, tema, pantalla de arranque y alias del servidor activo, coherentes con las reglas del proyecto (sin datos personales ni credenciales).
- El identificador de la aplicación y el tipo de build son datos estáticos conocidos en tiempo de compilación; el tema, la pantalla de arranque y el alias del servidor activo se leen de los ajustes locales ya existentes y son de solo lectura para el diálogo.
- Se mantiene el comportamiento actual del enlace al repositorio y del cierre del diálogo; esta feature los conserva o amplía, pero no los elimina.
- La feature no cambia la forma en que se calculan las versiones en el pipeline de publicación; solo consume el valor resultante.
- El diálogo sigue el estilo visual y los componentes ya existentes en el proyecto.

## Dependencies

- Requiere que el build exponga la versión real (nombre y número de compilación) como metadato consumible por la aplicación. El proyecto ya lo hace para las publicaciones en CI; en local cae a `1.0`.
- Reutiliza el flujo existente de Configuración → "Acerca de" y el diálogo modal ya implementados.
