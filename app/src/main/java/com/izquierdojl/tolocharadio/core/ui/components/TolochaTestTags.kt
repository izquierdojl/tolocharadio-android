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
}
