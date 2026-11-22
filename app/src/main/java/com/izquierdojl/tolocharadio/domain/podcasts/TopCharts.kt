package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult

/** Número máximo de elementos que pide la sección «Populares». */
const val TOP_CHARTS_LIMIT = 50

/**
 * Países admitidos por el ranking de Apple. La lista es fija y el orden
 * coincide con la web; España es el valor por defecto.
 */
enum class TopCountry(
    val id: String,
    val label: String,
) {
    SPAIN("es", "España"),
    MEXICO("mx", "México"),
    ARGENTINA("ar", "Argentina"),
    COLOMBIA("co", "Colombia"),
    CHILE("cl", "Chile"),
    PERU("pe", "Perú"),
    UNITED_STATES("us", "Estados Unidos"),
    UNITED_KINGDOM("gb", "Reino Unido"),
    FRANCE("fr", "Francia"),
    GERMANY("de", "Alemania"),
    ITALY("it", "Italia"),
    PORTUGAL("pt", "Portugal"),
    BRAZIL("br", "Brasil"),
    ;

    companion object {
        /** Devuelve el país con [id] o España si no existe o es inválido. */
        fun fromId(id: String?): TopCountry = entries.firstOrNull { it.id == id } ?: SPAIN
    }
}

/**
 * Categorías de Apple Podcasts. [ALL] representa «Todas las categorías» y
 * no envía el parámetro `genre`. Los IDs son los de Apple y no se traducen.
 */
enum class TopGenre(
    val id: String?,
    val label: String,
) {
    ALL(null, "Todas las categorías"),
    ARTS("1301", "Artes"),
    BUSINESS("1321", "Negocios"),
    COMEDY("1303", "Comedia"),
    EDUCATION("1304", "Educación"),
    FICTION("1483", "Ficción"),
    HISTORY("1487", "Historia"),
    HEALTH("1512", "Salud y bienestar"),
    KIDS("1305", "Niños y familia"),
    LEISURE("1502", "Ocio"),
    MUSIC("1310", "Música"),
    NEWS("1489", "Noticias"),
    RELIGION("1314", "Religión y espiritualidad"),
    SCIENCE("1533", "Ciencia"),
    SOCIETY("1324", "Sociedad y cultura"),
    SPORTS("1545", "Deportes"),
    TECHNOLOGY("1318", "Tecnología"),
    TRUE_CRIME("1488", "Crímenes reales"),
    TV_FILM("1309", "TV y cine"),
    ;

    companion object {
        /** Devuelve la categoría con [id] o [ALL] si no existe o es inválida. */
        fun fromId(id: String?): TopGenre = entries.firstOrNull { it.id == id } ?: ALL
    }
}

/** Ranking resuelto por [LoadTopChartsUseCase]. */
data class TopChartsLoad(
    val items: List<PodcastSearchResult> = emptyList(),
    val genreUnavailable: Boolean = false,
)
