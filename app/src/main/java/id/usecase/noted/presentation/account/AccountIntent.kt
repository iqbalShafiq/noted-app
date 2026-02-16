package id.usecase.noted.presentation.account

sealed interface AccountIntent {
    data object LoadAccountInfo : AccountIntent

    data object RefreshProfile : AccountIntent

    data object LoginClicked : AccountIntent

    data object LogoutClicked : AccountIntent

    data object NavigateBackClicked : AccountIntent
}
