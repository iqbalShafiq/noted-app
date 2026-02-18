package id.usecase.noted.presentation.note.detail

sealed interface NoteDetailEffect {
    data class NavigateToEditor(val localNoteId: Long) : NoteDetailEffect
    data class ShowMessage(val message: String) : NoteDetailEffect
    data object NavigateBack : NoteDetailEffect
    data object ScrollToComments : NoteDetailEffect
}
