package id.usecase.noted.presentation.auth

sealed interface AuthIntent {
    data class UsernameChanged(val value: String) : AuthIntent

    data class PasswordChanged(val value: String) : AuthIntent

    data class ConfirmPasswordChanged(val value: String) : AuthIntent

    data object LoginSubmitClicked : AuthIntent

    data object RegisterSubmitClicked : AuthIntent

    data object ForgotPasswordSubmitClicked : AuthIntent

    data object OpenRegisterClicked : AuthIntent

    data object OpenForgotPasswordClicked : AuthIntent

    data object OpenLoginClicked : AuthIntent

    data object LogoutClicked : AuthIntent

    data object BackClicked : AuthIntent
}
