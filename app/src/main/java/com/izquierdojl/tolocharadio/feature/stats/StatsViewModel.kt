package com.izquierdojl.tolocharadio.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.StatsRecentListDto
import com.izquierdojl.tolocharadio.domain.stats.ComputeStatsSummaryUseCase
import com.izquierdojl.tolocharadio.domain.stats.LoadStatsUseCase
import com.izquierdojl.tolocharadio.domain.stats.StatsBundle
import com.izquierdojl.tolocharadio.domain.stats.StatsPeriod
import com.izquierdojl.tolocharadio.domain.stats.StatsRange
import com.izquierdojl.tolocharadio.domain.stats.StatsSummary
import com.izquierdojl.tolocharadio.domain.stats.TimelineGranularity
import com.izquierdojl.tolocharadio.domain.stats.autoGranularity
import com.izquierdojl.tolocharadio.domain.stats.buildHabitsMatrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de la vista de gráficas: carga, contenido, vacío o error con reintento. */
sealed interface StatsUiState {
    data object Loading : StatsUiState

    data class Content(
        val bundle: StatsBundle,
        val summary: StatsSummary,
        val granularity: TimelineGranularity,
        val habitsMatrix: List<List<Long>> = emptyList(),
    ) : StatsUiState

    data object Empty : StatsUiState

    /** Error con marca de credenciales: en ese caso se ofrece editar el servidor (principio IV). */
    data class Error(
        val message: String,
        val isAuthError: Boolean = false,
    ) : StatsUiState
}

/**
 * Gráficas de escucha del servidor activo. El filtro de periodo vive
 * fuera de [StatsUiState] para permanecer visible también en
 * `Empty`/`Error` (data-model.md); cambiarlo recarga todo el conjunto
 * (SC-004) y `onForeground` repite la carga al volver de segundo plano
 * (principio IV).
 */
@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        private val load: LoadStatsUseCase,
        private val compute: ComputeStatsSummaryUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
        val ui: StateFlow<StatsUiState> = _ui.asStateFlow()

        private val _period = MutableStateFlow(StatsPeriod.THIRTY)
        val period: StateFlow<StatsPeriod> = _period.asStateFlow()

        /** Carga en curso: evita duplicar peticiones al reanudar. */
        private var loading = false

        init {
            refresh()
        }

        /** Cambia el periodo (7/30/90/todo) y recarga todos los bloques. */
        fun onPeriodChange(period: StatsPeriod) {
            if (_period.value == period) return
            _period.value = period
            refresh()
        }

        /** Recarga al volver a primer plano (la sesión o la red pueden haberse perdido). */
        fun onForeground() {
            if (loading) return
            refresh()
        }

        /** Recarga el conjunto del periodo vigente. */
        fun refresh() {
            viewModelScope.launch {
                loading = true
                try {
                    if (_ui.value !is StatsUiState.Content) _ui.value = StatsUiState.Loading
                    val range = _period.value.range()
                    when (val r = load(range)) {
                        is ApiResult.Ok -> _ui.value = stateFor(range, r.value)
                        is ApiResult.Err ->
                            _ui.value =
                                StatsUiState.Error(
                                    r.error.userMessage(),
                                    isAuthError = r.error is DomainError.Unauthorized,
                                )
                    }
                } finally {
                    loading = false
                }
            }
        }

        /** Alias de [refresh] para el botón de reintento. */
        fun retry() = refresh()

        private fun stateFor(
            range: StatsRange,
            bundle: StatsBundle,
        ): StatsUiState {
            val summary = compute(bundle.timeline, bundle.top)
            if (summary.totalMs <= 0L || summary.topStation == null) return StatsUiState.Empty
            return StatsUiState.Content(
                bundle =
                    bundle.copy(
                        recent =
                            StatsRecentListDto(
                                bundle.recent.items.sortedByDescending { it.startedAt },
                            ),
                    ),
                summary = summary,
                granularity = range.autoGranularity(),
                habitsMatrix = buildHabitsMatrix(bundle.habits.items),
            )
        }
    }
