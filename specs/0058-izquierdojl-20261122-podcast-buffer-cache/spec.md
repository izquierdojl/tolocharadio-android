# Feature Specification: Buffer profundo y caché en disco para episodios

**Feature Branch**: `0058-izquierdojl-20261122-podcast-buffer-cache`

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "Al entrar en una zona sin conexión se corta la reproducción de un podcast. Queremos ampliar el buffer de los episodios para evitar cortes en zonas de poca cobertura, sin empeorar la radio en directo. En Wi-Fi se puede cachear el episodio completo; con datos móviles, solo un margen pequeño."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Seguir escuchando al perder cobertura unos minutos (Priority: P1)

Una persona escucha un episodio en streaming mientras camina. Al entrar en una
zona sin cobertura, el audio sigue sonando durante varios minutos gracias al
audio ya descargado por delante. Cuando vuelve la señal, la reproducción no se
ha interrumpido ni ha cambiado de posición.

**Why this priority**: Es el problema reportado y el que aporta valor inmediato.
Sin esto, el resto de la feature no tiene sentido.

**Independent Test**: Reproducir un episodio en streaming con red estable, dejar
que se llene el buffer, cortar la red 3 minutos y comprobar que el audio no se
detiene; al devolver la red, sigue en la misma posición.

**Acceptance Scenarios**:

1. **Given** un episodio en streaming con el buffer lleno, **When** se pierde la
   cobertura durante 3 minutos, **Then** la reproducción continúa sin cortes.
2. **Given** un episodio en streaming en una red estable, **When** se pulsa play,
   **Then** el audio empieza en menos de 3 segundos, sin esperar a llenar todo el
   buffer profundo.
3. **Given** la radio en directo reproduciéndose, **When** se aplica el nuevo
   buffer de episodios, **Then** la latencia del directo no aumenta respecto al
   comportamiento actual.
4. **Given** un episodio descargado (archivo local), **When** se reproduce,
   **Then** no se aplica buffer profundo ni se pide red.

---

### User Story 2 - Escuchar sin conexión sin descargar a mano (Priority: P2)

La misma persona quiere seguir oyendo el episodio aunque se quede sin cobertura,
sin haber tenido que descargarlo antes. Con Wi-Fi, la app aprovecha para guardar
el episodio en curso completo; con datos móviles se limita a un margen pequeño
para no gastar la tarifa. También puede retroceder en lo que ya ha escuchado
estando sin red.

**Why this priority**: Convierte el buffer temporal en audio persistente y cubre
el caso real de perder cobertura a mitad de un trayecto. Es la diferencia entre
«aguantar un túnel» y «poder escuchar sin red».

**Independent Test**: Con Wi-Fi, empezar un episodio y esperar a que termine la
precarga; activar modo avión y comprobar que se reproduce de principio a fin. Con
datos móviles, comprobar que solo hay ~5 minutos por delante y 50 MB de caché.

**Acceptance Scenarios**:

1. **Given** 5 minutos ya escuchados de un episodio, **When** se activa el modo
   avión y se retrocede 3 minutos, **Then** el audio se reproduce sin red.
2. **Given** un episodio en curso sobre Wi-Fi, **When** termina la precarga,
   **Then** el episodio completo se puede reproducir sin red.
3. **Given** un episodio en curso sobre datos móviles, **When** se pierde la
   cobertura, **Then** se puede volver a escuchar lo ya oído y hasta 5 minutos
   por delante de lo cacheado, sin superar 50 MB.
4. **Given** el mismo episodio con la URL firmada renovada, **When** se vuelve a
   reproducir, **Then** la caché existente se reutiliza en lugar de descargarse
   otra vez.
5. **Given** un episodio descargado completo, **When** se reproduce, **Then** se
   usa el archivo local y no se duplica en la caché.

---

### User Story 3 - La caché no crece sin control (Priority: P3)

La persona quiere que la app no ocupe espacio indefinidamente. Puede ver cuánto
ocupa, liberarlo y ajustar el comportamiento; la app descarta lo más antiguo
cuando se llena.

**Why this priority**: Sin límite, la feature se convierte en un problema de
almacenamiento y de consumo de datos. Se puede lanzar P1 y P2 con los límites por
defecto y añadir el control visible después.

**Independent Test**: Llenar la caché por encima del límite configurado y
comprobar que el tamaño total no lo supera y que los episodios más antiguos son
los que se descartan.

**Acceptance Scenarios**:

1. **Given** una caché con el límite alcanzado, **When** se reproduce otro
   episodio, **Then** se libera espacio de las entradas menos usadas y el tamaño
   total no supera el límite de la red activa.
2. **Given** la pantalla de ajustes, **When** se pulsa «Liberar espacio», **Then**
   la caché se vacía y se muestra el espacio liberado.
3. **Given** una caché con contenido, **When** se reproduce el mismo episodio,
   **Then** las entradas usadas se marcan como recientes y no se descartan antes
   que las antiguas.

---

### Edge Cases

- **Pérdida de red antes de llenar el buffer**: el margen es el que haya; la
  reanudación tras el corte la cubre el bug 0057, no esta feature.
- **Cambio de Wi-Fi a datos móviles a mitad de precarga**: se detiene la precarga
  completa, se recorta al límite móvil y el audio no se interrumpe.
- **Cambio de datos móviles a Wi-Fi**: se reanuda la precarga completa.
- **Roaming o Ahorro de datos activo**: se trata como red medida; no se precarga
  el episodio completo.
- **Cambio de episodio**: la caché del anterior se conserva hasta que el límite o
  el uso la descarten; no debe bloquear la reproducción del nuevo.
- **Cambio de perfil de buffer en caliente mientras suena un episodio**: no debe
  interrumpir el episodio ni arrancar la radio.
