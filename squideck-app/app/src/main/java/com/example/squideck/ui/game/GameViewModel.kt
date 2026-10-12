package com.example.squideck.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.squideck.data.SharedPrefsProfileStore
import com.example.squideck.data.SharedPrefsSettingsStore
import com.example.squideck.domain.Phase
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.domain.economy.UsCents
import com.example.squideck.domain.economy.Wallet
import com.example.squideck.domain.profile.PlayerName
import com.example.squideck.domain.profile.ProfileStore
import com.example.squideck.domain.session.GameSession
import com.example.squideck.domain.session.LocalBotSession
import com.example.squideck.domain.session.SessionState
import com.example.squideck.domain.settings.SettingsStore
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

enum class Screen { Name, GameList, Lobby, Game }

/** Everything the screens render: which screen, the player, their credits and the table they sit at. */
data class GameUiState(
    val screen: Screen,
    val playerName: String = "",
    val avatarId: Int = AvatarStyles.DEFAULT_PLAYER_ID,
    val credits: UsCents = UsCents(0),
    val economy: EconomyConfig = EconomyConfig(),
    /** Present once the player has joined a table. */
    val session: SessionState? = null,
    val musicEnabled: Boolean = true,
) {
    /** Music plays while a round is on screen, quieter under the results card. */
    val musicPlaying: Boolean get() = musicEnabled && screen == Screen.Game
    val roundOver: Boolean get() = session?.game?.let { it.phase != Phase.Playing } ?: false
    val canAffordEntry: Boolean get() = credits >= economy.entryCost
    val hasName: Boolean get() = playerName.isNotBlank()
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModel(
    private val profileStore: ProfileStore,
    private val settingsStore: SettingsStore,
    private val wallet: Wallet,
    private val economy: EconomyConfig,
    private val sessionFactory: (scope: CoroutineScope, name: String, avatarId: Int, wallet: Wallet) -> GameSession,
) : ViewModel() {
    private val playerName = MutableStateFlow(profileStore.loadName())
    private val avatarId = MutableStateFlow(profileStore.loadAvatarId() ?: AvatarStyles.DEFAULT_PLAYER_ID)
    private val editingName = MutableStateFlow(false)
    private val musicEnabled = MutableStateFlow(settingsStore.loadMusicEnabled())
    private val session = MutableStateFlow<GameSession?>(null)

    private val sessionState: Flow<SessionState?> = session.flatMapLatest { it?.state ?: flowOf(null) }

    private val profile = combine(playerName, avatarId, musicEnabled) { name, avatar, music -> Triple(name, avatar, music) }

    val state: StateFlow<GameUiState> = combine(
        profile, editingName, wallet.balance, sessionState,
    ) { (name, avatar, music), editing, credits, table ->
        val screen = when {
            name.isNullOrBlank() || editing -> Screen.Name
            table == null -> Screen.GameList
            table.game == null -> Screen.Lobby
            else -> Screen.Game
        }
        GameUiState(screen, name.orEmpty(), avatar, credits, economy, table, music)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        GameUiState(
            screen = if (playerName.value.isNullOrBlank()) Screen.Name else Screen.GameList,
            playerName = playerName.value.orEmpty(),
            avatarId = avatarId.value,
            credits = wallet.balance.value,
            economy = economy,
            musicEnabled = musicEnabled.value,
        ),
    )

    fun toggleMusic() {
        val enabled = !musicEnabled.value
        settingsStore.saveMusicEnabled(enabled)
        musicEnabled.value = enabled
    }

    /** Saves the profile chosen on the first screen: display name and avatar. */
    fun submitProfile(raw: String, avatar: Int) {
        if (!PlayerName.isValid(raw)) return
        val name = PlayerName.clean(raw)
        profileStore.saveName(name)
        profileStore.saveAvatarId(avatar)
        playerName.value = name
        avatarId.value = avatar
        editingName.value = false
    }

    fun changeName() {
        editingName.value = true
    }

    /** Back out of editing; only possible once a name exists. */
    fun cancelNameChange() {
        if (!playerName.value.isNullOrBlank()) editingName.value = false
    }

    /** Sits the player at the (only) 10-cent table; the entry is charged when the round starts. */
    fun joinTable() {
        val name = playerName.value ?: return
        if (session.value != null || !wallet.canAfford(economy.entryCost)) return
        session.value = sessionFactory(viewModelScope, name, avatarId.value, wallet)
    }

    /** Back to the game list. Leaving mid-round forfeits the entry that was already paid. */
    fun leaveTable() {
        session.value?.close()
        session.value = null
    }

    fun refillWallet() {
        if (!wallet.canAfford(economy.entryCost)) wallet.reset()
    }

    fun restart() {
        session.value?.restart()
    }

    fun takeControl() {
        session.value?.takeControl()
    }

    /** Any touch on a card while auto-play is running hands control back to the player. */
    fun tapCard() {
        val current = session.value ?: return
        if (current.state.value.autoPlay) current.takeControl()
    }

    /** The player threw [cardId] up out of their hand. */
    fun passCard(cardId: Int) {
        val current = session.value ?: return
        if (current.state.value.autoPlay) current.takeControl()
        current.pass(cardId)
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
                    settingsStore = SharedPrefsSettingsStore(app),
                    wallet = Wallet(),
                    economy = EconomyConfig(),
                    sessionFactory = { scope, name, avatar, wallet ->
                        LocalBotSession(scope, name, wallet, playerAvatarId = avatar, botAvatarIds = AvatarStyles.idsExcept(avatar))
                    },
                )
            }
        }
    }
}
