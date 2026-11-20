# Quickstart: Refinamientos de experiencia podcast

**Feature**: `0048-izquierdojl-20261119-podcast-ux-refinements` | **Date**: 2026-11-19

Guía de validación end-to-end. No contiene implementación; los detalles de comportamiento están en [spec.md](./spec.md), [data-model.md](./data-model.md) y [contracts/](./contracts/).

---

## 1. Requisitos previos

- JDK 17 y Android SDK con `compileSdk 37`.
- Emulador o dispositivo API 26+ (recomendado probar 26, 33 y 37).
- Servidor TolochaRadio accesible con:
  - Podcasts habilitados.
  - Al menos una suscripción con varios episodios.
  - Estadísticas de podcast con datos de escucha.
  - Historial de radio para comparar las gráficas.
- Cuenta con credenciales guardadas en la app.
- Datos de prueba:
  - Un episodio en curso con progreso parcial.
  - Un episodio pendiente sin empezar y otro completado en la cola.
  - Un episodio de más de una hora.
  - Un episodio con duración desconocida o no informada.
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

### 3.1 Cola con episodio en curso (P1)

1. Reproduce un episodio y deja una cola con al menos tres pendientes.
2. Abre Cola.
3. **Esperado**: el episodio en curso aparece arriba con portada, programa, título, tiempo transcurrido, duración total y barra de avance; los pendientes aparecen debajo.
4. Comprueba que el episodio en curso no se repite en la lista.
5. Desplaza la lista de pendientes hacia abajo.
6. **Esperado**: el bloque del episodio en curso sigue visible en la parte superior.
7. Pausa y vuelve a abrir la cola.
8. **Esperado**: el bloque sigue visible y muestra la última posición.
9. Avanza 30 segundos y observa el bloque.
10. **Esperado**: el tiempo y la barra se actualizan en ≤ 2 s.
11. Vacía los pendientes dejando sonar el episodio.
12. **Esperado**: se ve el bloque y un mensaje de que no hay más episodios en cola.
13. Detén el episodio.
14. **Esperado**: el bloque y la tarjeta inferior desaparecen; no quedan cifras residuales.

### 3.2 Duración y minutos restantes en la cola (P2)

1. Abre una cola con un episodio sin empezar, uno a medias y uno completado.
2. **Esperado**:
   - sin empezar: solo duración;
   - a medias: duración y "Quedan N min" en una línea secundaria;
   - completado: solo duración, sin restante.
3. Reproduce el episodio a medias unos minutos y vuelve a la cola.
4. **Esperado**: el restante se ha reducido de forma coherente tras el refresco.
5. Usa un episodio al que le quede menos de un minuto.
6. **Esperado**: "Queda menos de 1 min", nunca "0 min" ni un valor negativo.
7. Usa un episodio con duración desconocida.
8. **Esperado**: no se muestra duración ni restante inventados.
9. Pon la letra al 200 %.
10. **Esperado**: la línea se lee completa y no se solapa con el título ni las acciones.

### 3.3 Detalle del episodio: detener y favorito (P1)

1. Abre el reproductor completo de un episodio.
2. **Esperado**: "Detener" es un icono con etiqueta accesible, sin texto visible, alineado con los demás controles.
3. Pulsa favorito.
4. **Esperado**: el corazón cambia, la reproducción no se pausa ni se cierra y el cambio se ve en la ficha, en una tarjeta de Inicio y en la lista de Favoritos en ≤ 2 s.
5. Vuelve a pulsar favorito.
6. **Esperado**: se desmarca y el resto de vistas lo reflejan.
7. Cierra y reabre el detalle.
8. **Esperado**: el estado persiste.
9. Activa el modo avión y pulsa favorito.
10. **Esperado**: se conserva el estado anterior, se muestra un aviso en español y se puede reintentar.
11. Pulsa detener.
12. **Esperado**: la reproducción se detiene, el detalle se cierra y la tarjeta inferior desaparece.

### 3.4 Tarjeta inferior con tiempos (P2)

1. Reproduce un episodio de 45 minutos.
2. **Esperado**: la tarjeta muestra el transcurrido a la izquierda y el restante con signo menos a la derecha.
3. Espera unos segundos.
4. **Esperado**: ambas cifras avanzan al menos una vez por segundo y coinciden con el reproductor.
5. Pausa.
6. **Esperado**: las cifras quedan fijas.
7. Salta +30 s y −15 s.
8. **Esperado**: ambas cifras reflejan la nueva posición de inmediato.
9. Deja terminar el episodio con otro en la cola.
10. **Esperado**: las cifras se reinician para el siguiente, sin arrastrar las del anterior.
11. Prueba un episodio de más de una hora.
12. **Esperado**: formato con horas y restante nunca negativo.
13. Prueba un episodio con duración desconocida.
14. **Esperado**: solo transcurrido, sin restante inventado.
15. Pon la letra al 200 %.
16. **Esperado**: las cifras se leen sin solaparse y la tarjeta sigue abriendo el reproductor completo.

### 3.5 Pantalla de arranque de podcast (P2)

