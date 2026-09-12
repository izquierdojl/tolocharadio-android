# Feature Specification: Reproducción exclusiva y soporte de listas m3u/m3u8/pls

**Feature Branch**: `0019-jlizquierdo-20260911-audio-focus-playlists`

**Created**: 2026-09-11

**Status**: Done (2026-09-11; escenarios 1-5 verificados manualmente por el usuario en emulador)

**Input**: User description: "Vamos a incluir dos mejoras en la reproducción de audio a ver si las podemos implementar
- Cuando se reproduce un audio y otra aplicación se pone a reproducir (por ejemplo foobar, vlc o pocket casts) el audio se mezcla. El comportamiento habitual de reproductores es detenerse y no mezclarse. También al contrario, es decir si tolocharadio comienza a reproducir, el resto de aplicaciones deberían silenciarse.
- En el reproductor de la aplicación, los enlaces m3u8 o m3u no se reproducen. En el navegador sí suelen funcionar, incluso en el móvil. Planifica si se puede descargar una librería adicional de audio o similar para tratar de hacerlo posible, ya que muchas emisoras utilizan ese formato."

## Clarifications

### Session 2026-09-11

- Q: Cuando la lista `.m3u` de una emisora contiene más de una entrada de audio, ¿qué debe hacer la app? → A: Reproducir la primera entrada reproducible como un único stream continuo, omitiendo las entradas inválidas.
- Q: Cuando una interrupción breve del sistema (llamada entrante o aviso de navegación) pausa la radio, ¿qué debe ocurrir al terminar la interrupción? → A: Reanudar automáticamente, salvo que el usuario hubiera pausado manualmente antes; no se usa duck ni queda en pausa permanente.
- Q: Además de `.m3u8` y `.m3u`, ¿debe la app soportar también listas de tipo `.pls`? → A: Sí, soportar también `.pls` como formato de lista de emisoras.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reproducción exclusiva entre aplicaciones (Priority: P1)

Un usuario está escuchando una emisora en TolochaRadio y, en ese momento, otra aplicación del teléfono (foobar, VLC, Pocket Casts, etc.) empieza a reproducir audio. En lugar de superponerse las dos fuentes, TolochaRadio se pausa para ceder el turno. A la inversa, si el usuario pulsa reproducir en TolochaRadio mientras otra app suena, la otra app deja de sonar y solo se escucha la radio.

**Why this priority**: Es la experiencia básica esperada de cualquier reproductor y afecta a cada sesión de escucha. Hoy la mezcla de audio degrada la calidad percibida y hace que la app parezca defectuosa frente a reproductores consolidados.

**Independent Test**: Con TolochaRadio reproduciendo, iniciar audio en una segunda app que respete el foco de audio (p. ej. un reproductor de música). Verificar que TolochaRadio pasa a pausa y no se oye mezcla. Repetir al revés: con la otra app reproduciendo, iniciar TolochaRadio y verificar que la otra app se silencia.

**Acceptance Scenarios**:

1. **Given** TolochaRadio reproduciendo una emisora, **When** otra aplicación comienza a reproducir audio, **Then** TolochaRadio se pausa y no hay solapamiento de sonido.
2. **Given** otra aplicación reproduciendo audio, **When** el usuario inicia reproducción en TolochaRadio, **Then** TolochaRadio suena en exclusiva y la otra aplicación se silencia o pausa.
3. **Given** una interrupción breve del sistema (p. ej. llamada entrante, aviso de navegación), **When** la interrupción termina, **Then** TolochaRadio reanuda la reproducción automáticamente salvo que el usuario la hubiera pausado de forma explícita antes.
4. **Given** TolochaRadio reproduciendo en segundo plano o controlando desde la notificación/Bluetooth, **When** cambia el foco de audio, **Then** el comportamiento exclusivo se aplica igual que en primer plano.

---

### User Story 2 - Reproducir emisoras con enlace de lista (.m3u8 / .m3u / .pls) (Priority: P2)

Un usuario encuentra una emisora cuyo enlace es una lista de reproducción (`...m3u8`, `...m3u` o `...pls`) y, al intentar escucharla, no suena nada. En el navegador del móvil esa misma emisora sí se reproduce. El usuario espera poder oírla también dentro de la aplicación.

