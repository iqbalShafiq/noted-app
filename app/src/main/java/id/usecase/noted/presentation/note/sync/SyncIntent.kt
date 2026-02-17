package id.usecase.noted.presentation.note.sync

sealed interface SyncIntent {
    data object LoadSyncStatus : SyncIntent

    data object UploadClicked : SyncIntent

    data object AccountClicked : SyncIntent

    data object LoginClicked : SyncIntent

    data class RetryNoteClicked(val noteId: Long) : SyncIntent

    data object NavigateBackClicked : SyncIntent
}
