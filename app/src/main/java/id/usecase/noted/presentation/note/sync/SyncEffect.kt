package id.usecase.noted.presentation.note.sync

sealed interface SyncEffect {
    data object NavigateBack : SyncEffect

    data object NavigateToAccount : SyncEffect

    data object NavigateToLogin : SyncEffect

    data class ShowMessage(val message: String) : SyncEffect
}
