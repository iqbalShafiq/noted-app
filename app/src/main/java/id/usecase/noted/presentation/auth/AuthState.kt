package id.usecase.noted.presentation.auth

import id.usecase.noted.data.sync.NoteSyncStatus

data class AuthState(
    val usernameInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val isSubmitting: Boolean = false,
    val syncStatus: NoteSyncStatus = NoteSyncStatus(),
)
