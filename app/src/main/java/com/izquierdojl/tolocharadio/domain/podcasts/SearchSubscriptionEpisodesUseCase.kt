package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeSearchPage
import com.izquierdojl.tolocharadio.data.repo.EpisodesRepo
import javax.inject.Inject

/**
 * Búsqueda de episodios por título dentro de las suscripciones (US8).
 * El servidor ya limita el resultado a los podcasts suscritos; el caso de
 * uso solo valida el término y delega la paginación.
 */
class SearchSubscriptionEpisodesUseCase
    @Inject
    constructor(private val repo: EpisodesRepo) {
        suspend operator fun invoke(
            term: String,
            offset: Int = 0,
            limit: Int = SEARCH_PAGE_SIZE,
        ): ApiResult<PodcastEpisodeSearchPage> {
            val query = term.trim()
            if (query.isEmpty()) return ApiResult.Err(DomainError.Validation(emptyList()))
            return repo.searchEpisodes(query, offset, limit)
        }
    }

/** Resultados por página de la búsqueda de episodios suscritos. */
const val SEARCH_PAGE_SIZE = 20
