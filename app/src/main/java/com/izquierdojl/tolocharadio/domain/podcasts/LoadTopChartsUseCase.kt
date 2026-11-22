package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.repo.PodcastSubscriptionsRepo
import javax.inject.Inject

/**
 * Obtiene el ranking de podcasts y aplica la degradación por categoría.
 *
 * Reglas:
 * - `TopGenre.ALL` pide el ranking general.
 * - Si la categoría falla con un error recuperable (`Unavailable`/`Unknown`),
 *   pide el general y marca `genreUnavailable`.
 * - No hay fallback para `Unauthorized` (401/403) ni `NotFound` (404): se
 *   propaga el error para que la UI muestre «Editar servidor» u oculte.
 *
 * @return ranking ordenado o el error tipado de la última petición.
 */
class LoadTopChartsUseCase
    @Inject
    constructor(
        private val repo: PodcastSubscriptionsRepo,
    ) {
        suspend operator fun invoke(
            country: TopCountry,
            genre: TopGenre,
        ): ApiResult<TopChartsLoad> {
            if (genre.id == null) {
                return when (val general = repo.top(country.id)) {
                    is ApiResult.Ok -> ApiResult.Ok(TopChartsLoad(general.value, genreUnavailable = false))
                    is ApiResult.Err -> general
                }
            }

            return when (val filtered = repo.top(country.id, genre.id)) {
                is ApiResult.Ok ->
                    ApiResult.Ok(TopChartsLoad(filtered.value, genreUnavailable = false))
                is ApiResult.Err -> {
                    if (!filtered.error.isRecoverable()) return filtered
                    when (val general = repo.top(country.id)) {
                        is ApiResult.Ok -> ApiResult.Ok(TopChartsLoad(general.value, genreUnavailable = true))
                        is ApiResult.Err -> general
                    }
                }
            }
        }

        private fun DomainError.isRecoverable(): Boolean {
            return this is DomainError.Unavailable || this is DomainError.Unknown
        }
    }
