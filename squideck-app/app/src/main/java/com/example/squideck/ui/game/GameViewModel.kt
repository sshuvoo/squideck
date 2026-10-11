package com.example.squideck.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.squideck.data.SharedPrefsProfileStore
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.domain.economy.UsCents
import com.example.squideck.domain.economy.Wallet
import com.example.squideck.domain.profile.PlayerName
import com.example.squideck.domain.profile.ProfileStore
import com.example.squideck.domain.session.GameSession
import com.example.squideck.domain.session.LocalBotSession
import com.example.squideck.domain.session.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class Screen { Name, GameList, Lobby, Game }

/** Everything the screens render: which screen, the player, their credits and the table they sit at. */
data class GameUiState(
    val screen: Screen,
    val playerName: String = "",
    val credits: UsCents = UsCents(0),
    val economy: EconomyConfig = EconomyConfig(),
    /** Present once the player has joined a table. */
    val session: SessionState? = null,
    val selectedCardId: Int? = null,
) {
    val canAffordEntry: Boolean get() = credits >= economy.entryCost
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModel(
    private val profileStore: ProfileStore,
    private val wallet: Wallet,
    private val economy: EconomyConfig,
    private val sessionFactory: (CoroutineScope, String, Wallet) -> GameSession,
) : ViewModel() {
    private val playerName = MutableStateFlow(profileStore.loadName())
    private val editingName = MutableStateFlow(false)
    private val session = MutableStateFlow<GameSession?>(null)
    private val selectedCardId = MutableStateFlow<Int?>(null)

    private val sessionState: Flow<SessionState?> = session.flatMapLatest { it?.state ?: flowOf(null) }

    val state: StateFlow<GameUiState> = combine(
        playerName, editingName, wallet.balance, sessionState, selectedCardId,
    ) { name, editing, credits, table, selected ->
        val screen = when {
            name.isNullOrBlank() || editing -> Screen.Name
            table == null -> Screen.GameList
            table.game == null -> Screen.Lobby
            else -> Screen.Game
        }
        GameUiState(screen, name.orEmpty(), credits, economy, table, selected?.takeIf { table != null && it in table.legalCardIds })
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        GameUiState(
            screen = if (playerName.value.isNullOrBlank()) Screen.Name else Screen.GameList,
            playerName = playerName.value.orEmpty(),
            credits = wallet.balance.value,
            economy = economy,
        ),
    )

    fun submitName(raw: String) {
        if (!PlayerName.isValid(raw)) return
        val name = PlayerName.clean(raw)
        profileStore.saveName(name)
        playerName.value = name
        editingName.value = false
    }

    fun changeName() {
        editingName.value = true
    }

    /** Sits the player at the (only) 10-cent table; the entry is charged when the round starts. */
    fun joinTable() {
        val name = playerName.value ?: return
        if (session.value != null || !wallet.canAfford(economy.entryCost)) return
        session.value = sessionFactory(viewModelScope, name, wallet)
    }

    /** Back to the game list. Leaving mid-round forfeits the entry that was already paid. */
    fun leaveTable() {
        session.value?.close()
        session.value = null
        selectedCardId.value = null
    }

    fun refillWallet() {
        if (!wallet.canAfford(economy.entryCost)) wallet.reset()
    }

    fun restart() {
        selectedCardId.value = null
        session.value?.restart()
    }

    fun takeControl() {
        session.value?.takeControl()
    }

    /** Any touch on a card while auto-play is running hands control back to the player. */
    fun selectCard(cardId: Int) {
        val current = session.value ?: return
        if (current.state.value.autoPlay) current.takeControl()
        if (cardId !in current.state.value.legalCardIds) return
        selectedCardId.update { if (it == cardId) null else cardId }
    }

    fun passSelected() {
        val cardId = state.value.selectedCardId ?: return
        selectedCardId.value = null
        session.value?.pass(cardId)
    }

    override fun onCleared() {
        session.value?.close()
    }

    companion object {
        /** Composition root for now; swap [LocalBotSession] for the API-backed session later. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                GameViewModel(
                    profileStore = SharedPrefsProfileStore(app),
                    wallet = Wallet(),
                    economy = EconomyConfig(),
                    sessionFactory = { scope, name, wallet -> LocalBotSession(scope, name, wallet) },
                )
            }
        }
    }
}
