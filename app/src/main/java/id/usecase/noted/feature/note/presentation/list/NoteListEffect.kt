package id.usecase.noted.feature.note.presentation.list

sealed interface NoteListEffect {
    data class NavigateToEditor(val noteId: Long?) : NoteListEffect

    data class ShowMessage(val message: String) : NoteListEffect
}
