# Quickstart: validar el modo Podcast

Objetivo: comprobar de extremo a extremo que la app replica la web (contratos en `contracts/`, modelo en `data-model.md`).

## Prerrequisitos
- Servidor de pruebas con `PODCASTS_ENABLED=true` (o equivalente) y cuenta con credenciales configuradas en la app; un segundo servidor/dispositivo opcional para probar el 409 de cola.
- Emulador o dispositivo (API 26+; descargas visibles en Descargas) con red normal.
- Compilación: `./gradlew assembleDebug`; unitarios: `./gradlew testDebugUnitTest`; estática: `./gradlew detekt ktlintCheck lintDebug`.

## Escenarios (resultado esperado entre paréntesis)
1. **Modo y arranque**: con podcasts habilitados aparece `Radio | Podcasts`; cambiar no corta la reproducción (SC-001). En Configuración elegir sección inicial Podcasts, reiniciar → abre en Podcasts. Con un servidor sin podcasts no hay rastro del modo (SC-009).
2. **Descubrir→suscribir→primera escucha** (< 3 min, SC-002): buscar texto, suscribirse desde el resultado; dar de alta una RSS manual válida y una inválida (motivo sin fantasma); importar OPML (sin duplicados, con informe) y exportar.
3. **Reproducción y reanudación**: reproducir, cambiar velocidad, saltar ±15/30, buscar en pista; pausar, salir y continuar (desviación ≤ 10 s, 9/10, SC-003). Terminar con cola → auto-avance sin tocar nada (9/10, SC-007). Descartar uno de continuar (sigue en su programa).
4. **Cola**: añadir al final/a continuación (existente = mover, sin duplicar); reordenar por arrastre; quitar; vaciar manteniendo el episodio en curso (SC-004). Provocar 409 (reordenar en dos dispositivos) → aviso + recarga sin perder reproducción.
5. **Favoritos y ficha**: marcar/desmarcar corazón (sin tocar favoritas de radio); abrir ficha desde título (hoja inferior, sin detener) y actuar desde ella.
6. **Portada y ficha de programa**: continuar + novedades + programas + resumen de cola (< 3 s, SC-005); filtros (sin empezar/en progreso/completados) y órdenes (recientes/duración/título); búsquedas con resultado o vacío explicativo (< 2 s, SC-006).
7. **Descargas**: con descargas habilitadas, descargar (progreso → descargado); reproducir el episodio → usa el archivo local (SC-010, modo avión tras descarga como prueba). Sin descargas, la opción no aparece. Archivo en origen caído o espacio insuficiente → aviso + reintentar/online.
8. **Estadísticas**: resumen coherente (tiempo, programas, empezados/completados, tasa) y desgloses; solo cuenta activa.
9. **Usabilidad** (SC-008): 10 participantes → ≥ 9 completan escucha sin ayuda.

## Calidad obligatoria (gates de merge)
- `testDebugUnitTest` en verde (JUnit + Turbine + MockK; serialización de DTOs podcast contra ejemplos del contrato; funciones puras de cola/OPML; ViewModels y repos).
- `detekt ktlintCheck lintDebug` sin errores (los warnings nuevos se justifican en la PR).
- Compose UI solo en flujos críticos: cambio de modo, reproducir/pausar episodio, cola básica, ficha.
