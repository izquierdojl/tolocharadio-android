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

    /** Sección de controles de auriculares. */
    const val HEADSET_CONTROLS = "tolocha_headset_controls"

    /** Selector de segundos de retroceso. */
    const val HEADSET_SKIP_BACK = "tolocha_headset_skip_back"

    /** Selector de segundos de avance. */
    const val HEADSET_SKIP_FORWARD = "tolocha_headset_skip_forward"

    /** Interruptor de gestos de salto del auricular. */
    const val HEADSET_GESTURES_SWITCH = "tolocha_headset_gestures_switch"

    /** Campo de búsqueda de Suscripciones. */
    const val SUBSCRIPTIONS_SEARCH_FIELD = "tolocha_subscriptions_search_field"

    /** Botón que despliega u oculta los controles de búsqueda. */
    const val SUBSCRIPTIONS_SEARCH_TOGGLE = "tolocha_subscriptions_search_toggle"

    /** Alternancia de ámbito Podcasts | Episodios. */
    const val SUBSCRIPTIONS_SEARCH_SCOPE = "tolocha_subscriptions_search_scope"

    /** Acción de limpiar el término de búsqueda. */
    const val SUBSCRIPTIONS_SEARCH_CLEAR = "tolocha_subscriptions_search_clear"

    /** Acción de confirmar la búsqueda de episodios. */
    const val SUBSCRIPTIONS_SEARCH_SUBMIT = "tolocha_subscriptions_search_submit"

    /** Contenedor de resultados de episodios. */
    const val SUBSCRIPTIONS_SEARCH_RESULTS = "tolocha_subscriptions_search_results"

    /** Estado vacío de la búsqueda de episodios. */
    const val SUBSCRIPTIONS_SEARCH_EMPTY = "tolocha_subscriptions_search_empty"

    /** Estado de error de la búsqueda de episodios. */
    const val SUBSCRIPTIONS_SEARCH_ERROR = "tolocha_subscriptions_search_error"

    /** Acción de cargar la página siguiente. */
    const val SUBSCRIPTIONS_SEARCH_LOAD_MORE = "tolocha_subscriptions_search_load_more"

    /** Estado anunciado de la búsqueda (live region). */
    const val SUBSCRIPTIONS_SEARCH_STATUS = "tolocha_subscriptions_search_status"

    /** Contenedor de recarga por arrastre de Inicio. */
    const val HOME_PULL_REFRESH = "tolocha_home_pull_refresh"

    /** Contenedor de recarga por arrastre de Suscripciones. */
    const val SUBSCRIPTIONS_PULL_REFRESH = "tolocha_subscriptions_pull_refresh"

    /** Indicador de progreso de la recarga por arrastre. */
    const val PULL_REFRESH_INDICATOR = "tolocha_pull_refresh_indicator"

    /** Contenedor de la sección «Populares». */
    const val TOP_CHARTS_SECTION = "tolocha_top_charts_section"

    /** Selector de país del ranking. */
    const val TOP_CHARTS_COUNTRY = "tolocha_top_charts_country"

    /** Selector de género del ranking. */
    const val TOP_CHARTS_GENRE = "tolocha_top_charts_genre"

    /** Lista de resultados del ranking. */
    const val TOP_CHARTS_LIST = "tolocha_top_charts_list"

    /** Estado anunciado de la sección (carga/error/vacío/aviso). */
    const val TOP_CHARTS_STATUS = "tolocha_top_charts_status"

    /** Acción de reintento del ranking. */
    const val TOP_CHARTS_RETRY = "tolocha_top_charts_retry"

    /** Acción «Editar servidor» ante error de credenciales. */
    const val TOP_CHARTS_AUTH_ACTION = "tolocha_top_charts_auth_action"

    /** Aviso de fallback de categoría con su reintento. */
    const val TOP_CHARTS_NOTICE = "tolocha_top_charts_notice"

    /** Estado vacío del ranking. */
    const val TOP_CHARTS_EMPTY = "tolocha_top_charts_empty"
}
