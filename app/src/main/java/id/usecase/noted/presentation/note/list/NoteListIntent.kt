package id.usecase.noted.presentation.note.list

sealed interface NoteListIntent {
    data object AddNoteClicked : NoteListIntent

    data class NoteClicked(val noteId: Long) : NoteListIntent

    data class NoteDeleteClicked(val noteId: Long) : NoteListIntent

    data object RetryObserve : NoteListIntent

    data object AuthClicked : NoteListIntent

    data object SyncNowClicked : NoteListIntent

    data object UploadNowClicked : NoteListIntent

    data object ImportNowClicked : NoteListIntent
}
