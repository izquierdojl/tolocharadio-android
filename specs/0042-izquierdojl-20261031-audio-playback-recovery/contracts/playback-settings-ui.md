# Contract: UI de ajustes de reproducción

**Feature**: `0042-izquierdojl-20261031-audio-playback-recovery`
**Scope**: pantalla de Configuración, sección "Reproducción".

## Ubicación y estructura

La sección "Reproducción" MUST aparecer en `SettingsScreen`, debajo de los
ajustes existentes de tema y pantalla de arranque, y antes de "Gráficas" y
"Acerca de". El usuario MUST poder acceder al selector de buffer en **tres
toques o menos** desde Configuración.

```text
Configuración
├── Tema
├── Pantalla de arranque
├── Reproducción
│   ├── Buffer de audio
│   │   ├── Ahorro de datos
│   │   ├── Equilibrado              [seleccionado por defecto]
│   │   └── Máxima estabilidad
│   └── Reanudar al recuperar conexión   [ON]
├── Gráficas
└── Acerca de
```

## Selector de buffer

| Elemento | Contrato |
|---|---|
| Etiqueta de sección | "Buffer de audio" |
| Opciones | "Ahorro de datos", "Equilibrado", "Máxima estabilidad" |
| Selección | Exactamente una; `BALANCED` en instalación nueva |
| Presentación | Tres filas con `RadioButton`, etiqueta y descripción secundaria; no se muestran milisegundos |
| Persistencia | Se guarda en DataStore antes de aplicar el cambio |
| Aplicación | Inmediata si hay una emisora sonando: una reconexión, aviso "Aplicando ajuste…" y vuelta al directo |
| Sin reproducción | Se aplica en la siguiente conexión; no se muestra error |
| Reproducción Cast | No interrumpe Cast; el perfil afecta al player local y se usará al volver al teléfono |
| Accesibilidad | `contentDescription` con etiqueta + descripción; estado seleccionado expuesto por el `RadioButton`; área táctil ≥ 48 dp |

### Descripciones

- **Ahorro de datos**: "Arranque más rápido y menor uso de memoria. Más sensible
  a los cortes en redes inestables."
- **Equilibrado**: "Buen equilibrio entre arranque y tolerancia a cortes.
  Recomendado."
- **Máxima estabilidad**: "Máxima tolerancia a cortes. Puede tardar hasta 5
  segundos más en arrancar y usa más memoria."

## Interruptor de reanudación

| Elemento | Contrato |
|---|---|
| Etiqueta | "Reanudar al recuperar conexión" |
| Descripción | "Si la emisora estaba sonando, vuelve sola cuando regresa la red." |
| Default | Activado (`true`) |
| Persistencia | DataStore; sobrevive a reinicio |
| Efecto | Con ON: al volver la red se reintenta la emisora. Con OFF: la emisora queda en pausa y el usuario debe pulsar play |
| Accesibilidad | `Switch` con etiqueta y descripción asociadas; estado ON/OFF anunciado |

## Estados de la pantalla

| Estado | Comportamiento |
|---|---|
| Carga inicial | Los valores se leen de DataStore; mientras tanto se muestra el default visual sin bloquear la pantalla |
| Cambio de perfil | Se actualiza la selección de inmediato y se persiste; si hay reproducción, se muestra el aviso de reconexión en el reproductor |
| Error al persistir | No ocurre en DataStore normal; si ocurriera, se conserva el valor anterior y se muestra un snackbar genérico sin PII |
| Rotación | La selección y el switch se conservan desde el ViewModel/DataStore |

## Fuera de alcance

- No se muestran valores numéricos de buffer ni un campo libre.
- No se permite configurar el buffer de Chromecast.
- No se añade un interruptor para desactivar los reintentos automáticos.

## Trazabilidad

Cubre FR-010, FR-011, FR-012, FR-013, FR-023 y FR-024; SC-004, SC-005 y SC-010.
