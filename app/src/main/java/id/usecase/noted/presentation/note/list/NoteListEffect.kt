package id.usecase.noted.presentation.note.list

sealed interface NoteListEffect {
    data class NavigateToEditor(val noteId: Long?) : NoteListEffect

    data class NavigateToHistoryNote(val noteId: String) : NoteListEffect

    data class ShowMessage(val message: String) : NoteListEffect

    data object NavigateToSync : NoteListEffect

    data object NavigateToAccount : NoteListEffect

    data object NavigateToExplore : NoteListEffect
}
