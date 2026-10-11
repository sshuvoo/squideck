package com.example.squideck.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.squideck.domain.session.GameSession
import com.example.squideck.domain.session.LocalBotSession
import com.example.squideck.domain.session.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Everything the screens render: session state plus UI-only selection. */
data class GameUiState(
    val session: SessionState,
    val selectedCardId: Int? = null,
)

class GameViewModel(sessionFactory: (CoroutineScope) -> GameSession) : ViewModel() {
    private val session = sessionFactory(viewModelScope)
    private val selectedCardId = MutableStateFlow<Int?>(null)

    val state: StateFlow<GameUiState> = combine(session.state, selectedCardId) { sessionState, selected ->
        GameUiState(sessionState, selected?.takeIf { it in sessionState.legalCardIds })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GameUiState(session.state.value))

    fun startGame() = session.start()

    fun restart() {
        selectedCardId.value = null
        session.restart()
    }

    fun selectCard(cardId: Int) {
        if (cardId !in session.state.value.legalCardIds) return
        selectedCardId.update { if (it == cardId) null else cardId }
    }

    fun passSelected() {
        val cardId = state.value.selectedCardId ?: return
        selectedCardId.value = null
        session.pass(cardId)
    }

    override fun onCleared() = session.close()

    companion object {
        /** Composition root for now; swap [LocalBotSession] for the API-backed session later. */
        val Factory = viewModelFactory {
            initializer { GameViewModel(::LocalBotSession) }
        }
    }
}
