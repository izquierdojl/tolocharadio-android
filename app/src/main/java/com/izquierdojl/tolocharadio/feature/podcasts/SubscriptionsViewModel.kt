package com.izquierdojl.tolocharadio.feature.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.PodcastSubscriptionsRepo
import com.izquierdojl.tolocharadio.domain.podcasts.UnsubscribePodcastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de Suscripciones (US2, FR-009). */
sealed interface SubscriptionsUiState {
    data object Loading : SubscriptionsUiState

    data object Empty : SubscriptionsUiState

    data class Content(val items: List<PodcastSummary>) : SubscriptionsUiState

    data class Error(val message: String) : SubscriptionsUiState
}

/** Suscripciones propias con baja (US2, FR-008/FR-009). */
@HiltViewModel
class SubscriptionsViewModel
    @Inject
    constructor(
        private val repo: PodcastSubscriptionsRepo,
        private val unsubscribeUseCase: UnsubscribePodcastUseCase,
        private val prefs: InstancePrefs,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<SubscriptionsUiState>(SubscriptionsUiState.Loading)
        val ui: StateFlow<SubscriptionsUiState> = _ui.asStateFlow()

        private val _message = MutableStateFlow<String?>(null)
        val message: StateFlow<String?> = _message.asStateFlow()

        /** Orden manual persistido en este dispositivo. */
        val manualOrder: StateFlow<List<Long>> =
            prefs.podcastSubscriptionOrder.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

        init {
            refresh()
            viewModelScope.launch {
                repo.subscriptions.collect { items ->
                    if (_ui.value !is SubscriptionsUiState.Loading) {
                        _ui.value =
                            if (items.isEmpty()) {
                                SubscriptionsUiState.Empty
                            } else {
                                SubscriptionsUiState.Content(items)
                            }
                    }
                }
            }
        }

        fun refresh() {
            viewModelScope.launch {
                _ui.value = SubscriptionsUiState.Loading
                when (val r = repo.list()) {
                    is ApiResult.Ok ->
                        _ui.value =
                            if (r.value.isEmpty()) {
                                SubscriptionsUiState.Empty
                            } else {
                                SubscriptionsUiState.Content(r.value)
                            }
                    is ApiResult.Err -> _ui.value = SubscriptionsUiState.Error(r.error.userMessage())
                }
            }
        }

        fun unsubscribe(podcastId: Long) {
            viewModelScope.launch {
                when (val r = unsubscribeUseCase(podcastId)) {
                    is ApiResult.Ok -> _message.value = "Baja de la suscripción."
                    is ApiResult.Err -> _message.value = r.error.userMessage()
                }
            }
        }

        fun clearMessage() {
            _message.value = null
        }

        /** Guarda el orden manual arrastrado (solo local, como en la web no hay servidor). */
        fun saveManualOrder(ids: List<Long>) {
            viewModelScope.launch { prefs.setPodcastSubscriptionOrder(ids) }
        }
    }