1. Abre Configuración.
2. **Esperado**: hay "Pantalla de arranque en radio" y "Pantalla de arranque en podcast", claramente separadas.
3. Elige "Cola" en podcast.
4. Cierra y reabre la app con la sección inicial Podcasts.
5. **Esperado**: abre Cola.
6. Repite con "Suscripciones" y "Descubrir".
7. **Esperado**: abre el destino elegido.
8. Cambia la pantalla de arranque de radio.
9. **Esperado**: la preferencia de podcast no cambia.
10. Vuelve a la app sin reiniciar tras cambiar la preferencia.
11. **Esperado**: no hay navegación abrupta; se aplica en el siguiente arranque.
12. Cambia a un servidor sin podcasts y arranca con la sección inicial Podcasts.
13. **Esperado**: abre la pantalla de arranque de radio, sin errores y sin selector de podcast.
14. Elige Cola y arranca con la cola vacía.
15. **Esperado**: abre Cola con su estado vacío habitual.

### 3.6 Gráficas de radio o podcast (P3)

1. Con historial de radio y podcast, pon el selector en Radio y abre Configuración → Gráficas.
2. **Esperado**: encabezado "Tus estadísticas de radio" y gráficas de radio.
3. Vuelve, cambia el selector a Podcasts y abre Gráficas.
4. **Esperado**: encabezado "Tus estadísticas de podcast" y gráficas de podcast: resumen, evolución, programas, episodios, hábitos, categorías y actividad reciente.
5. Comprueba que no aparece ninguna emisora ni dato de radio en la vista de podcast.
6. Con Gráficas de radio abierta, cambia el modo a Podcasts.
7. **Esperado**: no queda visible el conjunto de radio; se muestra el de podcast o se sale de la vista sin contenido obsoleto.
8. Repite el cambio inverso.
9. **Esperado**: mismo comportamiento.
10. Usa un servidor sin podcasts.
11. **Esperado**: Gráficas muestra radio y no una vista de podcast vacía o rota.
12. En modo Podcasts sin historial, abre Gráficas.
13. **Esperado**: estado vacío propio de podcast, sin cifras inventadas ni gráficas de radio.
14. Activa el modo avión y abre Gráficas.
15. **Esperado**: error en español con "Reintentar"; no se sustituye por el otro modo.
16. Provoca un error de credenciales.
17. **Esperado**: error con "Editar servidor".
18. En modo Radio, usa el filtro de periodo.
19. **Esperado**: sigue funcionando solo sobre radio.

### 3.7 Accesibilidad y adaptación

1. Activa TalkBack y recorre la cola, la tarjeta inferior, el detalle, Configuración y Gráficas.
2. **Esperado**: las etiquetas nuevas están en español y describen acción y estado.
3. Pon el tamaño de letra al 200 % y prueba un móvil estrecho (320 dp).
4. **Esperado**: no hay solapamientos ni recortes en tiempos, metadatos, controles ni encabezados.
5. Gira la pantalla en las pantallas afectadas.
6. **Esperado**: el estado y la reproducción se conservan.

---

## 4. Pruebas automatizadas

- Unitarias JVM:
  - `EpisodeTimeTest`
  - `StartDestinationTest`
  - `PodcastStatsViewModelTest`
  - `SettingsViewModelTest`
  - `EpisodeActionsViewModelTest`
  - `EpisodePlayerViewModelTest`
- Compose:
  - `QueueScreenTest`
  - `EpisodeMiniPlayerTest`
  - `EpisodeFullPlayerSheetTest`
  - `SettingsPodcastStartTest`
  - `StatsEntryFlowTest`
  - `PodcastStatsScreenTest`

Comando recomendado antes del merge:

```bash
./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug
```

Y, con emulador/dispositivo:

```bash
./gradlew connectedDebugAndroidTest
```

---

## 5. Criterios de aceptación rápidos

- [ ] El episodio en curso se ve arriba en la cola y no se duplica.
- [ ] Cada fila muestra duración y, si procede, restante en minutos.
- [ ] Detener es un icono y el favorito funciona sin interrumpir.
- [ ] La tarjeta inferior muestra transcurrido y restante.
- [ ] La pantalla de arranque de podcast se elige y persiste.
- [ ] Gráficas muestra radio o podcast según el modo, con paridad visual.
- [ ] Sin regresiones en reproducción, cola, favoritos, modo, configuración ni estadísticas.

---

## 6. Resultado de la implementación (2026-11-20)

- `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck` y `lintDebug`: **verdes**.
- `connectedDebugAndroidTest` en `Medium_Phone_API_37.0`: **BUILD SUCCESSFUL**.
- Corrección verificada del X de la tarjeta inferior: `EpisodePlayerViewModel.stop()` limpia el episodio y `EpisodeMiniPlayerTest` comprueba que el botón no abre el reproductor.
- Detener del reproductor completo: icono `Close` con etiqueta "Detener" (`EpisodeFullPlayerActions`).
- Validación manual: recorrido 3.1–3.7 y TalkBack/200 % verificados por el usuario (T052/T053, 2026-11-20).
