# Bug Verification: El potenciador de voz gana nivel audible

- **Slug**: 0055-izquierdojl-20261122-podcast-voice-boost-inaudible
- **Tested**: 2026-11-22
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

La persona usuaria confirmó en la prueba manual que el potenciador de voz ahora
se nota al activarlo. El ajuste de ganancia (6 → 10 dB) queda además cubierto
por un test que mide la ganancia en dB y que fallaba con el valor anterior.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Prueba manual de la persona usuaria alternando el interruptor en un episodio | pass | Confirma que «se nota la diferencia». |
| Equivalent automated reproduction | `./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.feature.player.VoiceBoostDspTest"` | pass | 7/7; el test de ganancia exige ≥ 5,5 dB para -18 dBFS y fallaba con `MAKEUP_DB = 6`. |
| Regression suite | `./gradlew testDebugUnitTest` | pass | 132 suites, 720 tests, 0 fallos, 0 errores. |
| Lint / type-check | `./gradlew assembleDebug detekt ktlintCheck lintDebug` | pass | `BUILD SUCCESSFUL`; 0 errores de lint. |

## Output Excerpts

```text
com.izquierdojl.tolocharadio.feature.player.VoiceBoostDspTest: tests=7 failures=0 errors=0 skipped=0
  - una voz a nivel de podcast gana al menos cinco decibelios
```

```text
suites=132 tests=720 failures=0 errors=0 skipped=0
BUILD SUCCESSFUL
```

## Residual Risks

- +4 dB sobre el comportamiento anterior puede aumentar ruido de fondo o
  respiraciones en grabaciones flojas; la prueba manual no reportó molestias.
- El limitador suave evita la saturación, pero en masters muy calientes puede
  recortar picos y sonar algo más duro.
- La política sigue apagando el DSP fuera de episodios locales (radio y Cast).

## Recommendation

Cerrar el bug: verificado manualmente y cubierto por tests. Si en el futuro se
busca un efecto más «de sistema», valorar `LoudnessEnhancer` sobre el
`audioSessionId` del `ExoPlayer` como una mejora aparte.
