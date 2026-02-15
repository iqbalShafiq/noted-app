package id.usecase.noted.presentation.account

sealed interface AccountEffect {
    data object NavigateBack : AccountEffect

    data object NavigateToLogin : AccountEffect

    data class ShowMessage(val message: String) : AccountEffect
}
