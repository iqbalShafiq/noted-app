package id.usecase.noted.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.usecase.noted.data.sync.NoteSyncCoordinator
import id.usecase.noted.data.sync.UserSession
import id.usecase.noted.domain.user.UserRepository
import id.usecase.noted.shared.user.UserProfileDto
import id.usecase.noted.shared.user.UserStatisticsDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val noteSyncCoordinator: NoteSyncCoordinator,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state.asStateFlow()

    private val _effect = Channel<AccountEffect>(Channel.BUFFERED)
    val effect: Flow<AccountEffect> = _effect.receiveAsFlow()

    private var sessionObserveJob: Job? = null

    init {
        startObserveSession()
        if (noteSyncCoordinator.session.value.isLoggedIn) {
            fetchProfile()
        }
    }

    fun onIntent(intent: AccountIntent) {
        when (intent) {
            AccountIntent.LoadAccountInfo -> loadAccountInfo()
            AccountIntent.RefreshProfile -> fetchProfile()
            AccountIntent.LoginClicked -> _effect.trySend(AccountEffect.NavigateToLogin)
            AccountIntent.LogoutClicked -> logout()
            AccountIntent.NavigateBackClicked -> _effect.trySend(AccountEffect.NavigateBack)
        }
    }

    private fun startObserveSession() {
        sessionObserveJob?.cancel()
        sessionObserveJob = viewModelScope.launch {
            noteSyncCoordinator.session.collect { session ->
                updateStateFromSession(session)
            }
        }
    }

    private fun updateStateFromSession(session: UserSession) {
        _state.update { currentState ->
            currentState.copy(
                isLoggedIn = session.isLoggedIn,
                username = session.username,
                userId = session.userId,
            )
        }
    }

    private fun fetchProfile() {
        if (_state.value.isLoading) {
            return
        }

        viewModelScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            userRepository.getProfile()
                .onSuccess { (profile, statistics) ->
                    updateStateFromProfileAndStatistics(profile, statistics)
                }
                .onFailure { error ->
                    _state.update { currentState ->
                        currentState.copy(
                            errorMessage = error.message ?: "Gagal memuat profil",
                        )
                    }
                    _effect.trySend(
                        AccountEffect.ShowMessage(
                            error.message ?: "Gagal memuat profil",
                        ),
                    )
                }

            _state.update { currentState ->
                currentState.copy(isLoading = false)
            }
        }
    }

    private fun updateStateFromProfileAndStatistics(
        profile: UserProfileDto,
        statistics: UserStatisticsDto,
    ) {
        _state.update { currentState ->
            currentState.copy(
                username = profile.username,
                userId = profile.userId,
                displayName = profile.displayName,
                bio = profile.bio,
                profilePictureUrl = profile.profilePictureUrl,
                email = profile.email,
                createdAtEpochMillis = profile.createdAtEpochMillis,
                lastLoginAtEpochMillis = profile.lastLoginAtEpochMillis,
                updatedAtEpochMillis = profile.updatedAtEpochMillis,
                totalNotes = statistics.totalNotes,
                notesShared = statistics.notesShared,
                notesReceived = statistics.notesReceived,
                lastSyncAtEpochMillis = statistics.lastSyncAtEpochMillis,
            )
        }
    }

    private fun loadAccountInfo() {
        _state.update { currentState ->
            currentState.copy(
                isLoading = true,
                errorMessage = null,
            )
        }

        val session = noteSyncCoordinator.session.value
        updateStateFromSession(session)

        _state.update { currentState ->
            currentState.copy(isLoading = false)
        }
    }

    private fun logout() {
        if (_state.value.isLoading) {
            return
        }

        viewModelScope.launch {
            _state.update { currentState ->
                currentState.copy(isLoading = true)
            }

            runCatching {
                noteSyncCoordinator.signOut()
            }.onSuccess {
                _effect.trySend(AccountEffect.ShowMessage("Logout berhasil"))
            }.onFailure { error ->
                _effect.trySend(AccountEffect.ShowMessage(error.message ?: "Logout gagal"))
            }

            _state.update { currentState ->
                currentState.copy(isLoading = false)
            }
        }
    }
}
