package com.example.squideck.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.squideck.data.SharedPrefsProfileStore
import com.example.squideck.data.SharedPrefsSettingsStore
import com.example.squideck.domain.Phase
import com.example.squideck.domain.profile.PlayerName
import com.example.squideck.domain.profile.ProfileStore
import com.example.squideck.domain.session.GameSession
import com.example.squideck.domain.session.LocalBotSession
import com.example.squideck.domain.session.SessionState
import com.example.squideck.domain.settings.SettingsStore
import com.example.squideck.net.ClientSession
import com.example.squideck.net.ClientTransport
import com.example.squideck.net.HostSession
import com.example.squideck.net.NearbyRadios
import com.example.squideck.net.RoomAd
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

/** What the Nearby tab shows while looking for rooms. */
data class NearbyBrowse(
    val searching: Boolean = false,
    val rooms: List<RoomAd> = emptyList(),
    val error: String? = null,
)

/** Everything the screens render: which screen, the player and the table they sit at. */
data class GameUiState(
    val screen: Screen,
    val playerName: String = "",
    val avatarId: Int = AvatarStyles.DEFAULT_PLAYER_ID,
    /** Present once the player has joined a table. */
    val session: SessionState? = null,
    val musicEnabled: Boolean = true,
    /** Rooms found nearby while the Nearby tab is open. */
    val nearby: NearbyBrowse = NearbyBrowse(),
) {
    /** Music plays while a round is on screen, quieter under the results card. */
    val musicPlaying: Boolean get() = musicEnabled && screen == Screen.Game
    val roundOver: Boolean get() = session?.game?.let { it.phase != Phase.Playing } ?: false
    val hasName: Boolean get() = playerName.isNotBlank()
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModel(
    private val profileStore: ProfileStore,
    private val settingsStore: SettingsStore,
    private val sessionFactory: (scope: CoroutineScope, name: String, avatarId: Int) -> GameSession,
    private val radios: NearbyRadios,
) : ViewModel() {
    private val playerName = MutableStateFlow(profileStore.loadName())
    private val avatarId = MutableStateFlow(profileStore.loadAvatarId() ?: AvatarStyles.DEFAULT_PLAYER_ID)
    private val editingName = MutableStateFlow(false)
    private val musicEnabled = MutableStateFlow(settingsStore.loadMusicEnabled())
    private val session = MutableStateFlow<GameSession?>(null)

    /** The radio used to look for rooms; handed to the session when the player joins one. */
    private var browser: ClientTransport? = null
    private val browserFlow = MutableStateFlow<ClientTransport?>(null)
    private val nearby: Flow<NearbyBrowse> = browserFlow.flatMapLatest { radio ->
        if (radio == null) flowOf(NearbyBrowse())
        else combine(radio.rooms, radio.discoveryError) { rooms, error -> NearbyBrowse(searching = error == null, rooms = rooms, error = error) }
    }

    private val sessionState: Flow<SessionState?> = session.flatMapLatest { it?.state ?: flowOf(null) }

    private val profile = combine(playerName, avatarId, musicEnabled) { name, avatar, music -> Triple(name, avatar, music) }

    val state: StateFlow<GameUiState> = combine(
        profile, editingName, sessionState, nearby,
    ) { (name, avatar, music), editing, table, rooms ->
        val screen = when {
            name.isNullOrBlank() || editing -> Screen.Name
            table == null -> Screen.GameList
            table.game == null -> Screen.Lobby
            else -> Screen.Game
        }
        GameUiState(screen, name.orEmpty(), avatar, table, music, rooms)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        GameUiState(
            screen = if (playerName.value.isNullOrBlank()) Screen.Name else Screen.GameList,
            playerName = playerName.value.orEmpty(),
            avatarId = avatarId.value,
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

    /** Sits the player at the bot table (free play). */
    fun joinTable() {
        val name = playerName.value ?: return
        if (session.value != null) return
        session.value = sessionFactory(viewModelScope, name, avatarId.value)
    }

    /** Nearby tab opened: look for rooms (the caller has made sure permissions are granted). */
    fun startNearbySearch() {
        if (session.value != null) return
        val radio = browser ?: radios.client().also {
            browser = it
            browserFlow.value = it
        }
        radio.startDiscovery()
    }

    /** Nearby tab closed: stop looking, which saves battery. */
    fun stopNearbySearch() {
        browser?.close()
        browser = null
        browserFlow.value = null
    }

    /** Opens a nearby room on this phone; friends see it and can join. */
    fun createRoom() {
        val name = playerName.value ?: return
        if (session.value != null) return
        stopNearbySearch()
        session.value = HostSession(viewModelScope, name, avatarId.value, radios.host(), botAvatarIds = AvatarStyles.idsExcept(avatarId.value) + avatarId.value)
    }

    /** Joins a room found nearby, reusing the radio that found it. */
    fun joinRoom(room: RoomAd) {
        val name = playerName.value ?: return
        val radio = browser ?: return
        if (session.value != null) return
        browser = null
        browserFlow.value = null
        session.value = ClientSession(viewModelScope, name, avatarId.value, radio, room.id, room.creatorName)
    }

    /** Room creator: start now, bots fill the empty seats. */
    fun startRoomNow() {
        session.value?.startNow()
    }

    /** Back to the game list. */
    fun leaveTable() {
        session.value?.close()
        session.value = null
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
        browser?.close()
    }

    companion object {
        /** Composition root for now; swap [LocalBotSession] for the API-backed session later. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                GameViewModel(
                    profileStore = SharedPrefsProfileStore(app),
                    settingsStore = SharedPrefsSettingsStore(app),
                    sessionFactory = { scope, name, avatar ->
                        LocalBotSession(scope, name, playerAvatarId = avatar, botAvatarIds = AvatarStyles.idsExcept(avatar))
                    },
                    radios = com.example.squideck.data.nearby.NearbyRadios(app),
                )
            }
        }
    }
}
