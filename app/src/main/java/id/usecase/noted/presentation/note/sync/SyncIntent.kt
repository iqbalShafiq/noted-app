package id.usecase.noted.presentation.note.sync

sealed interface SyncIntent {
    data object LoadSyncStatus : SyncIntent

    data object SyncNowClicked : SyncIntent

    data object UploadNowClicked : SyncIntent

    data object ImportNowClicked : SyncIntent

    data class RetryNoteClicked(val noteId: Long) : SyncIntent

    data object NavigateBackClicked : SyncIntent
}
