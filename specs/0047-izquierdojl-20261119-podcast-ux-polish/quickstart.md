# Quickstart: Mejoras de experiencia podcast

**Feature**: `0047-izquierdojl-20261119-podcast-ux-polish` | **Date**: 2026-11-19

Guía de validación end-to-end. No contiene implementación; los detalles de comportamiento están en [spec.md](./spec.md), [data-model.md](./data-model.md) y [contracts/](./contracts/).

---

## 1. Requisitos previos

- JDK 17 y Android SDK con `compileSdk 37`.
- Emulador o dispositivo API 26+ (recomendado probar 26, 33 y 37).
- Servidor TolochaRadio accesible con:
  - Podcasts habilitados.
  - Al menos una suscripción con varios episodios.
  - Descargas habilitadas (si se quiere validar ese flujo).
- Cuenta con credenciales guardadas en la app.
- Datos de prueba:
  - Un episodio parcialmente escuchado (≥ 2 minutos) que esté en la cola.
  - Un episodio completado en la cola.
  - Un podcast con título largo y un episodio con nombre de podcast largo.
  - Una emisora de radio en directo.

---

## 2. Comandos

Windows (PowerShell 7):

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat detekt ktlintCheck lintDebug
.\gradlew.bat connectedDebugAndroidTest   # dispositivo/emulador
```

Linux/CI:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew detekt ktlintCheck lintDebug
./gradlew connectedDebugAndroidTest
```

Instalación rápida:

```bash
./gradlew installDebug
```

---

## 3. Validación manual

### 3.1 Reanudación desde la cola (P1)

1. Reproduce un episodio de unos 30 minutos durante 2 minutos.
2. Pausa y vuelve a Inicio.
3. Abre Cola y selecciona ese mismo episodio.
4. **Esperado**: arranca cerca del minuto 2 (± 5 s), no en 0.
5. Marca otro episodio como completado y selecciónalo desde la cola.
6. **Esperado**: arranca desde el principio.
7. Deja terminar un episodio con otro a medias a continuación.
8. **Esperado**: el siguiente también reanuda su posición guardada.

### 3.2 Notificación multimedia (P1)

1. Reproduce un episodio y bloquea la pantalla.
2. En la notificación, pulsa retroceder.
3. **Esperado**: −15 s; al abrir la app, la posición coincide.
4. Pulsa avanzar.
5. **Esperado**: +30 s.
6. Pausa y repite.
7. **Esperado**: los botones siguen disponibles.
8. Con el episodio a menos de 15 s del inicio, retrocede.
9. **Esperado**: queda en 0 sin error.
10. Reproduce radio en directo y mira la notificación.
11. **Esperado**: no hay retroceder/avanzar; stop y mute siguen.

### 3.3 Cabecera, cajón y barra inferior (P2)

1. En un móvil pequeño (o emulador de 320 dp), pon la letra al 200 %.
2. **Esperado**: hamburguesa, logotipo (o emblema), selector y Cast sin solaparse.
3. Abre el menú de hamburguesa.
4. **Esperado**: Temporizador, Servidores y Configuración con etiquetas.
5. Entra en Configuración y vuelve.
6. **Esperado**: regresas a la misma pantalla y la reproducción no se reinicia.
7. Activa el temporizador y abre el cajón.
8. **Esperado**: ves los minutos restantes y puedes cancelarlo.
9. Revisa la barra inferior en Radio y en Podcasts.
10. **Esperado**: Configuración no aparece; los demás destinos conservan orden y etiqueta completa.

### 3.4 Potenciador de voz (P2)

**Muestra de SC-006**: 5 episodios de voz y 2 de música, escuchados con auriculares y altavoz.

1. Reproduce un episodio con voces bajas.
2. Activa “Potenciador de voz” en la hoja del reproductor.
3. **Esperado**: se oye más claro y alto sin pausas ni distorsión.
4. Desactívalo.
5. **Esperado**: vuelve el audio original.
6. Actívalo, cambia de episodio y reinicia la app.
7. **Esperado**: sigue activo.
8. Cambia a radio.
9. **Esperado**: la radio no recibe el efecto.
10. Emite a Chromecast.
11. **Esperado**: el efecto no se aplica a la salida remota.

