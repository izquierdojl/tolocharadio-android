package com.izquierdojl.tolocharadio.domain.playback

/** Contenido activo que determina si un salto es posible (data-model.md). */
enum class PlaybackContent {
    /** Sin episodio ni emisora activos. */
    Idle,

    /** Radio en directo: nunca permite salto. */
    LiveRadio,

    /** Episodio con reproducción activa. */
    EpisodePlaying,

    /** Episodio pausado: permite saltar sin reanudar. */
    EpisodePaused,
}

/** Comando lógico de salto, independiente de la tecla física. */
enum class HeadsetCommand {
    SkipBack,
    SkipForward,
    Next,
    Previous,
}

/** Origen del salto; decide si el interruptor de gestos aplica. */
enum class SkipSource {
    /** Pulsación larga de volumen o anterior/siguiente físicos. */
    HeadsetGesture,

    /** Botones del reproductor, notificación, bloqueo o comandos de sesión. */
    ExplicitControl,
}

/** Acción decidida por [HeadsetControlPolicy]. */
sealed interface HeadsetAction {
    /** Mover la posición del episodio [deltaMs] (positivo adelante). */
    data class Skip(val deltaMs: Long) : HeadsetAction

    /** No ejecutar ninguna acción. */
    data object Ignore : HeadsetAction
}

/**
 * Política pura de saltos (spec 0060, FR-006/FR-009/FR-012/FR-013).
 *
 * - Solo los episodios activos permiten salto.
 * - `gesturesEnabled = false` solo bloquea [SkipSource.HeadsetGesture]; los
 *   controles explícitos siguen funcionando.
 * - La radio en directo y el estado inactivo se ignoran.
 */
object HeadsetControlPolicy {
    /** Decide la acción para [command] en [content] desde [source]. */
    fun decide(
        content: PlaybackContent,
        command: HeadsetCommand,
        source: SkipSource,
        settings: HeadsetControlSettings,
    ): HeadsetAction {
        val episodeActive =
            content == PlaybackContent.EpisodePlaying || content == PlaybackContent.EpisodePaused
        if (!episodeActive) return HeadsetAction.Ignore
        if (source == SkipSource.HeadsetGesture && !settings.gesturesEnabled) {
            return HeadsetAction.Ignore
        }
        return when (command) {
            HeadsetCommand.SkipBack,
            HeadsetCommand.Previous,
            -> HeadsetAction.Skip(-settings.skipBackMs)

            HeadsetCommand.SkipForward,
            HeadsetCommand.Next,
            -> HeadsetAction.Skip(settings.skipForwardMs)
        }
    }
}
