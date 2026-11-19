package com.izquierdojl.tolocharadio.feature.podcasts.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.data.repo.QueueRepo
import com.izquierdojl.tolocharadio.domain.podcasts.QueueBadge
import com.izquierdojl.tolocharadio.domain.podcasts.queueBadgeFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Insignia de pendientes para la barra inferior (spec 0047, US5).
 * Se refresca al entrar en modo Podcasts y al volver a primer plano;
 * las mutaciones locales ya publican un `QueueState` nuevo.
 */
@HiltViewModel
class QueueBadgeViewModel
    @Inject
    constructor(
        private val queue: QueueRepo,
    ) : ViewModel() {
        private val playingId = MutableStateFlow<Long?>(null)

        val badge: StateFlow<QueueBadge> =
            combine(queue.queue, playingId) { state, id ->
                queueBadgeFor(state.items, id)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = QueueBadge(0),
            )

        /** Actualiza el episodio en curso para excluirlo del conteo. */
        fun setPlayingId(id: Long?) {
            playingId.value = id
        }

        /** Recarga la cola; un fallo conserva el último valor conocido. */
        fun refresh() {
            viewModelScope.launch { queue.load() }
        }
    }
