# Bug Assessment: El potenciador de voz apenas se nota

- **Slug**: 0055-izquierdojl-20261122-podcast-voice-boost-inaudible
- **Created**: 2026-11-22
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: low

## Report (verbatim or summarized)

> Revisar Potenciador de voz, creo que no funciona o no lo siento. Si el cambio
> es grande para solucionarlo lo ignoramos, puedo vivir sin ello.

No se aportó URL; el reporte es texto pegado.

## Symptom

Con el interruptor **Potenciador de voz** activo en el reproductor de un
episodio, el audio suena prácticamente igual que con el interruptor apagado. Lo
esperado es un aumento perceptible de claridad y nivel de la voz, sin saturación.

## Reproduction

1. Reproducir un episodio de podcast en local (sin Cast).
2. Abrir la hoja completa del reproductor y activar **Potenciador de voz**.
3. Alternar el interruptor varias veces con el mismo audio.
4. **Observado**: la diferencia es mínima o imperceptible. **Esperado**: un
   cambio audible de nivel/presencia de la voz.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostAudioProcessor.kt` — el `AudioProcessor` está correctamente enganchado al `DefaultAudioSink` y hace bypass cuando la política no aplica.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackPlayerFactory.kt:33` — se crea un procesador por `ExoPlayer` y se registra en el sink; no hay fallo de cableado.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostDsp.kt:116` — el compresor usa `THRESHOLD_DB = -24`, `RATIO = 3`, `MAKEUP_DB = 6`.
- `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/VoiceBoostPolicy.kt` — la política exige preferencia activa, episodio local y sin Cast; se cumple durante la reproducción normal.

## Root Cause Hypothesis

Confianza alta en el diagnóstico, media en la percepción subjetiva. El
potenciador **sí se ejecuta**: la política está activa, el `AudioProcessor`
recibe PCM 16 bits y el DSP procesa el búfer. El problema es de ajuste: con
`MAKEUP_DB = 6` y una relación 3:1 sobre -24 dBFS, un podcast ya masterizado y
«caliente» recibe entre 0 y +3 dB de ganancia, mientras que los picos altos
incluso se atenúan. El resultado es una reducción de rango dinámico sutil que no
se traduce en más volumen percibido. No es un fallo de integración.

## Proposed Remediation

**Preferred** (cambio pequeño): subir la ganancia de compensación del DSP de
6 dB a 10 dB. Es una sola constante: añade ~4 dB de nivel global sobre el
comportamiento actual, mantiene el compresor y el limitador suave, y no toca la
política ni el cableado. Añadir un test que mida la ganancia en dB para una
señal de nivel de voz (-18 dBFS) y fije un mínimo, para que el ajuste no
regrese en silencio.

**Alternativas**:
- `LoudnessEnhancer` de Android sobre el `audioSessionId` del `ExoPlayer`: efecto de loudness más «de sistema», pero exige sesión de audio, gestión de ciclo de vida y es un cambio grande; descartado por la tolerancia del usuario.
- Filtro de presencia (biquad ~2-4 kHz): más presencia sin subir tanto el nivel, pero más código y riesgo de sibilancia; no se aborda ahora.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostDsp.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostDspTest.kt`

**Tests to add or update**:
- La ganancia medida para una señal de -18 dBFS es al menos ~5,5 dB (con el nuevo ajuste son ~6 dB), manteniendo la ausencia de saturación.

## Risks & Considerations

- Subir el makeup puede hacer más audible el ruido de fondo y las respiraciones; es el compromiso habitual de un compresor.
- En masters ya limitados, el limitador suave recorta los picos; hay que escuchar en dispositivo antes de dar el cambio por bueno.
- Cambio de una constante: sin migraciones, sin contrato, sin impacto en radio ni Cast (la política sigue apagando el DSP fuera de episodios locales).

## Open Questions

- [NEEDS CLARIFICATION: confirmación auditiva en dispositivo tras el ajuste; si el usuario prefiere no arriesgar el timbre, se puede cerrar como «no se corrige».]
