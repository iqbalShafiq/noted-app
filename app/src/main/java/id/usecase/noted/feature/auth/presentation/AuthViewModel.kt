package id.usecase.noted.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.usecase.noted.feature.note.data.sync.NoteSyncCoordinator
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val noteSyncCoordinator: NoteSyncCoordinator,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _effect = Channel<AuthEffect>(Channel.BUFFERED)
    val effect: Flow<AuthEffect> = _effect.receiveAsFlow()

    private var syncStatusObserveJob: Job? = null

    init {
        startObserveSyncStatus()
    }

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.UsernameChanged -> {
                _state.update { currentState ->
                    currentState.copy(usernameInput = intent.value)
                }
            }

            is AuthIntent.PasswordChanged -> {
                _state.update { currentState ->
                    currentState.copy(passwordInput = intent.value)
                }
            }

            is AuthIntent.ConfirmPasswordChanged -> {
                _state.update { currentState ->
                    currentState.copy(confirmPasswordInput = intent.value)
                }
            }

            AuthIntent.LoginSubmitClicked -> submitLogin()
            AuthIntent.RegisterSubmitClicked -> submitRegister()
            AuthIntent.ForgotPasswordSubmitClicked -> submitForgotPassword()
            AuthIntent.OpenRegisterClicked -> {
                clearSensitiveInputs()
                _effect.trySend(AuthEffect.NavigateToRegister)
            }

            AuthIntent.OpenForgotPasswordClicked -> {
                clearSensitiveInputs()
                _effect.trySend(AuthEffect.NavigateToForgotPassword)
            }

            AuthIntent.OpenLoginClicked -> {
                clearSensitiveInputs()
                _effect.trySend(AuthEffect.NavigateToLogin)
            }

            AuthIntent.LogoutClicked -> logout()
            AuthIntent.BackClicked -> {
                clearSensitiveInputs()
                _effect.trySend(AuthEffect.NavigateBack)
            }
        }
    }

    private fun startObserveSyncStatus() {
        syncStatusObserveJob?.cancel()
        syncStatusObserveJob = viewModelScope.launch {
            noteSyncCoordinator.syncStatus.collect { syncStatus ->
                _state.update { currentState ->
                    val normalizedUsername = if (currentState.usernameInput.isBlank()) {
                        syncStatus.username.orEmpty()
                    } else {
                        currentState.usernameInput
                    }

                    currentState.copy(
                        syncStatus = syncStatus,
                        usernameInput = normalizedUsername,
                    )
                }
            }
        }
    }

    private fun submitLogin() {
        val currentState = state.value
        if (currentState.isSubmitting) {
            return
        }

        val username = currentState.usernameInput.trim()
        val password = currentState.passwordInput

        if (!validateUsernameAndPassword(username = username, password = password)) {
            return
        }

        viewModelScope.launch {
            _state.update { state -> state.copy(isSubmitting = true) }

            runCatching {
                noteSyncCoordinator.login(username = username, password = password)
            }.onSuccess {
                clearSensitiveInputs()
                _effect.trySend(AuthEffect.ShowMessage("Login sebagai $username berhasil"))
                _effect.trySend(AuthEffect.NavigateBack)
            }.onFailure { error ->
                _effect.trySend(AuthEffect.ShowMessage(error.message ?: "Login gagal"))
            }

            _state.update { state -> state.copy(isSubmitting = false) }
        }
    }

    private fun submitRegister() {
        val currentState = state.value
        if (currentState.isSubmitting) {
            return
        }

        val username = currentState.usernameInput.trim()
        val password = currentState.passwordInput
        val confirmPassword = currentState.confirmPasswordInput

        if (!validateUsernameAndPassword(username = username, password = password)) {
            return
        }
        if (password != confirmPassword) {
            _effect.trySend(AuthEffect.ShowMessage("Konfirmasi password tidak sama"))
            return
        }

        viewModelScope.launch {
            _state.update { state -> state.copy(isSubmitting = true) }

            runCatching {
                noteSyncCoordinator.register(username = username, password = password)
            }.onSuccess {
                clearSensitiveInputs()
                _effect.trySend(AuthEffect.ShowMessage("Registrasi berhasil sebagai $username"))
                _effect.trySend(AuthEffect.NavigateBack)
            }.onFailure { error ->
                _effect.trySend(AuthEffect.ShowMessage(error.message ?: "Registrasi gagal"))
            }

            _state.update { state -> state.copy(isSubmitting = false) }
        }
    }

    private fun submitForgotPassword() {
        val currentState = state.value
        if (currentState.isSubmitting) {
            return
        }

        val username = currentState.usernameInput.trim()
        val password = currentState.passwordInput
        val confirmPassword = currentState.confirmPasswordInput

        if (!validateUsernameAndPassword(username = username, password = password)) {
            return
        }
        if (password != confirmPassword) {
            _effect.trySend(AuthEffect.ShowMessage("Konfirmasi password tidak sama"))
            return
        }

        viewModelScope.launch {
            _state.update { state -> state.copy(isSubmitting = true) }

            runCatching {
                noteSyncCoordinator.forgotPassword(
                    username = username,
                    newPassword = password,
                )
            }.onSuccess {
                clearSensitiveInputs()
                _effect.trySend(
                    AuthEffect.ShowMessage(
                        "Password berhasil diperbarui. Silakan login dengan password baru.",
                    ),
                )
                _effect.trySend(AuthEffect.NavigateToLogin)
            }.onFailure { error ->
                _effect.trySend(AuthEffect.ShowMessage(error.message ?: "Reset password gagal"))
            }

            _state.update { state -> state.copy(isSubmitting = false) }
        }
    }

    private fun logout() {
        if (state.value.isSubmitting) {
            return
        }

        viewModelScope.launch {
            _state.update { currentState ->
                currentState.copy(isSubmitting = true)
            }

            runCatching {
                noteSyncCoordinator.signOut()
            }.onSuccess {
                clearSensitiveInputs()
                _effect.trySend(AuthEffect.ShowMessage("Logout berhasil"))
            }.onFailure { error ->
                _effect.trySend(AuthEffect.ShowMessage(error.message ?: "Logout gagal"))
            }

            _state.update { currentState ->
                currentState.copy(isSubmitting = false)
            }
        }
    }

    private fun validateUsernameAndPassword(username: String, password: String): Boolean {
        if (username.isBlank()) {
            _effect.trySend(AuthEffect.ShowMessage("Username wajib diisi"))
            return false
        }
        if (password.length < 8) {
            _effect.trySend(AuthEffect.ShowMessage("Password minimal 8 karakter"))
            return false
        }
        return true
    }

    private fun clearSensitiveInputs() {
        _state.update { currentState ->
            currentState.copy(
                passwordInput = "",
                confirmPasswordInput = "",
            )
        }
    }
}
