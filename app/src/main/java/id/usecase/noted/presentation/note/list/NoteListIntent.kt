package id.usecase.noted.presentation.note.list

sealed interface NoteListIntent {
    data object AddNoteClicked : NoteListIntent

    data class NoteClicked(val noteId: Long) : NoteListIntent

    data class NoteDeleteClicked(val noteId: Long) : NoteListIntent

    data object RetryObserve : NoteListIntent

    data class SearchQueryChanged(val query: String) : NoteListIntent

    data object SearchClicked : NoteListIntent

    data object SyncClicked : NoteListIntent

    data object AccountClicked : NoteListIntent

    data object ExploreClicked : NoteListIntent
}
