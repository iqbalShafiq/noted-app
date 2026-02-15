package id.usecase.noted.feature.note.presentation.list

sealed interface NoteListIntent {
    data object AddNoteClicked : NoteListIntent

    data class NoteClicked(val noteId: Long) : NoteListIntent

    data object RetryObserve : NoteListIntent
}
