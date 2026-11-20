package com.izquierdojl.tolocharadio.core.ui.components

/**
 * Etiquetas estables para los tests de UI de los controles nuevos
 * (spec 0047). Mantenerlas en un único lugar evita cadenas duplicadas
 * entre pruebas instrumentadas y composables.
 */
object TolochaTestTags {
    /** Barra superior compartida. */
    const val TOP_BAR = "tolocha_top_bar"

    /** Icono de hamburguesa que abre el cajón. */
    const val MENU_BUTTON = "tolocha_menu_button"

    /** Cajón de navegación secundario. */
    const val DRAWER = "tolocha_drawer"

    /** Insignia de episodios pendientes sobre el destino Cola. */
    const val QUEUE_BADGE = "tolocha_queue_badge"

    /** Botón de descarga con progreso. */
    const val DOWNLOAD_BUTTON = "tolocha_download_button"

    /** Tarjeta de programa en cuadrícula. */
    const val PODCAST_CARD = "tolocha_podcast_card"

    /** Tarjeta de episodio en cuadrícula. */
    const val EPISODE_CARD = "tolocha_episode_card"

    /** Control del potenciador de voz. */
    const val VOICE_BOOST = "tolocha_voice_boost"

    /** Bloque superior de la cola con el episodio en curso. */
    const val QUEUE_NOW_PLAYING = "tolocha_queue_now_playing"

    /** Línea secundaria de duración/restante de una fila de cola. */
    const val QUEUE_EPISODE_META = "tolocha_queue_episode_meta"

    /** Lista desplazable de pendientes de la cola. */
    const val QUEUE_LIST = "tolocha_queue_list"

    /** Fila de tiempo transcurrido/restante de la tarjeta inferior. */
    const val EPISODE_TIME_ROW = "tolocha_episode_time_row"

    /** Selector de pantalla de arranque de podcast en Configuración. */
    const val PODCAST_START_SCREEN = "tolocha_podcast_start_screen"
}