### 3.5 Insignia de cola (P3)

1. Vacía la cola y mira la barra inferior.
2. **Esperado**: sin insignia.
3. Añade 3 episodios.
4. **Esperado**: insignia “3”.
5. Quita uno.
6. **Esperado**: “2”.
7. Reproduce uno de la cola.
8. **Esperado**: el contador excluye el que suena.
9. Añade más de 99 (o simula el límite).
10. **Esperado**: “99+”.

### 3.6 Descarga con porcentaje (P3)

1. Pulsa descargar en un episodio.
2. **Esperado**: el icono se convierte en un anillo animado con porcentaje creciente.
3. Cambia de pantalla y vuelve.
4. **Esperado**: el porcentaje sigue visible y coherente.
5. Espera a que termine.
6. **Esperado**: estado descargado, sin porcentaje.
7. Repite con el modo avión activado tras iniciar la descarga.
8. **Esperado**: vuelve a descargable con aviso en español.
9. Pulsa dos veces seguidas sobre descargar.
10. **Esperado**: no se crea una segunda descarga.

### 3.7 Tarjetas de programa y episodio (P3)

1. Abre Inicio, Suscripciones y Favoritos con títulos largos.
2. **Esperado**: los títulos se leen hasta dos líneas; el nombre del podcast no queda cortado a un resto.
3. **Esperado**: las acciones están en una fila propia debajo del texto, sin pisarlo.
4. **Esperado**: todas las tarjetas de una fila tienen la misma altura.
5. Sube la letra al 200 %.
6. **Esperado**: la tarjeta crece; no hay solapamientos.
7. Prueba reproducir, favorito y baja de suscripción.
8. **Esperado**: siguen funcionando y anunciándose con TalkBack.

---

## 4. Verificación automatizada

| Área | Test |
|------|------|
| Reanudación | `EpisodeResumeTest`, `EpisodePlayerViewModelTest` |
| Notificación | `playback-notification.md` + prueba manual en API 26/33/37 |
| Cabecera/cajón | `TolochaTopBarTest`, `TolochaDrawerTest`, `TolochaNavigationBarTest` |
| Potenciador | `VoiceBoostDspTest`, `VoiceBoostPolicyTest`, `VoiceBoostToggleTest` |
| Insignia | `QueueBadgeTest`, `QueueBadgeViewModelTest` |
| Descarga | `DownloadsRepoTest`, `DownloadProgressButtonTest` |
| Tarjetas | `PodcastCardsTest` |

---

## 5. Problemas frecuentes

| Síntoma | Comprobación |
|---------|--------------|
| El episodio sigue empezando en 0 | Verifica que `GET /podcasts/episodes/{id}/progress` responde y que la cola trae `progress` o el respaldo lo obtiene |
| No aparecen los botones de seek | Comprueba que hay un episodio activo (no radio), que la API es ≥ 26 y que el contenido es desplazable |
| El potenciador no hace nada | Debe estar activo, con episodio local y sin Cast; revisa que el player se recreó con el renderers factory |
| La insignia no coincide | Abre la cola para forzar la recarga; revisa que el episodio en curso esté excluido |
| El porcentaje no avanza | El `DownloadManager` puede no informar el total; debe mostrarse indeterminado, no un 0 % fijo |
| Las tarjetas siguen desiguales | Comprueba que el título usa `minLines = 2` y que la fila de acciones tiene altura fija |

### 3.8 Retención de descargas (extensión 2026-11-19)

1. Abre Configuración > Descargas > Retención de descargas.
2. **Esperado**: opciones 7, 14, 30, 60, 90 y «Nunca», con 30 días por defecto.
3. Con una descarga completada hace más de 30 días, abre Podcasts o cambia la retención.
4. **Esperado**: la descarga antigua desaparece; las recientes y el episodio en curso se conservan.
5. Elige «Nunca» y comprueba que no se borra ninguna descarga automáticamente.
