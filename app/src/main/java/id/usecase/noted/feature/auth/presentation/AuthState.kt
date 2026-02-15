package id.usecase.noted.feature.auth.presentation

import id.usecase.noted.feature.note.data.sync.NoteSyncStatus

data class AuthState(
    val usernameInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val isSubmitting: Boolean = false,
    val syncStatus: NoteSyncStatus = NoteSyncStatus(),
)