**Why this priority**: Un número relevante de emisoras emiten en estos formatos; sin soporte, esas emisoras son inutilizables dentro de la app y el usuario no entiende por qué funcionan fuera y no dentro.

**Independent Test**: Reproducir desde la app varias emisoras de prueba cuyos enlaces sean `.m3u8`, `.m3u` y `.pls` y confirmar que suenan; comprobar que las emisoras de reproducción directa siguen funcionando igual.

**Acceptance Scenarios**:

1. **Given** una emisora cuyo enlace apunta a un recurso `.m3u8`, **When** el usuario la reproduce, **Then** la emisora suena de forma continua.
2. **Given** una emisora cuyo enlace es una lista de reproducción `.m3u` o `.pls`, **When** el usuario la reproduce, **Then** la app localiza la primera entrada de audio reproducible contenida en la lista y suena.
3. **Given** una emisora de lista que no se puede resolver o no contiene ninguna entrada reproducible, **When** el usuario intenta reproducirla, **Then** se muestra un mensaje de error claro con opción de reintentar y la app no se bloquea ni se cierra.
4. **Given** una emisora personalizada creada por el usuario con enlace de lista, **When** se reproduce, **Then** se comporta igual que una emisora del catálogo.

---

### Edge Cases

- La otra aplicación no respeta el foco de audio (reproductores mal implementados): TolochaRadio debe pausarse igualmente cuando sea notificado y no debe producirse mezcla activa por su parte.
- Pérdida de foco mientras TolochaRadio está cargando (buffering) y no reproduciendo todavía.
- Recuperación de foco después de una interrupción transitoria cuando el usuario había pausado manualmente: no debe reanudarse por sorpresa.
- Encadenamiento de interrupciones rápidas (varias pérdidas/ganancias de foco seguidas).
- Lista `.m3u`/`.m3u8`/`.pls` con varias entradas: se usa la primera entrada reproducible; si falla, se intentan las siguientes hasta un máximo de 3 entradas por intento de reproducción, sin repetir candidatos y sin bucles.
- URLs relativas dentro de una lista: deben resolverse respecto a la ubicación de la lista.
- Entradas de la lista en HTTP no cifrado: bloqueadas por la política de seguridad de la app.
- Lista con redirecciones o servidores no disponibles: error controlado con reintento.
- Lista de emisión continua (radio) frente a lista de archivos finitos: en v1 se reproduce la primera entrada como stream continuo; las listas de pistas finitas encadenadas (tipo álbum o podcast) quedan fuera de alcance.
- Desconexión de auriculares/Bluetooth durante la reproducción.
- Interacción con una sesión de emisión en dispositivo externo (Chromecast): el dispositivo remoto gestiona su propio foco; el foco local no debe interferir con la sesión de emisión.
- Modo silencio / No molestar activo: la app no altera la configuración de sonido del sistema; la reproducción se rige por el stream de medios y el volumen del dispositivo, sin comportamiento especial adicional.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST pausar la reproducción cuando otra aplicación pasa a reproducir audio, sin mezclar sonido.
- **FR-002**: El sistema MUST solicitar el foco de audio al iniciar la reproducción, de modo que las aplicaciones que respeten el foco dejen de sonar.
- **FR-003**: Ante una pérdida de foco transitoria (interrupción breve del sistema), el sistema MUST pausar y MUST reanudar automáticamente al recuperar el foco, salvo que el usuario hubiera pausado explícitamente.
- **FR-004**: Ante una pérdida de foco permanente (otra app de medios toma el control), el sistema MUST pausar y MUST NOT reanudar automáticamente.
- **FR-005**: El comportamiento de foco de audio MUST aplicarse por igual en primer plano, en segundo plano y mediante los controles de la notificación y de dispositivos Bluetooth.
- **FR-006**: El sistema MUST poder reproducir emisoras cuyo enlace de stream sea un recurso de lista de tipo `.m3u8` (emisión HLS).
- **FR-007**: El sistema MUST poder reproducir emisoras cuyo enlace sea una lista de reproducción de texto (`.m3u` o `.pls`), resolviendo la primera entrada reproducible y reproduciéndola como un único stream continuo, omitiendo las entradas inválidas.
- **FR-008**: El soporte de listas MUST aplicarse tanto a emisoras del catálogo como a emisoras personalizadas.
- **FR-009**: Cuando una lista no pueda resolverse o no contenga entradas reproducibles, el sistema MUST mostrar un error accionable con opción de reintento, sin bloquearse ni cerrarse.
- **FR-010**: Las entradas de audio descubiertas dentro de una lista MUST cumplir las mismas restricciones de seguridad que el resto de streams (conexión cifrada; nada de URLs en claro).
- **FR-011**: El sistema MUST NOT degradar la reproducción de formatos ya soportados (streams directos de audio) al añadir el soporte de listas.
- **FR-012**: La reproducción de emisoras de lista MUST mantener los controles de reproducción y el temporizador de apagado. El preestado de disponibilidad se consulta pero MUST NOT bloquear la reproducción de listas (FR-006/FR-007). El registro de historial server-side no aplica a la reproducción directa de listas: queda registrado como deuda técnica en el issue #4 con revisión el 2026-10-09 (ver `plan.md`, Complexity Tracking).
- **FR-013**: El sistema SHOULD exponer al usuario información suficiente para distinguir un fallo de resolución de lista de un fallo de red o de emisora caída.

