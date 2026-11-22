# Bug Fix: Hacer perceptible el potenciador de voz

- **Slug**: 0055-izquierdojl-20261122-podcast-voice-boost-inaudible
- **Fixed**: 2026-11-22
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

El potenciador ya estaba correctamente enganchado al `AudioSink`, pero su
compensación de 6 dB apenas cambiaba el nivel percibido en podcasts ya
masterizados. Se sube la ganancia de compensación a 10 dB (≈ +4 dB sobre el
comportamiento anterior) manteniendo el compresor y el limitador suave.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostDsp.kt` | modified | `MAKEUP_DB` de 6 a 10 dB. Sin cambios de política, cableado ni contrato. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostDspTest.kt` | test added | Mide la ganancia en dB para una señal de nivel de voz. |

## Diff Highlights

```kotlin
const val MAKEUP_DB = 10.0
```

```kotlin
@Test
fun `una voz a nivel de podcast gana al menos cinco decibelios`() {
    // Pico a -18 dBFS, el nivel habitual de una voz ya normalizada.
    val input = sine(amplitude = 0.125, frames = 48_000)
    val output = input.copyOf()
    VoiceBoostDsp(sampleRate, channels = 1).process(output, output.size)

    val gainDb = 20.0 * log10(rms(output, 24_000, output.size) / rms(input, 24_000, input.size))

    assertTrue("ganancia insuficiente: $gainDb dB", gainDb >= 5.5)
}
```

## Tests Added or Updated

- `VoiceBoostDspTest::una voz a nivel de podcast gana al menos cinco decibelios` — fija un mínimo de ganancia para una señal de -18 dBFS; con el ajuste anterior (~2 dB) fallaba.

## Local Verification

- Commands run: `./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.feature.player.VoiceBoostDspTest"` → `BUILD SUCCESSFUL`, 7/7 tests (el nuevo fallaba antes del cambio).
- Commands run: `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` → `BUILD SUCCESSFUL`, 132 suites / 720 tests, 0 fallos.
- Manual checks: **pendiente de escucha en dispositivo**. El cambio es de una constante y el limitador evita la saturación, pero el timbre final debe validarse con audio real (podcast con voz y música).

## Deviations from Assessment

- Ninguna en el código. El assessment contemplaba descartar el cambio si resultaba grande; al ser una sola constante se aplicó, dejando la validación auditiva para `/speckit.bug.test`.

## Follow-ups

- Escuchar en dispositivo con el interruptor on/off: si el ruido de fondo o la sibilancia molestan, bajar `MAKEUP_DB` a 8 o subir el umbral.
- Si se busca un efecto más «de sistema» (no solo más nivel), valorar `LoudnessEnhancer` sobre el `audioSessionId` del `ExoPlayer`; es un cambio mayor y quedó descartado.
