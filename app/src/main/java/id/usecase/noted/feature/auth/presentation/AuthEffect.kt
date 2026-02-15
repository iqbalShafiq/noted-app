package id.usecase.noted.feature.auth.presentation

sealed interface AuthEffect {
    data class ShowMessage(val message: String) : AuthEffect

    data object NavigateToLogin : AuthEffect

    data object NavigateToRegister : AuthEffect

    data object NavigateToForgotPassword : AuthEffect

    data object NavigateBack : AuthEffect
}