### Key Entities *(include if feature involves data)*

- **Sesión de reproducción**: estado actual del reproductor (inactivo, cargando, reproduciendo, pausado, error) y su relación con el foco de audio del dispositivo.
- **Fuente de stream**: origen de audio de una emisora; puede ser directo o una lista de reproducción que contiene una o más entradas de audio.
- **Lista de reproducción**: documento de referenciación (`.m3u`, `.m3u8` o `.pls`) con una o varias entradas que apuntan a audio, posiblemente con URLs relativas o redirecciones.
- **Emisora**: como ya se modela, con un enlace que puede ser directo o de lista; aplica tanto a catálogo como a personalizadas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las emisoras de prueba `.m3u8`, `.m3u` y `.pls` seleccionadas se reproducen correctamente desde la app.
- **SC-002**: En el 100% de las pruebas de foco, al iniciar audio otra app TolochaRadio se pausa en menos de 2 segundos y no se observa mezcla de audio.
- **SC-003**: En el 100% de las pruebas inversas, al iniciar TolochaRadio la otra aplicación deja de sonar.
- **SC-004**: La reanudación automática tras una interrupción transitoria funciona en al menos el 95% de una muestra mínima de 20 interrupciones, y nunca se reanuda tras una pausa manual del usuario.
- **SC-005**: No se introduce ningún fallo de reproducción en emisoras de stream directo ya soportadas (0 regresiones en la batería de pruebas existente).
- **SC-006**: Las emisoras de lista no resueltas muestran un error comprensible en el 100% de los casos probados, sin cierres inesperados de la app.
- **SC-007**: La app se mantiene estable (sin incremento de bloqueos ni cierres) durante sesiones de escucha con interrupciones de foco repetidas.

## Assumptions

- El mecanismo de foco de audio del sistema operativo es la vía estándar de coordinación entre reproductores; las apps que lo respetan cooperarán, y frente a las que no lo hacen TolochaRadio cederá igualmente el turno cuando reciba la notificación.
- El reproductor actual puede ampliarse con soporte de formatos de lista sin sustituir el motor de reproducción ni romper las restricciones de arquitectura y de seguridad ya establecidas.
- La selección de entrada dentro de una lista será la primera entrada reproducible; el listado de varias calidades (característico de HLS) se resuelve internamente sin intervención del usuario.
- La reanudación automática tras interrupción transitoria es el comportamiento por defecto, respetando siempre una pausa manual previa del usuario.
- El alcance de foco de audio aplica a la reproducción en el dispositivo local; una sesión de emisión en dispositivo externo gestiona su propio foco.
- Los formatos de lista considerados en esta versión son `.m3u8` (HLS), `.m3u` y `.pls` (listas de texto). Otros formatos de lista (p. ej. `.asx`, `.xspf`) quedan fuera de alcance salvo que se confirmen como necesarios.
- Se reutiliza la política de seguridad existente (solo conexiones cifradas) para cualquier URL descubierta dentro de una lista.
