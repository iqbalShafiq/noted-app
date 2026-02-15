package id.usecase.noted.presentation.note.list

sealed interface NoteListEffect {
    data class NavigateToEditor(val noteId: Long?) : NoteListEffect

    data object NavigateToAuth : NoteListEffect

    data class ShowMessage(val message: String) : NoteListEffect
}
