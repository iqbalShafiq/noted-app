package id.usecase.noted.feature.note.presentation.list

sealed interface NoteListIntent {
    data object AddNoteClicked : NoteListIntent

    data class NoteClicked(val noteId: Long) : NoteListIntent

    data class NoteDeleteClicked(val noteId: Long) : NoteListIntent

    data object RetryObserve : NoteListIntent

    data class LoginInputChanged(val value: String) : NoteListIntent

    data class PasswordInputChanged(val value: String) : NoteListIntent

    data object LoginSubmitClicked : NoteListIntent

    data object RegisterSubmitClicked : NoteListIntent

    data object LogoutClicked : NoteListIntent

    data object SyncNowClicked : NoteListIntent

    data object UploadNowClicked : NoteListIntent

    data object ImportNowClicked : NoteListIntent
}