- **Poco espacio libre en el dispositivo**: la caché se recorta o se desactiva
  sin impedir la reproducción en streaming.
- **Velocidad de reproducción 2x**: el buffer y la caché se miden en audio, no en
  tiempo de reloj; la cobertura efectiva se reduce a la mitad en tiempo real.
- **Saltar hacia delante más allá de lo cacheado**: requiere red; debe mostrar el
  estado de carga habitual, no un error.
- **Cast**: el receptor gestiona su propio buffer; el móvil no debe imponer el
  perfil de episodios a la sesión remota.
- **URL firmada caducada**: la caché debe seguir siendo válida porque la clave es
  el episodio, no la URL.
- **Parar el episodio o cerrar la app durante la precarga**: se cancela la
  descarga en curso y se conserva lo ya cacheado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El buffer de los episodios MUST ser independiente del buffer de la
  radio; cambiar uno no debe alterar el otro.
- **FR-002**: El perfil de episodios MUST permitir un margen de al menos 2
  minutos y un objetivo de 4 minutos por defecto.
- **FR-003**: La radio en directo MUST conservar su latencia actual; esta feature
  no puede aumentarla.
- **FR-004**: El arranque de un episodio MUST NOT esperar a llenar el buffer
  profundo ni la precarga; con red normal debe empezar en menos de 3 segundos.
- **FR-005**: El audio ya reproducido de un episodio MUST conservarse en el
  dispositivo y poder reproducirse sin red.
- **FR-006**: La caché MUST sobrevivir al cierre de la app y al reinicio del
  dispositivo.
- **FR-007**: La identidad de una entrada de caché MUST ser el episodio, no la
  URL firmada; renovar la URL no debe invalidar el audio guardado.
- **FR-008**: La caché MUST tener un límite de tamaño configurable y descartar
  primero las entradas menos usadas recientemente. Por defecto: 500 MB en redes
  no medidas y 50 MB en redes medidas.
- **FR-009**: Si existe una descarga completa del episodio, la reproducción MUST
  usar el archivo local y MUST NOT duplicarlo en la caché.
- **FR-010**: La caché MUST contener solo audio; MUST NOT almacenar credenciales,
  tokens ni cabeceras de autorización.
- **FR-011**: Las operaciones de caché y precarga MUST NOT bloquear el hilo
  principal ni provocar cortes en la reproducción.
- **FR-012**: La persona usuaria MUST poder consultar el espacio ocupado y
  liberarlo desde Ajustes.
- **FR-013**: El reporte de progreso de escucha MUST seguir funcionando igual con
  audio servido desde la caché.
- **FR-014**: En redes no medidas (Wi-Fi/Ethernet), la app MUST precargar el
  episodio en curso completo, sin superar el límite de caché.
- **FR-015**: En redes medidas, la app MUST limitar la precarga a 5 minutos por
  delante del punto de reproducción y al límite de 50 MB.
- **FR-016**: Al cambiar el tipo de red, la app MUST reajustar la política de
  caché y precarga sin interrumpir el audio.
- **FR-017**: La app MUST respetar el Ahorro de datos del sistema y tratar el
  roaming como red medida.
- **FR-018**: Parar el episodio o cerrar la app MUST cancelar la precarga en
  curso, conservando lo ya cacheado.

### Key Entities

- **Perfil de buffer de episodios**: límites mínimo y máximo de audio por delante,
  umbral de arranque y umbral tras un rebuffer.
- **Entrada de caché**: episodio, bytes guardados, último uso y fecha de creación.
- **Política de red**: tipo de red (medida / no medida), límite aplicable y
  margen de precarga permitido.
- **Ajuste de caché**: límite en redes no medidas, límite en redes medidas y
  estado de activación.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con el buffer lleno, una pérdida de cobertura de 3 minutos no
  interrumpe el audio del episodio (hoy se corta a los ~50 s).
- **SC-002**: La latencia del directo de radio no varía más de 2 segundos
  respecto al comportamiento actual.
- **SC-003**: Se pueden volver a escuchar los últimos 5 minutos de un episodio sin
  ninguna conexión.
- **SC-004**: El buffer en memoria del episodio no supera 8 MB.
- **SC-005**: La caché nunca supera 500 MB en Wi-Fi ni 50 MB en datos móviles, y
  se reduce sola al alcanzar el límite de la red activa.
- **SC-006**: Un episodio en streaming empieza a sonar en menos de 3 segundos con
  red normal.
- **SC-007**: Los tests de reproducción de radio existentes siguen pasando sin
  cambios de comportamiento.
- **SC-008**: Tras completar la precarga en Wi-Fi, un episodio de 60 minutos se
  reproduce sin red de principio a fin.
- **SC-009**: En datos móviles, la precarga nunca supera 5 minutos por delante del
  punto de reproducción.

## Assumptions

- Los episodios se sirven en MP3/AAC progresivo a 96–192 kbps; el cálculo de
  memoria y los límites de 500/50 MB usan ese rango.
- «Red no medida» es la que el sistema marca como no medida (Wi-Fi o Ethernet).
  Roaming y Ahorro de datos se consideran redes medidas.
- Existe un único reproductor compartido entre radio y episodios; aplicar un
  buffer distinto por tipo exigirá intercambiarlo al cambiar de contenido o
  separar el reproductor de VOD. La decisión se toma en el plan.
- La descarga completa existente se mantiene y sigue siendo la vía preferente
  para escucha sin conexión planificada; la precarga automática no la sustituye.
- El almacenamiento de caché es interno de la app y no requiere permisos nuevos.
- La reanudación tras un corte de red la resuelve el bug 0057; esta feature solo
  amplía el margen y conserva audio.
